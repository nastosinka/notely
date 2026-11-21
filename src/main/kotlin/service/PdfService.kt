package com.notely.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.nio.file.Path
import javax.imageio.ImageIO

data class RenderedScore(
    val image: ImageBitmap,
    val pageCount: Int
)

class PdfService {

    fun renderPage(
        filePath: Path,
        pageIndex: Int
    ): RenderedScore {
        return when (
            filePath.fileName.toString()
                .substringAfterLast('.')
                .lowercase()
        ) {
            "pdf" -> renderPdf(filePath, pageIndex)
            "jpg", "jpeg", "png" -> renderImage(filePath, pageIndex)
            else -> error("Неподдерживаемый формат файла")
        }
    }

    private fun renderPdf(
        filePath: Path,
        pageIndex: Int
    ): RenderedScore {
        Loader.loadPDF(filePath.toFile()).use { document ->
            val pageCount = document.numberOfPages

            require(pageIndex in 0 until pageCount) {
                "Страница не найдена"
            }

            val renderer = PDFRenderer(document)
            val image = renderer.renderImageWithDPI(
                pageIndex,
                150f
            )

            return RenderedScore(
                image = image.toComposeImageBitmap(),
                pageCount = pageCount
            )
        }
    }

    private fun renderImage(
        filePath: Path,
        pageIndex: Int
    ): RenderedScore {
        require(pageIndex == 0) {
            "Страница не найдена"
        }

        val image = ImageIO.read(filePath.toFile())
            ?: error("Не удалось прочитать изображение")

        return RenderedScore(
            image = image.toComposeImageBitmap(),
            pageCount = 1
        )
    }
}
