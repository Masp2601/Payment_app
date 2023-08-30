package com.softmed.payment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.helpers.FilesHelper
import com.softmed.payment.helpers.InternetHelper
import com.softmed.payment.helpers.ReportExcelHelpers
import com.softmed.payment.reports.BillsReportActivity
import com.softmed.payment.reports.DetailsByActivity
import com.softmed.payment.reports.DetailsByDayReportActivity
import com.softmed.payment.reports.TransactionByDateReportActivity
import com.softmed.payment.storage.*
import kotlinx.android.synthetic.main.activity_report.*
import kotlinx.android.synthetic.main.content_report.*
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import java.io.File

class ReportActivity : BaseActivity(), AnkoLogger {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private var fileToSend: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        setSupportActionBar(toolbar)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "Report Selection View", null)

        val isLimited = isApplicationLimited()
        val reportList = resources.getStringArray(R.array.report_list)
        val reportAdapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                reportList)
        {
            override fun isEnabled(position: Int): Boolean {
                if (isLimited) {
                    if (position > 2) {
                        return false
                    }
                }
                return true
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                if (position > 2 && isLimited) {
                    view.setBackgroundColor(Color.argb(10, 60, 60, 60))
                } else {
                    view.setBackgroundColor(Color.WHITE)
                }

                return view
            }
        }

        reportsListView.adapter = reportAdapter
        reportsListView.setOnItemClickListener {
            _, _, i, _ ->
            when(i) {
                0 -> {
                    val intent = Intent(this, TransactionByDateReportActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                1 -> {
                    val intent = Intent(this, BillsReportActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                2 -> {
                    val intent = Intent(this, DetailsByActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                3 -> {
                    val intent = Intent(this, DetailsByDayReportActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                4 -> onSendReportSelected(4)
                5 -> onSendReportSelected(5)
                6 -> onSendReportSelected(6)
            }
        }

        checkForPermissions()
    }

    private fun onSendReportSelected(id: Int) {
        showProgressBar()
        val internetHelper = InternetHelper.getInstance(applicationContext)
        if (!internetHelper.deviceHasInternetConnection()) {
            runOnUiThread {
                Toast.makeText(this, R.string.error_no_internet_connection, Toast.LENGTH_LONG).show()
            }
            hideProgressBar()
            return
        }
        when (id) {
            4 -> {
                internetHelper.checkIfLicenseIsValid({
                    isValid ->
                    if (isValid) {
                        doAsync {
                            val clients = ClientesDbHelper.getInstance(applicationContext).getAll() ?: return@doAsync
                            val filename = getString(R.string.file_name_all_clients_list)
                            val report = ReportExcelHelpers(applicationContext).createClientList(clients) ?: return@doAsync

                            sendReport(filename, report)
                        }
                    } else {
                        logout()
                    }
                }, checkLicenseError)
            }
            5 -> {
                internetHelper.checkIfLicenseIsValid({
                    isValid ->
                    if (isValid) {
                        doAsync {
                            val service = ServiciosDbHelper.getInstance(applicationContext).getAll() ?: return@doAsync
                            val filename = getString(R.string.file_name_all_services_list)
                            val report = ReportExcelHelpers(applicationContext).createServiceList(service) ?: return@doAsync

                            sendReport(filename, report)
                        }
                    } else {
                        logout()
                    }
                }, checkLicenseError)
            }
            6 -> {
                internetHelper.checkIfLicenseIsValid({
                    isValid ->
                    if (isValid) {
                        doAsync {
                            val bills = InvoiceDbHelper.getInstance(applicationContext).getAll() ?: return@doAsync
                            val items = InvoiceItemsDbHelper.getInstance(applicationContext).getAll() ?: return@doAsync
                            val payments = TransactionDbHelper.getInstance(applicationContext).getAll() ?: return@doAsync

                            val filename = getString(R.string.file_name_all_bills_with_items)
                            val report = ReportExcelHelpers(applicationContext).createSalesList(bills, items, payments) ?: return@doAsync

                            sendReport(filename, report)
                        }
                    } else {
                        logout()
                    }
                }, checkLicenseError)
            }
            7 -> {
                internetHelper.checkIfLicenseIsValid({
                    isValid ->
                    if (isValid) {
                        doAsync {
                            val expenses = ExpensesDbHelper.getInstance(applicationContext).getAll() ?: return@doAsync
                            val filename = getString(R.string.file_name_all_expenses)
                            val report = ReportExcelHelpers(applicationContext).createExpensesList(expenses) ?: return@doAsync
                            sendReport(filename, report)
                        }
                    } else {
                        logout()
                    }
                }, checkLicenseError)
            }

        }
    }

    private fun showProgressBar() {
        runOnUiThread { progressBar.visibility = View.VISIBLE }
    }

    private fun hideProgressBar() {
        runOnUiThread { progressBar.visibility = View.GONE }
    }

    private val checkLicenseError = Response.ErrorListener { error: VolleyError? ->
        error(error?.message)
        runOnUiThread {
            Toast.makeText(this, R.string.error_report_license_check, Toast.LENGTH_LONG).show()
            hideProgressBar()
        }
    }

    private fun logout() {
        val preference = getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
        val editor = preference.edit()
        editor.putBoolean(getString(R.string.pref_value_is_application_registered), false)
        editor.apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun sendReport(filename: String, report: HSSFWorkbook) {
        val fileHelper = FilesHelper(applicationContext)
        val file = fileHelper.getCacheFile(filename) ?: return
        val fileStream = fileHelper.getCacheStream(file) ?: return

        report.write(fileStream)
        report.close()
        fileStream.close()

        fileToSend = file
        val intent = fileHelper.intentFileToSend(file)

        hideProgressBar()
        startActivityForResult(intent, SEND_FILE_CODE)
    }

    private fun checkForPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            val arrayOfPermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NETWORK_STATE)
            ActivityCompat.requestPermissions(this, arrayOfPermission, PERMISSION_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEND_FILE_CODE){
            FilesHelper.deleteFileWithDelay(fileToSend)
        }
    }

    companion object {
        val SEND_FILE_CODE = 112
    }
}
