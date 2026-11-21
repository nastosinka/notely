package com.notely.repository.filesystem

import com.notely.domain.Folder
import com.notely.repository.FolderRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class FileFolderRepository(
    private val libraryPath: Path
) : FolderRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        Files.createDirectories(libraryPath)
    }

    override fun findAll(): List<Folder> {
        return Files.list(libraryPath).use { paths ->
            paths
                .filter { Files.isDirectory(it) }
                .map { it.resolve(METADATA_FILE_NAME) }
                .filter { Files.isRegularFile(it) }
                .map { path ->
                    json.decodeFromString<Folder>(
                        Files.readString(path)
                    )
                }
                .toList()
                .sortedBy { it.name.lowercase() }
        }
    }


    override fun save(folder: Folder) {
        val folderPath = libraryPath.resolve(folder.name)

        require(Files.notExists(folderPath)) {
            "Папка с названием «${folder.name}» уже существует"
        }

        Files.createDirectory(folderPath)

        try {
            Files.writeString(
                folderPath.resolve(METADATA_FILE_NAME),
                json.encodeToString(folder)
            )
        } catch (exception: Exception) {
            Files.deleteIfExists(
                folderPath.resolve(METADATA_FILE_NAME)
            )
            Files.deleteIfExists(folderPath)
            throw exception
        }
    }

    companion object {
        private const val METADATA_FILE_NAME = ".notely-folder.json"
    }
}
