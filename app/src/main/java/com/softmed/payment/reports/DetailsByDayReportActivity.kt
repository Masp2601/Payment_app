package com.softmed.payment.reports

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import android.widget.BaseAdapter
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.BaseActivity
import com.softmed.payment.LoginActivity
import com.softmed.payment.R
import com.softmed.payment.adapters.ItemsSoldListAdapter
import com.softmed.payment.adapters.ItemsSoldRVAdapter
import com.softmed.payment.helpers.*
import com.softmed.payment.storage.InvoiceItemsContract
import com.softmed.payment.storage.InvoiceItemsDbHelper
import kotlinx.android.synthetic.main.activity_details_by_day_report.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*

class DetailsByDayReportActivity : BaseActivity() {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mAdapter: ItemsSoldRVAdapter
    private lateinit var mLayoutManager: LinearLayoutManager

    private val calendarSince: Calendar = Calendar.getInstance()
    private val calendarEnd: Calendar = Calendar.getInstance()
    var fileToSend: File? = null
    private var resumeItemsSold: List<InvoiceItemsContract.InvoiceItemsTotalResumen>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_by_day_report)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        mAdapter = ItemsSoldRVAdapter()
        mLayoutManager = LinearLayoutManager(this)
        rvBills.adapter = mAdapter
        rvBills.layoutManager = mLayoutManager
        rvBills.addItemDecoration(DividerItemDecoration(this))

        if (savedInstanceState != null){
            calendarSince.timeInMillis = savedInstanceState.getLong(DATE_SINCE)
            calendarEnd.timeInMillis = savedInstanceState.getLong(DATE_END)
            showResume()
        }

        reportSince.isFocusable = false
        reportSince.keyListener = null
        reportSince.setOnClickListener {
            val picker = DatePickerFragment.getInstance(calendarSince.get(Calendar.YEAR), calendarSince.get(Calendar.MONTH), calendarSince.get(Calendar.DAY_OF_MONTH))
            picker.setOnDataSetListener(onDateStartSelected)
            picker.show(fragmentManager, "DatePickerStart")
        }
        reportEnd.isFocusable = false
        reportEnd.keyListener = null
        reportEnd.setOnClickListener {
            val picker = DatePickerFragment.getInstance(calendarEnd.get(Calendar.YEAR), calendarEnd.get(Calendar.MONTH), calendarEnd.get(Calendar.DAY_OF_MONTH))
            picker.setOnDataSetListener(onDateEndSelected)
            picker.show(fragmentManager, "DatePickerEnd")
        }

        btnShowBills.setOnClickListener {
            showResume()
        }

        fabSendEmail.setOnClickListener {
            showProgress()
            val internetHelper = InternetHelper.getInstance(applicationContext)
            if (!internetHelper.deviceHasInternetConnection()) {
                hideProgress()
                runOnUiThread {
                    Toast.makeText(this, R.string.error_no_internet_connection, Toast.LENGTH_LONG).show()
                }
                return@setOnClickListener
            }
            internetHelper.checkIfLicenseIsValid({isValid -> checkLicense(isValid)}, checkLicenseError)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            FilesHelper.deleteFileWithDelay(fileToSend)
        } catch (e: Exception) {
            error(e.message)
        }
    }

    private val onDateStartSelected = object : DatePickerFragment.DatePickerListener {
        override fun onDateSet(year: Int, month: Int, day: Int) {
            val date: Date = DateTimeHelper.parseStringToDate("$year.${month + 1}.$day", DateTimeHelper.PATTERN_SHORT)
            reportSince.setText(DateTimeHelper.dateToString(date, DateTimeHelper.PATTERN_LONG))
            calendarSince.time = date
        }
    }

    private val onDateEndSelected = object : DatePickerFragment.DatePickerListener {
        override fun onDateSet(year: Int, month: Int, day: Int) {
            val date: Date = DateTimeHelper.parseStringToDate("$year.${month + 1}.$day", DateTimeHelper.PATTERN_SHORT)
            reportEnd.setText(DateTimeHelper.dateToString(date, DateTimeHelper.PATTERN_LONG))
            calendarEnd.time = date
        }
    }

    private fun showResume() {
        progressBar.visibility = View.VISIBLE
        fabSendEmail.hide()
        doAsync {
            val end = DateTimeHelper.getEndDate(calendarEnd)
            val resumen = InvoiceItemsDbHelper.getInstance(applicationContext)
                    .getResumenItemSoldByDate(calendarSince.timeInMillis, end.timeInMillis)

            uiThread {
                val totalItems = resumen?.sumByDouble { it.itemAmountTotal } ?: 0.0
                val totalPaid = resumen?.sumByDouble { it.itemPriceTotal } ?: 0.0
                reportTotalOfItems.text = "$totalItems"
                reportTotalPaid.setText(totalPaid)

                mAdapter.update(resumen)
                rvBills.scheduleLayoutAnimation()

                if (resumen != null && !isApplicationLimited()) {
                    fabSendEmail.show()
                } else {
                    fabSendEmail.hide()
                }

                progressBar.visibility = View.GONE
            }

            resumeItemsSold = resumen
        }
    }

    private fun checkLicense(isValid: Boolean) {
        if (isValid) {
            if (resumeItemsSold == null) return
            val filename = getString(R.string.file_name_all_items_sold_by_date, DateTimeHelper.dateToString(calendarSince.time), DateTimeHelper.dateToString(calendarEnd.time))
            val fileHelper = FilesHelper(applicationContext)
            val file = fileHelper.getCacheFile(filename) ?: return
            val outputStream = fileHelper.getCacheStream(file) ?: return

            doAsync {
                val end = DateTimeHelper.getEndDate(calendarEnd)
                val itemsDetails = InvoiceItemsDbHelper.getInstance(applicationContext).getItemsByDate(calendarSince.timeInMillis, end.timeInMillis)
                val success = ReportExcelHelpers(applicationContext).reportResumeItemsSold(resumeItemsSold!!, itemsDetails, outputStream)

                if (success) {
                    if (file.exists()) {
                        fileToSend = file
                        startActivityForResult(fileHelper.intentFileToSend(file), SEND_FILE_CODE)
                    }
                }
            }
        } else {
            val preference = getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
            val editor = preference.edit()
            editor.putBoolean(getString(R.string.pref_value_is_application_registered), false)
            editor.apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
        hideProgress()
    }

    private val checkLicenseError = Response.ErrorListener { error: VolleyError? ->
        error(error?.message)
        runOnUiThread {
            Toast.makeText(this, R.string.error_report_license_check, Toast.LENGTH_LONG).show()
        }
        hideProgress()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState?.putLong(DATE_SINCE, calendarSince.timeInMillis)
        outState?.putLong(DATE_END, calendarEnd.timeInMillis)
        super.onSaveInstanceState(outState)
    }

    private fun showProgress() {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgress() {
        runOnUiThread {
            progressBar.visibility = View.GONE
        }
    }

    companion object {
        private val DATE_SINCE = "date_since"
        private val DATE_END = "date_end"
        private val SEND_FILE_CODE = 102
    }
}
