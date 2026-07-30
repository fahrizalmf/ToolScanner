package com.tollscan.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    /** Creates a new empty file under filesDir/receipts to hold a captured photo. */
    fun createImageFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
        return File(dir, "STRUK_$timestamp.jpg")
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Loads a downsampled bitmap from disk so large photos don't blow up memory. */
    fun loadBitmap(path: String, maxDimension: Int = 2000): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        var sample = 1
        while (boundsOptions.outWidth / sample > maxDimension || boundsOptions.outHeight / sample > maxDimension) {
            sample *= 2
        }

        val finalOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, finalOptions)
    }

    fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
    }

    /**
     * Reads EXIF orientation off a just-captured photo and returns an upright bitmap.
     * Camera photos are very often stored rotated 90/180/270 degrees.
     */
    fun fixOrientation(context: Context, file: File): Bitmap {
        val original = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalStateException("Tidak dapat membaca file foto")

        val orientation = try {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (degrees == 0f) return original

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    }
}
