package com.notely.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.nio.file.Path
import javax.imageio.ImageIO

data class RenderedPages(
    val images: List<ImageBitmap>,
    val pageCount: Int
)

class PdfService {

    fun renderPages(
        filePath: Path,
        startIndex: Int,
        count: Int
    ): RenderedPages {
        require(startIndex >= 0)
        require(count in 1..3)

        return when (
            filePath.fileName.toString()
                .substringAfterLast('.')
                .lowercase()
        ) {
            "pdf" -> renderPdfPages(
                filePath,
                startIndex,
                count
            )

            "jpg", "jpeg", "png" -> renderImage(filePath, startIndex)

            else -> error("Неподдерживаемый формат файла")
        }
    }

    private fun renderPdfPages(
        filePath: Path,
        startIndex: Int,
        count: Int
    ): RenderedPages {
        Loader.loadPDF(filePath.toFile()).use { document ->
            val totalPages = document.numberOfPages

            require(startIndex < totalPages) {
                "Страница не найдена"
            }

            val renderer = PDFRenderer(document)
            val endIndex = minOf(startIndex + count, totalPages)

            val images = (startIndex until endIndex).map { index ->
                renderer.renderImageWithDPI(
                    index,
                    150f
                ).toComposeImageBitmap()
            }

            return RenderedPages(
                images = images,
                pageCount = totalPages
            )
        }
    }

    private fun renderImage(
        filePath: Path,
        startIndex: Int
    ): RenderedPages {
        require(startIndex == 0) {
            "Страница не найдена"
        }

        val image = ImageIO.read(filePath.toFile())
            ?: error("Не удалось прочитать изображение")

        return RenderedPages(
            images = listOf(image.toComposeImageBitmap()),
            pageCount = 1
        )
    }
}
