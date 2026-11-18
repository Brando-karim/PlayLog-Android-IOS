package com.playlog.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

/**
 * Prepares a profile avatar the same way the web profile editor does:
 * centre-crop to a square, resize to 512x512, and encode as WebP.
 */
object ProfileImage {
    private const val TARGET_SIZE = 512
    private const val MAX_INPUT_BYTES = 5L * 1024 * 1024
    private const val MAX_PIXELS = 40_000_000L
    private const val MAX_OUTPUT_BYTES = 2 * 1024 * 1024

    fun prepare(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val inputBytes = runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull()
        if (inputBytes != null && inputBytes > MAX_INPUT_BYTES) {
            throw IllegalArgumentException("Choose an image smaller than 5 MB.")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsInput = resolver.openInputStream(uri)
            ?: throw IllegalArgumentException("That image could not be opened.")
        boundsInput.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("That image could not be read. Choose another photo.")
        }
        if (bounds.outWidth.toLong() * bounds.outHeight > MAX_PIXELS) {
            throw IllegalArgumentException("That image is too large. Choose a smaller image.")
        }

        val orientation = resolver.openInputStream(uri)?.use { input ->
            runCatching {
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalArgumentException("That image could not be opened.")

        var oriented: Bitmap? = null
        var square: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            oriented = applyOrientation(decoded, orientation)
            val side = minOf(oriented.width, oriented.height)
            square = Bitmap.createBitmap(
                oriented,
                (oriented.width - side) / 2,
                (oriented.height - side) / 2,
                side,
                side,
            )
            scaled = if (square.width == TARGET_SIZE && square.height == TARGET_SIZE) {
                square
            } else {
                Bitmap.createScaledBitmap(square, TARGET_SIZE, TARGET_SIZE, true)
            }

            val output = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            if (!scaled.compress(format, 86, output)) {
                throw IllegalArgumentException("That image could not be prepared. Choose another photo.")
            }
            val bytes = output.toByteArray()
            if (bytes.size > MAX_OUTPUT_BYTES) {
                throw IllegalArgumentException("The resized photo is larger than 2 MB. Choose a simpler image.")
            }
            return bytes
        } finally {
            if (scaled != null && scaled !== square) scaled.recycle()
            if (square != null && square !== oriented) square.recycle()
            if (oriented != null && oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= 1024 && height / (sample * 2) >= 1024) {
            sample *= 2
        }
        return sample
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
