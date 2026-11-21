package com.notely.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notely.domain.Folder
import com.notely.domain.Score
import com.notely.service.ScoreService
import com.notely.ui.component.ScoreCard
import com.notely.ui.dialog.ScoreFilePicker
import com.notely.service.ThumbnailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

@Composable
fun FolderScreen(
    folder: Folder,
    scoreService: ScoreService,
    filePicker: ScoreFilePicker,
    onBack: () -> Unit,
    onOpenScore: (Score) -> Unit,
    thumbnailService: ThumbnailService,
) {
    val scores = remember(folder.id) { mutableStateListOf<Score>() }
    val scope = rememberCoroutineScope()
    var isImporting by remember(folder.id) { mutableStateOf(false) }
    var isLoading by remember(folder.id) { mutableStateOf(true) }
    var errorMessage by remember(folder.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(folder.id) {
        isLoading = true
        try {
            val loaded = withContext(Dispatchers.IO) { scoreService.getScores(folder) }
            scores.clear()
            scores.addAll(loaded)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            errorMessage = "Не удалось загрузить ноты"
        } finally {
            isLoading = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDF4FC))
    ) {
        Column(
            modifier = Modifier
                .width(84.dp)
                .fillMaxHeight()
                .background(Color(0xFFDCEAF8))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color(0xFF304C70)
                )
            }

            Button(
                onClick = {
                    if (!isImporting) {
                        scope.launch {
                            isImporting = true
                            errorMessage = null
                            try {
                                val selected = withContext(Dispatchers.IO) { filePicker.chooseFile() }
                                if (selected != null) {
                                    val imported = withContext(Dispatchers.IO) {
                                        scoreService.importScore(folder, selected)
                                    }
                                    scores.add(imported)
                                }
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                errorMessage = "Не удалось импортировать ноты"
                            } finally {
                                isImporting = false
                            }
                        }
                    }
                },
                enabled = !isImporting,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFBDD7F3),
                    contentColor = Color(0xFF304C70)
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 1.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить ноты",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Text(
                text = "${folder.emoji} ${folder.name}",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF304C70)
            )

            Spacer(Modifier.height(28.dp))

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFB65B66)
                )

                Spacer(Modifier.height(16.dp))
            }

            if (isImporting) {
                Text("Импортируем ноты...", color = Color(0xFF6583A4))
                Spacer(Modifier.height(12.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF719FCF))
            } else if (scores.isEmpty()) {
                Text(
                    text = "Здесь пока нет нот. Нажмите +, чтобы импортировать файл.",
                    color = Color(0xFF6583A4),
                    fontSize = 16.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(24.dp)
                ) {

                    items(
                        items = scores,
                        key = { it.id }
                    ) { score ->
                        val filePath = remember(folder.id, score.id) {
                            runCatching {
                                scoreService.getFilePath(folder, score)
                            }
                        }

                        Box(
                            modifier = Modifier.clickable {
                                onOpenScore(score)
                            }
                        ) {
                            filePath.onSuccess { path ->
                                ScoreCard(
                                    score = score,
                                    filePath = path,
                                    thumbnailService = thumbnailService
                                )
                            }.onFailure {
                                Text(
                                    text = score.title,
                                    color = Color(0xFFB65B66)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
