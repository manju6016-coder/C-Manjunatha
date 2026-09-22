package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
        return try {
            val dir = File(context.filesDir, "challan_photos")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "challan_cam_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri): String {
        return try {
            val dir = File(context.filesDir, "challan_photos")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "challan_gal_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
    }
}
