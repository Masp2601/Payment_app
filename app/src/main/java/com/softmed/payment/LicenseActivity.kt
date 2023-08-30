package com.softmed.payment

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.helpers.InternetHelper
import kotlinx.android.synthetic.main.activity_license.*
import org.jetbrains.anko.error
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

class LicenseActivity : BaseActivity() {

    private val DB_FILEPATH = "/data/data/{package_name}/databases/database.db";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val licenseType = preference.getInt(getString(R.string.pref_value_license_type_id), 0)
        val expirationDate = preference.getLong(getString(R.string.pref_value_license_expiration_date), 0)
        val date = Date(expirationDate)

        licenseTypeValue.text = getLicenseName(licenseType)
        expirationDateValue.text = DateTimeHelper.dateToString(date, "dd/MM/yyyy")
        promotionCodeApplyButton.setOnClickListener {
            this.applyPromotionCode()
        }

        exportBackupButton.setOnClickListener { this.exportBackup() }
        importBackupButton.setOnClickListener { this.importBackup() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                val uri = data!!.data!!
                val dbFolder = getDatabasePath(DATABASE_NAME)

                if (dbFolder.canWrite()) {
                    val currentDb = contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor

                    if (currentDb != null && currentDb.valid()) {
                        val src = FileInputStream(currentDb).channel
                        val dst = FileOutputStream(dbFolder).channel
                        dst.transferFrom(src, 0, src.size())
                        src.close()
                        dst.close()

                        Toast.makeText(this, getString(R.string.import_backup_success), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                error(e.message)
            }
        }
    }

    private fun applyPromotionCode() {
        val code = promotionCode.text

        if (code.isNullOrEmpty()) {
            return
        }

        val queue = InternetHelper.getInstance(applicationContext)
        val url = "${LICENSE_URL}/api/promotion-code/apply"
        val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val email = preference.getString(getString(R.string.pref_value_email_registered), "")

        val parameters = JSONObject()
        parameters.put("Username", email)
        parameters.put("PromotionCode", code.toString())
        val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                parameters,
                responseSuccess,
                responseError
        )
        queue.executeRequest(request)
    }

    private fun exportBackup() {
        try {
            val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadFolder.canWrite()) {
                val currentDb = getDatabasePath(DATABASE_NAME)
                val backupDB = File(downloadFolder, DATABASE_NAME)

                if (currentDb.exists()) {
                    val src = FileInputStream(currentDb).channel
                    val dst = FileOutputStream(backupDB).channel
                    dst.transferFrom(src, 0 , src.size())
                    src.close()
                    dst.close()

                    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    downloadManager.addCompletedDownload(
                            "Backup DB",
                            "DB",
                            false,
                            "application/octet-stream",
                            backupDB.absolutePath,
                            backupDB.length(),
                            true)
                }

                Toast.makeText(this, getString(R.string.export_backup_success), Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            error(e.message)
        }
    }

    private fun importBackup() {
        val chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        chooseFileIntent.type = "*/*"
        val chooseFile = Intent.createChooser(chooseFileIntent, "Choose a file")
        startActivityForResult(chooseFile, 123)
        /*
        try {
            val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dbFolder = getDatabasePath(DATABASE_NAME)
            if (dbFolder.canWrite()) {
                val currentDb = File(downloadFolder, DATABASE_NAME)

                val src = FileInputStream(currentDb).channel
                val dst = FileOutputStream(dbFolder).channel
                dst.transferFrom(src, 0, src.size())
                src.close()
                dst.close()

                Toast.makeText(this, getString(R.string.import_backup_success), Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            error(e.message)
        }
        */
    }

    private val responseSuccess = Response.Listener<JSONObject> {
        val success = it.getBoolean("Success")
        val responseCode = it.getInt("Code")
        if (success && responseCode == 200) {
            val data = it.getJSONObject("Data")
            val date = DateTimeHelper.parseStringToDate(
                    data.getString("ExpirationDate"),
                    DateTimeHelper.PATTERN_REQUEST_RESPONSE
            )
            val today = Date()
            val licenseType = data.getInt("LicenseTypeId")
            val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = preference.edit()
            editor.putLong(getString(R.string.pref_value_license_expiration_date), date.time)
            editor.putInt(getString(R.string.pref_value_license_type_id), licenseType)
            editor.putBoolean(getString(R.string.pref_value_application_is_limited), date.after(today))
            editor.apply()

            licenseTypeValue.text = getLicenseName(licenseType)
            expirationDateValue.text = DateTimeHelper.dateToString(date, "dd/MM/yyyy")
            promotionCode.text = null

            Toast.makeText(this, getString(R.string.license_promotion_code_success), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.license_promotion_code_denied), Toast.LENGTH_LONG).show()
        }
    }

    private val responseError = Response.ErrorListener {
        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
    }

    fun getLicenseName(type: Int): String {
        return when(type) {
            1 -> getString(R.string.license_type_free)
            10 -> getString(R.string.license_type_distributor)
            50 -> getString(R.string.license_type_annual)
            51 -> getString(R.string.license_type_monthly)
            60 -> getString(R.string.license_type_free)
            99 -> "99"
            else -> getString(R.string.license_type_free)
        }
    }
}
