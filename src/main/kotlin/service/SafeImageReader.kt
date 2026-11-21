package com.notely.service

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.sqrt

object SafeImageReader {
    private const val MAX_SOURCE_PIXELS = 80_000_000L
    private const val MAX_OUTPUT_PIXELS = 12_000_000L

    fun read(file: Path, maxSide: Int = 4000): BufferedImage {
        Files.newInputStream(file).use { stream ->
            ImageIO.createImageInputStream(stream).use { input ->
                requireNotNull(input) { "Не удалось открыть изображение" }
                val readers = ImageIO.getImageReaders(input)
                require(readers.hasNext()) { "Формат изображения не распознан" }
                val reader = readers.next()
                try {
                    reader.input = input
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    val pixels = width.toLong() * height.toLong()
                    require(width > 0 && height > 0 && pixels <= MAX_SOURCE_PIXELS) {
                        "Изображение слишком большое"
                    }
                    val factor = maxOf(
                        1,
                        ceil(maxOf(width, height).toDouble() / maxSide).toInt(),
                        ceil(sqrt(pixels.toDouble() / MAX_OUTPUT_PIXELS)).toInt()
                    )
                    val params = reader.defaultReadParam
                    params.setSourceSubsampling(factor, factor, 0, 0)
                    return reader.read(0, params) ?: error("Изображение не удалось прочитать")
                } finally {
                    reader.dispose()
                }
            }
        }
    }
}
