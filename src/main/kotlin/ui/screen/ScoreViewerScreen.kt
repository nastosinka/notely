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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notely.domain.Score
import com.notely.service.PdfService
import com.notely.service.RenderedPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

private val ViewerBackground = Color(0xFFEDF4FC)
private val ToolbarBackground = Color(0xFFDCEAF8)
private val ViewerText = Color(0xFF304C70)
private val AccentColor = Color(0xFFBDD7F3)

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

    var pagesPerRow by remember(filePath) {
        mutableIntStateOf(1)
    }

    var zoom by remember(filePath) {
        mutableFloatStateOf(1f)
    }

    var pageAlignment by remember(filePath) {
        mutableStateOf(Alignment.CenterHorizontally)
    }

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    val rendered by produceState<Result<RenderedPages>?>(
        initialValue = null,
        filePath,
        pageIndex,
        pagesPerRow
    ) {
        value = null

        value = withContext(Dispatchers.IO) {
            runCatching {
                pdfService.renderPages(
                    filePath = filePath,
                    startIndex = pageIndex,
                    count = pagesPerRow
                )
            }
        }
    }

    val pages = rendered?.getOrNull()
    val pageCount = pages?.pageCount ?: 0
    val lastPage = minOf(pageIndex + pagesPerRow, pageCount)

    LaunchedEffect(pageIndex, pagesPerRow) {
        horizontalScroll.scrollTo(0)
        verticalScroll.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(ToolbarBackground)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

            (1..3).forEach { count ->
                Button(
                    onClick = {
                        pageIndex = (pageIndex / count) * count
                        pagesPerRow = count
                    },
                    modifier = Modifier.size(38.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (pagesPerRow == count) {
                            AccentColor
                        } else {
                            ViewerBackground
                        },
                        contentColor = ViewerText
                    ),
                    elevation = null
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    pageIndex = (pageIndex - pagesPerRow)
                        .coerceAtLeast(0)
                },
                enabled = pageIndex > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Предыдущие страницы"
                )
            }

            Text(
                text = if (pageCount > 0) {
                    "${pageIndex + 1}–$lastPage / $pageCount"
                } else {
                    "..."
                },
                color = ViewerText,
                fontSize = 13.sp
            )

            IconButton(
                onClick = {
                    pageIndex += pagesPerRow
                },
                enabled = pageCount > 0 &&
                        pageIndex + pagesPerRow < pageCount
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Следующие страницы"
                )
            }

            Spacer(Modifier.width(8.dp))



            IconButton(
                onClick = {
                    pageAlignment = when (pageAlignment) {
                        Alignment.Start -> Alignment.CenterHorizontally
                        Alignment.CenterHorizontally -> Alignment.End
                        else -> Alignment.Start
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                val alignmentIcon = when (pageAlignment) {
                    Alignment.Start -> Icons.Default.FormatAlignLeft
                    Alignment.CenterHorizontally -> Icons.Default.FormatAlignCenter
                    else -> Icons.Default.FormatAlignRight
                }

                val alignmentDescription = when (pageAlignment) {
                    Alignment.Start -> "Слева"
                    Alignment.CenterHorizontally -> "По центру"
                    else -> "Справа"
                }

                Icon(
                    imageVector = alignmentIcon,
                    contentDescription = "Выравнивание: $alignmentDescription",
                    tint = ViewerText,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = {
                    zoom = (zoom - 0.25f).coerceAtLeast(0.5f)
                },
                enabled = zoom > 0.5f
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Уменьшить"
                )
            }

            Text(
                text = "${(zoom * 100).toInt()}%",
                color = ViewerText,
                fontSize = 13.sp
            )

            IconButton(
                onClick = {
                    zoom = (zoom + 0.25f).coerceAtMost(2.5f)
                },
                enabled = zoom < 2.5f
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
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
                        text = rendered?.exceptionOrNull()?.message
                            ?: "Не удалось открыть ноты",
                        color = Color(0xFFB65B66)
                    )
                }
            }

            pages != null -> {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val pageSpacing = 16.dp
                    val outerPadding = 24.dp
                    val containerWidth = maxWidth

                    val availableWidth = containerWidth -
                            outerPadding * 2 -
                            pageSpacing * (pagesPerRow - 1)

                    val fittedPageWidth = (
                            availableWidth / pagesPerRow
                            ).coerceAtLeast(80.dp)

                    val pageWidth = fittedPageWidth * zoom

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScroll)
                            .verticalScroll(verticalScroll)
                    ) {

                        Row(
                            modifier = Modifier
                                .padding(outerPadding)
                                .widthIn(
                                    min = containerWidth - outerPadding * 2
                                ),
                            horizontalArrangement = Arrangement.spacedBy(
                                space = pageSpacing,
                                alignment = when (pageAlignment) {
                                    Alignment.Start -> Alignment.Start
                                    Alignment.CenterHorizontally -> Alignment.CenterHorizontally
                                    else -> Alignment.End
                                }
                            ),
                            verticalAlignment = Alignment.Top
                        ) {
                            pages.images.forEachIndexed { index, image ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color.White,
                                        elevation = 4.dp
                                    ) {
                                        Image(
                                            bitmap = image,
                                            contentDescription = "Страница ${pageIndex + index + 1}",
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier
                                                .width(pageWidth)
                                                .aspectRatio(
                                                    image.width.toFloat() /
                                                            image.height.toFloat()
                                                )
                                        )
                                    }

                                    Text(
                                        text = "${pageIndex + index + 1}",
                                        color = ViewerText,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
