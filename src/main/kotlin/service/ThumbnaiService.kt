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

class ThumbnailService {

    fun getThumbnail(filePath: Path): ImageBitmap {
        val thumbnailPath = filePath.parent.resolve(
            ".notely-preview.png"
        )

        val originalModified = Files.getLastModifiedTime(filePath)

        val shouldGenerate = !Files.isRegularFile(thumbnailPath) ||
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

                PDFRenderer(document).renderImageWithDPI(0, 72f)
            }

            "jpg", "jpeg", "png" -> {
                ImageIO.read(filePath.toFile())
                    ?: error("Не удалось прочитать изображение")
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
