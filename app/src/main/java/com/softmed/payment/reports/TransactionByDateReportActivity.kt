package com.softmed.payment.reports

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.BaseActivity
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.helpers.FilesHelper
import com.softmed.payment.helpers.InternetHelper
import com.softmed.payment.helpers.ReportExcelHelpers
import com.softmed.payment.LoginActivity
import com.softmed.payment.R
import com.softmed.payment.storage.TransactionContract
import com.softmed.payment.storage.TransactionDbHelper
import kotlinx.android.synthetic.main.activity_report_transaction_by_date.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class TransactionByDateReportActivity : BaseActivity() {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private var calendar: Calendar = Calendar.getInstance()
    private var paymentsList: List<TransactionContract.Transaction>? = null
    private var fileToSend: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_transaction_by_date)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        selectDate.keyListener = null
        selectDate.isFocusable = false
        selectDate.setOnClickListener {
            val datePicker = DatePickerFragment.getInstance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.setOnDataSetListener(onDateSelected)
            datePicker.show(fragmentManager, "DatePicker")
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            FilesHelper.deleteFileWithDelay(fileToSend)
        } catch (e: Exception) {
            error(e.message)
        }
    }

    private fun getResume() {
        progressBar.visibility = View.VISIBLE
        doAsync {
            val payments = TransactionDbHelper.getInstance(applicationContext)
                    .getPayments(calendar)

            if (payments != null){
                val cashBills = payments.filter { it.paymentType == TransactionContract.PaymentMethods.Cash.ordinal }
                val cashTotalBills = cashBills.size
                val cash: Double = cashBills.sumByDouble { it.paymentTotal }

                val cardBills = payments.filter { it.paymentType == TransactionContract.PaymentMethods.Card.ordinal }
                val cardTotalBills = cardBills.size
                val cardTotal = cardBills.sumByDouble { it.paymentTotal }

                val checkBills = payments.filter { it.paymentType == TransactionContract.PaymentMethods.Check.ordinal }
                val checkTotalBills = checkBills.size
                val checkTotal = checkBills.sumByDouble { it.paymentTotal }

                val creditBills = payments.filter { it.paymentType == TransactionContract.PaymentMethods.Credit.ordinal }
                val creditTotalBills = creditBills.size
                val creditTotal = creditBills.sumByDouble { it.invoiceTotal - it.paymentCreditDeposit }
                val creditPaid = creditBills.sumByDouble { it.paymentCreditDeposit }

                uiThread {
                    frameResume.visibility = View.VISIBLE

                    if (isApplicationLimited()) {
                        fabSendEmail.hide()
                    } else {
                        fabSendEmail.show()
                    }

                    paymentCashTotalBillsValue.text = cashTotalBills.toString()
                    paymentCashTotalPayValue.text = cash.toString()
                    paymentCardTotalBillsValue.text = cardTotalBills.toString()
                    paymentCardTotalPayValue.text = cardTotal.toString()
                    paymentCheckTotalBillsValue.text = checkTotalBills.toString()
                    paymentCheckTotalPayValue.text = checkTotal.toString()
                    paymentCreditTotalBillsValue.text = creditTotalBills.toString()
                    paymentCreditTotalInCreditValue.text = creditTotal.toString()
                    paymentCreditTotalCreditPaidValue.text = creditPaid.toString()

                    paymentTotalBillsValue.text = (cashTotalBills + cardTotalBills + checkTotalBills + creditTotalBills).toString()
                    paymentTotalPayValue.text = (cash + cardTotal + checkTotal + creditTotal + creditPaid).toString()

                    val intent = Intent(this@TransactionByDateReportActivity, PaymentsListActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    btnShowCashResume.setOnClickListener {
                        intent.putExtra(PaymentsListActivity.EXTRA_PAYMENTS, ArrayList(cashBills))
                        startActivity(intent)
                    }
                    btnShowCardResume.setOnClickListener {
                        intent.putExtra(PaymentsListActivity.EXTRA_PAYMENTS, ArrayList(cardBills))
                        startActivity(intent)
                    }
                    btnShowCheckResume.setOnClickListener {
                        intent.putExtra(PaymentsListActivity.EXTRA_PAYMENTS, ArrayList(checkBills))
                        startActivity(intent)
                    }
                    btnShowCreditResume.setOnClickListener {
                        intent.putExtra(PaymentsListActivity.EXTRA_PAYMENTS, ArrayList(creditBills))
                        startActivity(intent)
                    }
                    btnShowTotalResume.setOnClickListener {
                        intent.putExtra(PaymentsListActivity.EXTRA_PAYMENTS, ArrayList(payments))
                        startActivity(intent)
                    }
                }

                paymentsList = payments
            }

            uiThread {
                progressBar.visibility = View.GONE
            }
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

    private val onDateSelected = object : DatePickerFragment.DatePickerListener {
        override fun onDateSet(year: Int, month: Int, day: Int) {
            val date = DateTimeHelper.parseStringToDate("$year.${month + 1}.$day", DateTimeHelper.PATTERN_SHORT)
            selectDate.setText(DateTimeHelper.dateToString(date, DateTimeHelper.PATTERN_LONG))
            calendar.time = date
            getResume()
        }
    }

    private fun checkLicense(isValid: Boolean) {
        if (isValid) {
            if (paymentsList == null) return
            val filename = getString(R.string.file_name_report_payments_day, DateTimeHelper.dateToString(calendar.time, DateTimeHelper.PATTERN_FILE))
            val fileHelper = FilesHelper(applicationContext)
            val file = fileHelper.getCacheFile(filename) ?: return
            val fileStream = fileHelper.getCacheStream(file)

            doAsync {
                val success = ReportExcelHelpers(applicationContext).createReportPayments(paymentsList!!, fileStream)

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
        hideProgress()
        runOnUiThread {
            Toast.makeText(this, R.string.error_report_license_check, Toast.LENGTH_LONG).show()
        }
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
        private val SEND_FILE_CODE = 102
    }
}
