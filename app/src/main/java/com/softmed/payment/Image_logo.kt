package com.softmed.payment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.android.synthetic.main.activity_image_logo.imageView
import kotlinx.android.synthetic.main.activity_image_logo.saveImageButton
import kotlinx.android.synthetic.main.activity_image_logo.selectImageButton
import java.io.File
import java.io.FileOutputStream
import androidx.core.view.*
import androidx.core.graphics.drawable.toBitmap
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class Image_logo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_logo)
        selectImageButton.setOnClickListener {
            // Abre la galería para seleccionar una imagen
            ImagePicker.with(this)
                    .galleryOnly()
                    .galleryMimeTypes(arrayOf("image/png", "image/jpg", "image/jpeg"))
                    .crop()
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .start()
            //getImageFile()
        }

        saveImageButton.setOnClickListener {
            // Guarda la imagen en almacenamiento local
            saveImageLocally()
            //getImageFile()
        }
    }

    fun getImageFile(dir: File? = null, extension: String? = null): File? {
        try {
            // Create an image file name
            val ext = extension ?: ".jpg"
            val imageFileName = "IMG_${getTimestamp()}$ext"

            // Create File Directory Object
            val storageDir = dir ?: getCameraDirectory()

            // Create Directory If not exist
            if (!storageDir.exists()) storageDir.mkdirs()

            // Create File Object
            val file = File(storageDir, imageFileName)

            // Create empty file
            file.createNewFile()

            return file
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
    }

    /**
     * Get Camera Image Directory
     *
     * @return File Camera Image Directory
     */
    private fun getCameraDirectory(): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        return File(dir, "Camera")
    }

    /**
     * Get Current Time in yyyyMMdd HHmmssSSS format
     *
     * 2019/01/30 10:30:20 000
     * E.g. 20190130_103020000
     */
    private fun getTimestamp(): String {
        val timeFormat = "yyyyMMdd_HHmmssSSS"
        return SimpleDateFormat(timeFormat, Locale.getDefault()).format(Date())
    }

    private fun saveImageLocally() {
        val bitmap = imageView.drawable.toBitmap()
        val file = File(getExternalFilesDir(null), "my_image.jpg")
        val fos = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.flush()
        fos.close()

        // El archivo se guarda en getExternalFilesDir, que es el almacenamiento privado de la aplicación
    }
}