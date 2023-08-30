package com.softmed.payment

import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import com.firebase.jobdispatcher.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.ClientBirthdayListAdapter
import com.softmed.payment.helpers.DateTimeHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*

class MainActivity : BaseActivity(), AnkoLogger {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        validateApplication()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "Main", null)

        btnNewSale.setOnClickListener { _ ->
            if (!isDayOpen()) {
                showOpenDayMessage()
                return@setOnClickListener
            }
            if (!isValidDayToOpen()) {
                showNotValidDayToOpen()
                return@setOnClickListener
            }

            if (hasInvoiceToAdd()) {
                goToAddBill()
            } else {
                showNoInvoiceNumberToAddMessage()
            }
        }

        btnOpenOrCloseDay.setOnClickListener {
            if (isDayOpen()) {
                val alert = AlertDialog.Builder(this)
                alert.setTitle(R.string.main_operation_on_close_day_alert_title)
                        .setMessage(R.string.main_operation_on_close_day_alert_message)
                        .setPositiveButton(R.string.alert_dialog_button_accept) { _,_ ->
                            closeDay()
                            updateDashboard()
                        }
                        .setNegativeButton(R.string.alert_dialog_button_cancel, null)

                alert.create()
                alert.show()
            } else {
                openDay()
                updateDashboard()
            }
        }

        //val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(applicationContext))

        /*val newJob = dispatcher.newJobBuilder()
                .setService(BirthdayService::class.java)
                .setTag(BirthdayService.BIRTHDAY_TAG)
                .setRecurring(false)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setReplaceCurrent(false)
                .setTrigger(Trigger.executionWindow(0, 60))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .build()

        dispatcher.schedule(newJob)*/
    }

    override fun onResume() {
        validateApplication()
        updateDashboard()
        updateBirthdays()
        checkNewVersion()
        super.onResume()
    }

    private fun checkNewVersion() {
        if (getAppLastVersion() != BuildConfig.VERSION_NAME) {
            updateLastVersion(BuildConfig.VERSION_NAME)

            val alert = AlertDialog.Builder(this)
            val view = this.layoutInflater.inflate(R.layout.layout_whats_new, null)

            alert.setPositiveButton("Ok"){
                d, _ ->
                d.dismiss()
            }

            val webView = view.findViewById<WebView>(R.id.whatsNewWV)
            webView.webChromeClient = object: WebChromeClient() {
                override fun onProgressChanged(webView: WebView?, newProgress: Int) {
                    view.findViewById<ProgressBar>(R.id.progressBar).progress = newProgress

                    if (newProgress == 100) {
                        view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                    }
                }
            }
            webView.webViewClient = object : WebViewClient() {
                override fun onReceivedError(webView: WebView?, request: WebResourceRequest?, error: WebResourceError) {
                    Toast.makeText(this@MainActivity,
                            getString(R.string.error_no_internet_connection),
                            Toast.LENGTH_LONG).show()
                }
            }

            val language = getCurrentLanguage()

            webView.loadUrl("http://softmedsas.com/invoiceapp/novedades.$language.html")

            alert.setView(view)
            alert.show()
        }
    }

    private fun updateDashboard() {
        val controlDate = getControlDate()
        val dateToShow = if (controlDate.time == 0L) Date() else controlDate

        openDateValue.text = DateTimeHelper.dateToString(dateToShow, "MMMM dd, yyyy")
        totalBillsToday.text = "${getTotalBillsToday()}"
        totalInvoicedDay.setText(getTotalInvoicedToday())
        totalInCreditDay.setText(getTotalInCreditToday())
        totalInCreditDepositDay.setText(getTotalInCreditDepositToday())

        if (isDayOpen()) {
            val icon = ContextCompat.getDrawable(this, R.drawable.ic_lock_open_black_24dp)
            icon!!.setColorFilter(ContextCompat.getColor(this, R.color.colorGreen500), PorterDuff.Mode.SRC_IN)
            openDateValue.setTextColor(ContextCompat.getColor(this, R.color.colorGreen500))
            openDateValue.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            operationStatusValue.text = getString(R.string.main_operation_status_open)
            operationStatusValue.setBackgroundResource(R.color.colorGreen500)
            btnOpenOrCloseDay.text = getString(R.string.title_button_close_day)
            updateTodayData {
                total, bills, totalInCredit, totalInCreditDeposit ->
                updateTotalsDay(total, bills, totalInCredit, totalInCreditDeposit)
            }
        } else {
            val icon = ContextCompat.getDrawable(this, R.drawable.ic_lock_outline_black_24dp)
            icon!!.setColorFilter( ContextCompat.getColor(this, R.color.colorRed500), PorterDuff.Mode.SRC_IN)
            openDateValue.setTextColor(ContextCompat.getColor(this, R.color.colorRed500))
            openDateValue.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            operationStatusValue.text = getString(R.string.main_operation_status_close)
            operationStatusValue.setBackgroundResource(R.color.colorRed500)
            btnOpenOrCloseDay.text = getString(R.string.title_button_open_day)
        }
    }

    private fun updateTotalsDay(
            totalDay: Float,
            totalBills: Int,
            totalInCredit: Float = 0F,
            totalInCreditDeposit: Float = 0F
    ) {
        runOnUiThread {
            totalInvoicedDay.text = totalDay.toString()
            totalBillsToday.text = totalBills.toString()
            totalInCreditDay.setText(totalInCredit)
            totalInCreditDepositDay.setText(totalInCreditDeposit)
        }
    }

    private fun goToAddBill() {
        val intent = Intent(this, NewSaleActivity::class.java)
        startActivity(intent)
    }

    private fun updateBirthdays() {
        val birthdays = getBirthdayOfTheDay()

        val adapter = ClientBirthdayListAdapter(this, birthdays)
        clientsBirthdayList.adapter = adapter
    }

    private fun showOpenDayMessage() {
        val alert = AlertDialog.Builder(this)
        alert.setTitle(R.string.main_button_closed_day_title_alert)
                .setMessage(R.string.main_button_closed_day_new_sale_alert)
                .setPositiveButton(R.string.alert_dialog_button_accept) { _,_ ->
                    openDay()

                    if (hasInvoiceToAdd()) {
                        goToAddBill()
                    } else {
                        showNoInvoiceNumberToAddMessage()
                    }
                }
                .setNegativeButton(R.string.alert_dialog_button_cancel) {
                    _, _ ->
                    showClosedDayNotification()
                }

        alert.create()
        alert.show()
    }
}
