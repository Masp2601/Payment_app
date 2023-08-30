package com.softmed.payment.reports

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.BaseAdapter
import android.widget.Toast
import com.softmed.payment.adapters.PurchaseListAdapter
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.helpers.FilesHelper
import com.softmed.payment.helpers.ReportExcelHelpers
import com.softmed.payment.R
import com.softmed.payment.storage.ExpenseContract
import com.softmed.payment.storage.ExpensesDbHelper
import kotlinx.android.synthetic.main.activity_purchase_report.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*

class ExpenseReportActivity : AppCompatActivity() {

    private val calendarSince: Calendar = Calendar.getInstance()
    private val calendarEnd: Calendar = Calendar.getInstance()
    private var adapter: BaseAdapter? = null
    private var expenseList: List<ExpenseContract.Expense>? = null
    var fileToSend: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_report)

        if (savedInstanceState != null) {
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

        btnShowResume.setOnClickListener {
            showResume()
        }

        fabSendEmail.setOnClickListener {
            if (expenseList == null) return@setOnClickListener
            val filename = getString(R.string.file_name_report_purchases_dates, DateTimeHelper.dateToString(calendarSince.time), DateTimeHelper.dateToString(calendarEnd.time))
            val fileHelper = FilesHelper(applicationContext)
            val file = fileHelper.getCacheFile(filename) ?: return@setOnClickListener
            val outputStream = fileHelper.getCacheStream(file) ?: return@setOnClickListener

            doAsync {
                val success = ReportExcelHelpers(applicationContext).reportPurchases(expenseList!!, outputStream)

                if (success) {
                    if (file.exists()) {
                        fileToSend = file
                        startActivityForResult(fileHelper.intentFileToSend(file), SEND_FILE_CODE)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fileToSend?.delete()
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
        fabSendEmail.visibility = View.GONE
        doAsync {
            val end = DateTimeHelper.getEndDate(calendarEnd)
            val purchases = ExpensesDbHelper.getInstance(applicationContext).filterByDates(calendarSince.time.time, end.time.time)
            if (purchases != null) {
                adapter = PurchaseListAdapter(this@ExpenseReportActivity, purchases)
                uiThread {
                    purchasesListView.adapter = adapter
                    progressBar.visibility = View.GONE
                    fabSendEmail.visibility = View.VISIBLE
                }
            }
            expenseList = purchases
        }
    }

    private fun showFileSavedMessage(success: Boolean) {
        when(success) {
            true -> {
                runOnUiThread {
                    Toast.makeText(this, getText(R.string.file_saved_success), Toast.LENGTH_SHORT).show()
                }
            }
            false -> {
                runOnUiThread {
                    Toast.makeText(this, getText(R.string.file_saved_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState?.putLong(DATE_SINCE, calendarSince.timeInMillis)
        outState?.putLong(DATE_END, calendarEnd.timeInMillis)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private val DATE_SINCE = "date_since"
        private val DATE_END = "date_end"
        private val SEND_FILE_CODE = 102
    }
}
