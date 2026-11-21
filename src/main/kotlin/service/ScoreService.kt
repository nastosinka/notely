package com.notely.service

import com.notely.domain.Folder
import com.notely.domain.Score
import com.notely.repository.ScoreRepository
import java.nio.file.Path

class ScoreService(
    private val repository: ScoreRepository
) {
    fun getScores(folder: Folder): List<Score> {
        return repository.findAll(folder)
    }

    fun importScore(
        folder: Folder,
        file: Path
    ): Score {
        return repository.importFile(folder, file)
    }

    fun getFilePath(
        folder: Folder,
        score: Score
    ): Path {
        return repository.getFilePath(folder, score)
    }
}
