package com.notely.repository.filesystem

import com.notely.domain.Folder
import com.notely.repository.FolderRepository
import security.SafeLibraryPaths
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE

class FileFolderRepository(libraryPath: Path) : FolderRepository {
    private val paths = SafeLibraryPaths(libraryPath)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override fun findAll(): List<Folder> = Files.list(paths.root).use { entries ->
        entries.filter { Files.isDirectory(it, NOFOLLOW_LINKS) && !Files.isSymbolicLink(it) }
            .map { directory ->
                runCatching {
                    paths.validateFolderName(directory.fileName.toString())
                    val metadata = paths.regularFile(paths.file(directory, METADATA_FILE_NAME))
                    val folder = json.decodeFromString<Folder>(Files.readString(metadata))
                    paths.validateId(folder.id)
                    require(folder.name == directory.fileName.toString()) { "Несоответствие имени папки" }
                    folder
                }.getOrNull()
            }
            .filter { it != null }
            .map { it!! }
            .toList()
            .sortedBy { it.name.lowercase() }
    }

    override fun save(folder: Folder) {
        paths.validateId(folder.id)
        val destination = paths.folder(folder.name)
        require(Files.notExists(destination, NOFOLLOW_LINKS)) { "Папка с таким названием уже существует" }
        val staging = Files.createTempDirectory(paths.root, ".notely-pending-")
        try {
            Files.writeString(paths.file(staging, METADATA_FILE_NAME), json.encodeToString(folder))
            require(Files.notExists(destination, NOFOLLOW_LINKS)) { "Папка уже существует" }
            Files.move(staging, destination, ATOMIC_MOVE)
        } catch (exception: Exception) {
            try {
                Files.deleteIfExists(staging.resolve(METADATA_FILE_NAME))
                Files.deleteIfExists(staging)
            } catch (cleanup: Exception) {
                exception.addSuppressed(cleanup)
            }
            throw exception
        }
    }

    companion object {
        private const val METADATA_FILE_NAME = ".notely-folder.json"
    }
}
