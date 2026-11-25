package com.localfoodreviews.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import java.io.ByteArrayOutputStream

/**
 * Spark-plan photo storage: a deliberately small JPEG data URI in Firestore.
 * The limit protects Firestore's 1 MiB document limit and is suitable only for a small demo app.
 */
object ImageCodec {
    private const val MAX_EDGE = 900
    private const val MAX_JPEG_BYTES = 330 * 1024

    fun toSmallDataUri(context: Context, uri: Uri): Result<String> = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decodeBitmap(resolver, uri, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            error("Unable to read this image. Please choose a JPG or PNG from Files or Gallery.")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = decodeBitmap(resolver, uri, options)
            ?: error("Unable to decode this image. Please choose another JPG or PNG photo.")

        val scaled = scaleToMax(decoded, MAX_EDGE)
        if (scaled !== decoded) decoded.recycle()
        val output = ByteArrayOutputStream()
        var encoded: ByteArray? = null
        for (quality in intArrayOf(78, 68, 58, 48, 38)) {
            output.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            if (output.size() <= MAX_JPEG_BYTES) {
                encoded = output.toByteArray()
                break
            }
        }
        scaled.recycle()
        val bytes = encoded ?: error("This photo is too detailed. Please choose a smaller photo.")
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * DocumentsUI/Gallery normally provides an input stream. The file-descriptor fallback
     * supports providers that return null from openInputStream (seen on some emulators).
     */
    private fun decodeBitmap(
        resolver: android.content.ContentResolver,
        uri: Uri,
        options: BitmapFactory.Options
    ): Bitmap? {
        resolver.openInputStream(uri)?.use { stream ->
            return BitmapFactory.decodeStream(stream, null, options)
        }
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            return BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)
        }
        return null
    }

    fun loadDataUri(imageView: ImageView, dataUri: String) {
        try {
            val payload = dataUri.substringAfter(',', dataUri)
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        } catch (_: Exception) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var largest = maxOf(width, height)
        while (largest / 2 >= MAX_EDGE) {
            sample *= 2
            largest /= 2
        }
        return sample
    }

    private fun scaleToMax(source: Bitmap, maxEdge: Int): Bitmap {
        val largest = maxOf(source.width, source.height)
        if (largest <= maxEdge) return source
        val ratio = maxEdge.toFloat() / largest.toFloat()
        return Bitmap.createScaledBitmap(source, (source.width * ratio).toInt(), (source.height * ratio).toInt(), true)
    }
}
