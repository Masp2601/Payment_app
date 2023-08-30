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
import com.softmed.payment.adapters.BillsListAdapter
import com.softmed.payment.BaseActivity
import com.softmed.payment.LoginActivity
import com.softmed.payment.R
import com.softmed.payment.adapters.BillsListRVAdapter
import com.softmed.payment.helpers.*
import com.softmed.payment.storage.InvoiceContract
import com.softmed.payment.storage.InvoiceDbHelper
import kotlinx.android.synthetic.main.activity_bills_report.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*

class BillsReportActivity : BaseActivity() {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private val calendarSince: Calendar = Calendar.getInstance()
    private val calendarEnd: Calendar = Calendar.getInstance()

    private lateinit var mAdapter: BillsListRVAdapter
    private lateinit var mLayoutManager: LinearLayoutManager

    private var billList: List<InvoiceContract.Invoice>? = null
    var fileToSend: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_bills_report)
        super.onCreate(savedInstanceState)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

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
                runOnUiThread {
                    Toast.makeText(this, R.string.error_no_internet_connection, Toast.LENGTH_LONG).show()
                }
                hideProgress()
                return@setOnClickListener
            }
            internetHelper.checkIfLicenseIsValid({isValid -> checkLicense(isValid)}, checkLicenseError)
        }


        mAdapter = BillsListRVAdapter()
        mLayoutManager = LinearLayoutManager(this)
        rvBills.layoutManager = mLayoutManager
        rvBills.adapter = mAdapter
        rvBills.addItemDecoration(DividerItemDecoration(this))
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

    private fun showResume(){
        progressBar.visibility = View.VISIBLE
        fabSendEmail.hide()
        doAsync {
            val end = DateTimeHelper.getEndDate(calendarEnd)
            val invoices = InvoiceDbHelper.getInstance(applicationContext).filterByDates(calendarSince.time.time, end.time.time)
            uiThread {
                val total = invoices?.sumByDouble {
                    if (it.nullify == 0)
                        it.total
                    else
                        0.0
                } ?: 0.0
                reportTotalPaid.setText(total)
                reportTotalOfTicket.text = "${invoices?.size ?: 0}"

                mAdapter.update(invoices)
                rvBills.scheduleLayoutAnimation()

                progressBar.visibility = View.GONE

                if (!isApplicationLimited() && invoices !== null) {
                    fabSendEmail.show()
                } else {
                    fabSendEmail.hide()
                }
            }
            billList = invoices
        }
    }

    private fun checkLicense(isValid: Boolean) {
        if (isValid) {
            if (billList == null) return
            val filename = getString(R.string.file_name_report_invoices_dates, DateTimeHelper.dateToString(calendarSince.time), DateTimeHelper.dateToString(calendarEnd.time))
            val fileHelper = FilesHelper(applicationContext)
            val file = fileHelper.getCacheFile(filename) ?: return
            val outputStream = fileHelper.getCacheStream(file) ?: return

            doAsync {
                val success = ReportExcelHelpers(applicationContext).reportInvoices(billList!!, outputStream)

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
