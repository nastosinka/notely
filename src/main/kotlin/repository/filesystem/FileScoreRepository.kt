package com.notely.repository.filesystem

import com.notely.domain.Folder
import com.notely.domain.Score
import com.notely.repository.ScoreRepository
import security.SafeLibraryPaths
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.util.UUID

class FileScoreRepository(libraryPath: Path) : ScoreRepository {
    private val paths = SafeLibraryPaths(libraryPath)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override fun findAll(folder: Folder): List<Score> {
        val directory = verifiedFolder(folder)
        return Files.list(directory).use { entries ->
            entries.filter { Files.isDirectory(it, NOFOLLOW_LINKS) && !Files.isSymbolicLink(it) }
                .map { scoreDirectory ->
                    runCatching {
                        paths.validateId(scoreDirectory.fileName.toString())
                        val metadata = paths.regularFile(paths.file(scoreDirectory, METADATA_FILE_NAME))
                        val score = json.decodeFromString<Score>(Files.readString(metadata))
                        validateScore(score, folder, scoreDirectory)
                        score
                    }.getOrNull()
                }
                .filter { it != null }
                .map { it!! }
                .toList()
                .sortedBy { it.title.lowercase() }
        }
    }

    override fun importFile(folder: Folder, source: Path): Score {
        val directory = verifiedFolder(folder)
        require(Files.isRegularFile(source, NOFOLLOW_LINKS)) { "Исходный файл не найден" }
        val originalName = source.fileName.toString()
        val extension = originalName.substringAfterLast('.', "").lowercase()
        require(extension in SUPPORTED_EXTENSIONS) { "Поддерживаются PDF, JPG и PNG" }
        require(Files.size(source) in 1..MAX_FILE_BYTES) { "Файл слишком большой или пустой" }
        val id = UUID.randomUUID().toString()
        val name = "original.$extension"
        val score = Score(id, folder.id, originalName.substringBeforeLast('.'), originalName, name)
        val destination = paths.score(directory, id)
        val staging = Files.createTempDirectory(directory, ".notely-pending-")
        try {
            val copied = paths.file(staging, name)
            Files.copy(source, copied)
            require(Files.size(copied) in 1..MAX_FILE_BYTES) { "Некорректный размер файла" }
            Files.writeString(paths.file(staging, METADATA_FILE_NAME), json.encodeToString(score))
            require(Files.notExists(destination, NOFOLLOW_LINKS)) { "Произведение уже существует" }
            Files.move(staging, destination, ATOMIC_MOVE)
            return score
        } catch (exception: Exception) {
            try {
                Files.deleteIfExists(staging.resolve(METADATA_FILE_NAME))
                Files.deleteIfExists(staging.resolve(name))
                Files.deleteIfExists(staging)
            } catch (cleanup: Exception) {
                exception.addSuppressed(cleanup)
            }
            throw exception
        }
    }

    override fun getFilePath(folder: Folder, score: Score): Path {
        val directory = verifiedFolder(folder)
        val scoreDirectory = paths.directory(paths.score(directory, score.id))
        validateScore(score, folder, scoreDirectory)
        return paths.regularFile(paths.file(scoreDirectory, score.storedFileName))
    }

    private fun verifiedFolder(folder: Folder): Path {
        paths.validateId(folder.id)
        val directory = paths.directory(paths.folder(folder.name))
        val metadata = paths.regularFile(paths.file(directory, ".notely-folder.json"))
        val stored = json.decodeFromString<Folder>(Files.readString(metadata))
        require(stored.id == folder.id && stored.name == folder.name) { "Папка не соответствует метаданным" }
        return directory
    }

    private fun validateScore(score: Score, folder: Folder, directory: Path) {
        paths.validateId(score.id)
        require(score.id == directory.fileName.toString() && score.folderId == folder.id) {
            "Произведение не соответствует папке"
        }
        require(score.storedFileName in SUPPORTED_EXTENSIONS.map { "original.$it" }) {
            "Недопустимое имя файла нот"
        }
        paths.regularFile(paths.file(directory, score.storedFileName))
    }

    companion object {
        private const val METADATA_FILE_NAME = ".notely-score.json"
        private const val MAX_FILE_BYTES = 250L * 1024 * 1024
        private val SUPPORTED_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png")
    }
}
