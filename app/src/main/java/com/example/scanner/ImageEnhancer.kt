package com.example.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object ImageEnhancer {

    /**
     * Boosts contrast and sharpens faint / blurry ("madhuro") text.
     */
    fun enhanceContrast(bitmap: Bitmap, contrast: Float = 1.6f, brightness: Float = -20f): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val colorMatrix = ColorMatrix().apply {
            // Apply contrast scale and brightness shift
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
     * Converts to high-contrast grayscale with gamma boost to extract faded or damaged numbers.
     */
    fun enhanceGrayscale(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val matrix = ColorMatrix().apply {
            setSaturation(0f) // convert to grayscale
            // Multiply luminance contrast
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
            // Luminance formula
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            pixels[i] = if (lum < threshold) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Rescales low-res or far-away image regions so small phone numbers become readable by OCR.
     */
    fun upscaleIfSmall(bitmap: Bitmap, targetMinWidth: Int = 1200): Bitmap {
        if (bitmap.width >= targetMinWidth) return bitmap
        val scale = targetMinWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetMinWidth, newHeight, true)
    }
}
