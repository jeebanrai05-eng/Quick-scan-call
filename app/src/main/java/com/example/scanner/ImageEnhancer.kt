package com.example.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

object ImageEnhancer {

    /**
     * Safely downscales large camera or gallery bitmaps to prevent OutOfMemoryError
     * on memory-constrained devices while maintaining OCR legibility.
     */
    fun safeDownscale(bitmap: Bitmap, maxDim: Int = 1800): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (ratio > 1) {
            targetWidth = maxDim
            targetHeight = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDim
            targetWidth = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Rotates image by specified degrees (e.g. 90, 180, 270) to fix upside-down or tilted documents.
     */
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Crops image to the specified normalized (0..1) or pixel bounding rectangle.
     */
    fun crop(bitmap: Bitmap, rect: RectF, isNormalized: Boolean = false): Bitmap {
        val x: Int
        val y: Int
        val w: Int
        val h: Int

        if (isNormalized) {
            x = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            y = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            w = ((rect.right - rect.left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
            h = ((rect.bottom - rect.top) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
        } else {
            x = rect.left.toInt().coerceIn(0, bitmap.width - 1)
            y = rect.top.toInt().coerceIn(0, bitmap.height - 1)
            w = rect.width().toInt().coerceIn(1, bitmap.width - x)
            h = rect.height().toInt().coerceIn(1, bitmap.height - y)
        }

        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    /**
     * Boosts contrast and sharpens faint / blurry text.
     */
    fun enhanceContrast(bitmap: Bitmap, contrast: Float = 1.6f, brightness: Float = -20f): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val colorMatrix = ColorMatrix().apply {
            val scale = contrast
            val translate = brightness
            set(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            isAntiAlias = true
            isFilterBitmap = true
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Adjusts brightness and contrast for user-guided manual tuning.
     */
    fun adjustBrightnessAndContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        return enhanceContrast(bitmap, contrast = contrast, brightness = brightness)
    }

    /**
     * Sharpens image edges using a high-pass unsharp mask convolution kernel.
     */
    fun sharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 3x3 Laplacian sharpening kernel: center = 5, cardinals = -1
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x

                val cCenter = pixels[idx]
                val cTop = pixels[idx - width]
                val cBottom = pixels[idx + width]
                val cLeft = pixels[idx - 1]
                val cRight = pixels[idx + 1]

                val r = (5 * (cCenter shr 16 and 0xFF) - (cTop shr 16 and 0xFF) - (cBottom shr 16 and 0xFF) - (cLeft shr 16 and 0xFF) - (cRight shr 16 and 0xFF)).coerceIn(0, 255)
                val g = (5 * (cCenter shr 8 and 0xFF) - (cTop shr 8 and 0xFF) - (cBottom shr 8 and 0xFF) - (cLeft shr 8 and 0xFF) - (cRight shr 8 and 0xFF)).coerceIn(0, 255)
                val b = (5 * (cCenter and 0xFF) - (cTop and 0xFF) - (cBottom and 0xFF) - (cLeft and 0xFF) - (cRight and 0xFF)).coerceIn(0, 255)

                resultPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Denoises document images using a 3x3 smoothing filter to remove grain and sensor noise.
     */
    fun denoise(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                var rSum = 0
                var gSum = 0
                var bSum = 0

                for (dy in -1..1) {
                    val subOffset = (y + dy) * width
                    for (dx in -1..1) {
                        val p = pixels[subOffset + (x + dx)]
                        rSum += (p shr 16) and 0xFF
                        gSum += (p shr 8) and 0xFF
                        bSum += p and 0xFF
                    }
                }

                resultPixels[rowOffset + x] = (0xFF shl 24) or
                        ((rSum / 9) shl 16) or
                        ((gSum / 9) shl 8) or
                        (bSum / 9)
            }
        }

        output.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Converts to high-contrast grayscale with gamma boost to extract faded or damaged numbers.
     */
    fun enhanceGrayscale(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val matrix = ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.5f, 0f, 0f, 0f, -30f,
                        0f, 1.5f, 0f, 0f, -30f,
                        0f, 0f, 1.5f, 0f, -30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
            isAntiAlias = true
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Inverts image colors for white/bright text printed on dark cards, black paper or neon boards.
     */
    fun invertColors(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val invertMatrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(invertMatrix)
            isAntiAlias = true
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * High-speed adaptive threshold binarization to clean paper texture, shadows, and watermarks.
     */
    fun binarize(bitmap: Bitmap, threshold: Int = 135): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            pixels[i] = if (lum < threshold) Color.BLACK else Color.WHITE
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Automatic document edge detection based on luminance gradients.
     * Returns normalized bounding RectF of the document content area.
     */
    fun detectDocumentEdges(bitmap: Bitmap): RectF {
        val w = bitmap.width
        val h = bitmap.height
        val stepX = max(1, w / 40)
        val stepY = max(1, h / 40)

        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h step stepY) {
            val rowOffset = y * w
            for (x in 0 until w step stepX) {
                val color = pixels[rowOffset + x]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                // Contrast difference against dark/light surroundings
                if (lum in 50..220) {
                    minX = min(minX, x)
                    maxX = max(maxX, x)
                    minY = min(minY, y)
                    maxY = max(maxY, y)
                }
            }
        }

        return if (maxX > minX && maxY > minY) {
            RectF(
                minX.toFloat() / w,
                minY.toFloat() / h,
                maxX.toFloat() / w,
                maxY.toFloat() / h
            )
        } else {
            RectF(0.08f, 0.12f, 0.92f, 0.88f)
        }
    }

    /**
     * 4-point perspective warp correction for tilted documents.
     */
    fun correctPerspective(bitmap: Bitmap, srcCorners: FloatArray): Bitmap {
        if (srcCorners.size != 8) return bitmap
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val dstCorners = floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return output
    }

    /**
     * Rescales low-res or far-away image regions so small phone numbers become readable by OCR.
     */
    fun upscaleIfSmall(bitmap: Bitmap, targetMinWidth: Int = 1200): Bitmap {
        if (bitmap.width >= targetMinWidth) return bitmap
        val scale = targetMinWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetMinWidth, newHeight, true)
    }
}
