package com.notely.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import java.nio.file.LinkOption.NOFOLLOW_LINKS

class ThumbnailService {

    fun getThumbnail(filePath: Path): ImageBitmap {
        require(Files.isRegularFile(filePath, NOFOLLOW_LINKS)) { "Файл нот недоступен" }
        val thumbnailPath = filePath.parent.resolve(
            ".notely-preview.png"
        )

        val originalModified = Files.getLastModifiedTime(filePath)

        require(!Files.isSymbolicLink(thumbnailPath)) { "Ссылка вместо превью запрещена" }
        val shouldGenerate = !Files.isRegularFile(thumbnailPath, NOFOLLOW_LINKS) ||
                Files.getLastModifiedTime(thumbnailPath) < originalModified

        if (shouldGenerate) {
            val sourceImage = loadFirstPage(filePath)
            val thumbnail = resize(sourceImage, 320)

            val tempPath = Files.createTempFile(
                filePath.parent,
                ".notely-preview-",
                ".png"
            )

            try {
                check(ImageIO.write(thumbnail, "png", tempPath.toFile()))

                try {
                    Files.move(
                        tempPath,
                        thumbnailPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (exception: java.nio.file.AtomicMoveNotSupportedException) {
                    Files.move(
                        tempPath,
                        thumbnailPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } finally {
                Files.deleteIfExists(tempPath)
            }
        }

        val image = ImageIO.read(thumbnailPath.toFile())
            ?: error("Не удалось загрузить превью")

        return image.toComposeImageBitmap()
    }

    private fun loadFirstPage(filePath: Path): BufferedImage {
        return when (
            filePath.fileName.toString()
                .substringAfterLast('.')
                .lowercase()
        ) {
            "pdf" -> Loader.loadPDF(filePath.toFile()).use { document ->
                require(document.numberOfPages > 0) {
                    "PDF не содержит страниц"
                }

                PDFRenderer(document).renderImageWithDPI(
                    0,
                    PdfService.safeDpi(
                        document.getPage(0).cropBox.width.toDouble(),
                        document.getPage(0).cropBox.height.toDouble(),
                        72.0
                    )
                )
            }

            "jpg", "jpeg", "png" -> {
                SafeImageReader.read(filePath, 1024)
            }

            else -> error("Неподдерживаемый формат")
        }
    }


    private fun resize(
        image: BufferedImage,
        targetWidth: Int
    ): BufferedImage {
        val cropSize = minOf(image.width, image.height)

        val cropX = (image.width - cropSize) / 2
        val cropY = 0

        val cropped = image.getSubimage(
            cropX,
            cropY,
            cropSize,
            cropSize
        )

        val result = BufferedImage(
            targetWidth,
            targetWidth,
            BufferedImage.TYPE_INT_RGB
        )

        val graphics = result.createGraphics()

        try {
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            )

            graphics.drawImage(
                cropped,
                0,
                0,
                targetWidth,
                targetWidth,
                null
            )
        } finally {
            graphics.dispose()
        }

        return result
    }
}
