package com.notely

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.notely.domain.Folder
import com.notely.domain.Score
import com.notely.repository.filesystem.FileFolderRepository
import com.notely.repository.filesystem.FileScoreRepository
import com.notely.service.FolderService
import com.notely.service.PdfService
import com.notely.service.ScoreService
import com.notely.ui.dialog.ScoreFilePicker
import com.notely.ui.screen.FolderScreen
import com.notely.ui.screen.LibraryScreen
import com.notely.ui.screen.ScoreViewerScreen
import com.notely.service.ThumbnailService
import java.nio.file.Paths

fun main() = application {
    val libraryPath = remember {
        Paths.get(
            System.getProperty("user.home"),
            "Documents",
            "Notely Library"
        )
    }

    val folderService = remember {
        FolderService(
            FileFolderRepository(libraryPath)
        )
    }

    val scoreService = remember {
        ScoreService(
            FileScoreRepository(libraryPath)
        )
    }

    val pdfService = remember {
        PdfService()
    }

    val thumbnailService = remember {
        ThumbnailService()
    }

    val filePicker = remember {
        ScoreFilePicker()
    }

    var selectedFolder by remember {
        mutableStateOf<Folder?>(null)
    }

    var selectedScore by remember {
        mutableStateOf<Score?>(null)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Notely",
        state = rememberWindowState(
            width = 1100.dp,
            height = 750.dp
        )
    ) {
        MaterialTheme {
            val folder = selectedFolder
            val score = selectedScore

            when {
                folder == null -> {
                    LibraryScreen(
                        folderService = folderService,
                        onOpenFolder = {
                            selectedFolder = it
                        }
                    )
                }

                score == null -> {
                    FolderScreen(
                        folder = folder,
                        scoreService = scoreService,
                        filePicker = filePicker,
                        onBack = {
                            selectedFolder = null
                        },
                        onOpenScore = {
                            selectedScore = it
                        },
                        thumbnailService = thumbnailService,
                    )
                }

                else -> {
                    val filePath = remember(folder, score) {
                        runCatching {
                            scoreService.getFilePath(
                                folder,
                                score
                            )
                        }
                    }

                    filePath.onSuccess { path ->
                        ScoreViewerScreen(
                            score = score,
                            filePath = path,
                            pdfService = pdfService,
                            onBack = {
                                selectedScore = null
                            }
                        )
                    }.onFailure { exception ->
                        Text(
                            text = exception.message
                                ?: "Не удалось открыть произведение"
                        )
                    }
                }
            }
        }
    }
}
