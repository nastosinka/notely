package com.notely.repository

import com.notely.domain.Folder
import com.notely.domain.Score
import java.nio.file.Path

interface ScoreRepository {
    fun findAll(folder: Folder): List<Score>

    fun importFile(
        folder: Folder,
        source: Path
    ): Score

    fun getFilePath(
        folder: Folder,
        score: Score
    ): Path
}
