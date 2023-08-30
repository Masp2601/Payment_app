package com.softmed.payment.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.softmed.payment.BuildConfig
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FilesHelper(val ctx: Context) : AnkoLogger {
    companion object {
        private val DIRECTORY_TO_SAVE: String = Environment.DIRECTORY_DOCUMENTS

        @JvmStatic fun deleteFileWithDelay(file: File?) {
            try {
                val delay = 60 * 1000L // one minute
                // Delay the files' delete to ensure that it has been send.
                android.os.Handler().postDelayed( { file?.delete() }, delay)
            } catch (e: Exception) { }
        }
    }

    init {
        createDirectoryIsNotExists()
    }

    fun getFile(filename: String) : File  = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_TO_SAVE), filename)

    fun getOutputStream(filename: String) : FileOutputStream? {
        return try {
            val outputStream = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_TO_SAVE), filename)
            FileOutputStream(outputStream)
        } catch (e: IOException) {
            error(e.message)
            null
        } catch (e: Exception) {
            error(e.message)
            null
        }
    }

    fun getCacheFile(filename: String) : File? {
        return try {
            File(ctx.getExternalFilesDir(null), filename)
        } catch (e: IOException) {
            error(e.message)
            null
        } catch (e: Exception) {
            error(e.message)
            null
        }
    }

    fun getCacheStream(file: File): FileOutputStream? {
        return try {
            FileOutputStream(file)
        } catch (e: IOException) {
            error(e.message)
            null
        } catch (e: Exception) {
            error(e.message)
            null
        }
    }

    fun intentFileToSend(file: File): Intent? {
        val intent = Intent(Intent.ACTION_SEND)
        val uri = FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".provider", file)
        intent.type = "application/excel"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(Intent.EXTRA_SUBJECT, file.name)

        return Intent.createChooser(intent, "Email")
    }

    private fun createDirectoryIsNotExists() {
        val directory = Environment.getExternalStoragePublicDirectory(DIRECTORY_TO_SAVE)
        if (!(directory.exists() || directory.mkdirs())) {
            error("Failed to create directory ${directory.absolutePath}")
        }
    }
}