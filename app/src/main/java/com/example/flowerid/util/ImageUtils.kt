package com.example.flowerid.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/** Loads, downscales and JPEG-compresses images picked from the camera or gallery. */
object ImageUtils {

    fun toJpegBase64(context: Context, uri: Uri, maxEdge: Int, quality: Int): String {
        val bytes = toJpegBytes(context, uri, maxEdge, quality)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun toJpegBytes(context: Context, uri: Uri, maxEdge: Int, quality: Int): ByteArray {
        val bitmap = decodeScaled(context, uri, maxEdge)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun decodeScaled(context: Context, uri: Uri, maxEdge: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                if (w > 0 && h > 0) {
                    val scale = minOf(1f, maxEdge.toFloat() / maxOf(w, h).toFloat())
                    if (scale < 1f) {
                        decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                    }
                }
            }
        } else {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: error("无法读取图片")
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            bmp = applyExifRotation(bmp, orientation)
            bmp = scaleToMaxEdge(bmp, maxEdge)
            bmp
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        if (width <= 0 || height <= 0) return sample
        while (maxOf(width / sample, height / sample) > maxEdge * 2) {
            sample *= 2
        }
        return sample
    }

    private fun scaleToMaxEdge(bmp: Bitmap, maxEdge: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val scale = minOf(1f, maxEdge.toFloat() / maxOf(w, h).toFloat())
        if (scale >= 1f) return bmp
        val scaled = Bitmap.createScaledBitmap(bmp, (w * scale).toInt(), (h * scale).toInt(), true)
        if (scaled != bmp) bmp.recycle()
        return scaled
    }

    private fun applyExifRotation(bmp: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bmp
        }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        if (rotated != bmp) bmp.recycle()
        return rotated
    }
}
