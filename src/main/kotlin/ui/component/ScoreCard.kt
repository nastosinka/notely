package com.notely.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notely.domain.Score
import com.notely.service.ThumbnailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

@Composable
fun ScoreCard(
    score: Score,
    filePath: Path,
    thumbnailService: ThumbnailService
) {
    val thumbnail by produceState<Result<ImageBitmap>?>(
        initialValue = null,
        filePath
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                thumbnailService.getThumbnail(filePath)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFC5DBF5)),
            contentAlignment = Alignment.Center
        ) {
            when {
                thumbnail == null -> {
                    CircularProgressIndicator(
                        color = Color(0xFF719FCF),
                        modifier = Modifier.size(28.dp)
                    )
                }

                thumbnail?.isSuccess == true -> {
                    thumbnail?.getOrNull()?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = score.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        )
                    }
                }

                else -> {
                    Text(
                        text = "📄",
                        fontSize = 48.sp
                    )
                }
            }
        }

        Text(
            text = score.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF304C70),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
