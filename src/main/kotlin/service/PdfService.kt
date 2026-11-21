package com.notely.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.nio.file.Path
import java.util.concurrent.Semaphore
import kotlin.math.sqrt

data class RenderedPages(val images: List<ImageBitmap>, val pageCount: Int)

class PdfService {
    private val renderSlots = Semaphore(1)

    fun renderPages(filePath: Path, startIndex: Int, count: Int): RenderedPages {
        require(startIndex >= 0 && count in 1..3)
        renderSlots.acquire()
        try {
            return when (filePath.fileName.toString().substringAfterLast('.').lowercase()) {
                "pdf" -> renderPdf(filePath, startIndex, count)
                "jpg", "jpeg", "png" -> {
                    require(startIndex == 0) { "Страница не найдена" }
                    RenderedPages(listOf(SafeImageReader.read(filePath).toComposeImageBitmap()), 1)
                }
                else -> error("Неподдерживаемый формат файла")
            }
        } finally {
            renderSlots.release()
        }
    }

    private fun renderPdf(file: Path, startIndex: Int, count: Int): RenderedPages {
        Loader.loadPDF(file.toFile()).use { document ->
            require(startIndex < document.numberOfPages) { "Страница не найдена" }
            val renderer = PDFRenderer(document)
            val images = (startIndex until minOf(startIndex + count, document.numberOfPages)).map { index ->
                val dpi = safeDpi(
                    document.getPage(index).cropBox.width.toDouble(),
                    document.getPage(index).cropBox.height.toDouble(),
                    150.0
                )
                renderer.renderImageWithDPI(index, dpi).toComposeImageBitmap()
            }
            return RenderedPages(images, document.numberOfPages)
        }
    }

    companion object {
        private const val MAX_RENDER_PIXELS = 7_000_000.0

        fun safeDpi(widthPoints: Double, heightPoints: Double, preferredDpi: Double): Float {
            require(widthPoints.isFinite() && heightPoints.isFinite() &&
                    widthPoints > 0.0 && heightPoints > 0.0 &&
                    widthPoints <= 20_000.0 && heightPoints <= 20_000.0) {
                "Недопустимый размер страницы PDF"
            }
            val area = widthPoints * heightPoints
            val limit = 72.0 * sqrt(MAX_RENDER_PIXELS / area)
            val dpi = minOf(preferredDpi, limit)
            require(dpi >= 18.0) { "Страница PDF слишком большая" }
            return dpi.toFloat()
        }
    }
}
