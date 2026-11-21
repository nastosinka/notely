package com.notely.repository.filesystem

import com.notely.domain.Folder
import com.notely.domain.Score
import com.notely.repository.ScoreRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class FileScoreRepository(
    private val libraryPath: Path
) : ScoreRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun findAll(folder: Folder): List<Score> {
        val folderPath = libraryPath.resolve(folder.name)

        if (!Files.isDirectory(folderPath)) {
            return emptyList()
        }

        return Files.list(folderPath).use { paths ->
            paths
                .filter { Files.isDirectory(it) }
                .map { it.resolve(METADATA_FILE_NAME) }
                .filter { Files.isRegularFile(it) }
                .map { path ->
                    json.decodeFromString<Score>(
                        Files.readString(path)
                    )
                }
                .filter { it.folderId == folder.id }
                .toList()
                .sortedBy { it.title.lowercase() }
        }
    }

    override fun importFile(
        folder: Folder,
        source: Path
    ): Score {
        require(Files.isRegularFile(source)) {
            "Выбранный файл не найден"
        }

        val originalFileName = source.fileName.toString()
        val extension = originalFileName
            .substringAfterLast('.', "")
            .lowercase()

        require(extension in SUPPORTED_EXTENSIONS) {
            "Поддерживаются только PDF, JPG и PNG"
        }

        val title = originalFileName.substringBeforeLast('.')
        val scoreId = UUID.randomUUID().toString()
        val storedFileName = "original.$extension"

        val score = Score(
            id = scoreId,
            folderId = folder.id,
            title = title,
            originalFileName = originalFileName,
            storedFileName = storedFileName
        )

        val folderPath = libraryPath.resolve(folder.name)

        require(Files.isDirectory(folderPath)) {
            "Папка библиотеки не найдена"
        }

        val scorePath = folderPath.resolve(scoreId)

        Files.createDirectory(scorePath)

        try {
            Files.copy(
                source,
                scorePath.resolve(storedFileName)
            )

            Files.writeString(
                scorePath.resolve(METADATA_FILE_NAME),
                json.encodeToString(score)
            )
        } catch (exception: Exception) {
            Files.deleteIfExists(
                scorePath.resolve(METADATA_FILE_NAME)
            )
            Files.deleteIfExists(
                scorePath.resolve(storedFileName)
            )
            Files.deleteIfExists(scorePath)
            throw exception
        }

        return score
    }

    override fun getFilePath(
        folder: Folder,
        score: Score
    ): Path {
        require(score.folderId == folder.id) {
            "Ноты не принадлежат выбранной папке"
        }

        val folderPath = libraryPath.resolve(folder.name)
        val scorePath = folderPath.resolve(score.id).normalize()
        val filePath = scorePath.resolve(score.storedFileName).normalize()

        require(filePath.parent == scorePath) {
            "Некорректный путь к нотам"
        }

        require(Files.isRegularFile(filePath)) {
            "Файл нот не найден"
        }

        return filePath
    }


    companion object {
        private const val METADATA_FILE_NAME =
            ".notely-score.json"

        private val SUPPORTED_EXTENSIONS =
            setOf("pdf", "jpg", "jpeg", "png")
    }
}
