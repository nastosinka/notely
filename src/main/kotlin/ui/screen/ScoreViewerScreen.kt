package com.notely.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notely.domain.Score
import com.notely.service.PdfService
import com.notely.service.RenderedScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

private val ViewerBackground = Color(0xFFEDF4FC)
private val ToolbarBackground = Color(0xFFDCEAF8)
private val ViewerText = Color(0xFF304C70)

@Composable
fun ScoreViewerScreen(
    score: Score,
    filePath: Path,
    pdfService: PdfService,
    onBack: () -> Unit
) {
    var pageIndex by remember(filePath) {
        mutableIntStateOf(0)
    }

    var zoom by remember(filePath) {
        mutableFloatStateOf(1f)
    }

    val rendered by produceState<Result<RenderedScore>?>(
        initialValue = null,
        filePath,
        pageIndex
    ) {
        value = null

        value = withContext(Dispatchers.IO) {
            runCatching {
                pdfService.renderPage(filePath, pageIndex)
            }
        }
    }

    val page = rendered?.getOrNull()
    val pageCount = page?.pageCount ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(ToolbarBackground)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = ViewerText
                )
            }

            Text(
                text = score.title,
                color = ViewerText,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { pageIndex-- },
                enabled = pageIndex > 0
            ) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Предыдущая страница"
                )
            }

            Text(
                text = "${pageIndex + 1} / $pageCount",
                color = ViewerText
            )

            IconButton(
                onClick = { pageIndex++ },
                enabled = page != null && pageIndex < pageCount - 1
            ) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = "Следующая страница"
                )
            }

            Spacer(Modifier.width(12.dp))

            IconButton(
                onClick = {
                    zoom = (zoom - 0.25f).coerceAtLeast(0.5f)
                },
                enabled = zoom > 0.5f
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Уменьшить"
                )
            }

            Text(
                text = "${(zoom * 100).toInt()}%",
                color = ViewerText
            )

            IconButton(
                onClick = {
                    zoom = (zoom + 0.25f).coerceAtMost(2.5f)
                },
                enabled = zoom < 2.5f
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Увеличить"
                )
            }
        }

        when {
            rendered == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF719FCF)
                    )
                }
            }

            rendered?.isFailure == true -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rendered?.exceptionOrNull()
                            ?.message
                            ?: "Не удалось открыть ноты",
                        color = Color(0xFFB65B66)
                    )
                }
            }

            page != null -> {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val pageWidth = (maxWidth - 48.dp) * zoom
                    val aspectRatio =
                        page.image.width.toFloat() /
                                page.image.height.toFloat()

                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScroll)
                            .verticalScroll(verticalScroll)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            elevation = 5.dp,
                            color = Color.White
                        ) {
                            Image(
                                bitmap = page.image,
                                contentDescription = score.title,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .width(pageWidth)
                                    .aspectRatio(aspectRatio)
                            )
                        }
                    }
                }
            }
        }
    }
}
