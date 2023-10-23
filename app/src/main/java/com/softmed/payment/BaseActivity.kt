package com.softmed.payment

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.storage.InvoiceContract
import com.softmed.payment.storage.InvoiceDbHelper
import com.softmed.payment.storage.TransactionDbHelper
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import java.util.*

abstract class BaseActivity : AppCompatActivity(), AnkoLogger {
    var menu: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_home -> {
                if (this !is MainActivity){
                    menu?.close()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                return true
            }
            R.id.action_clientes -> {
                if (this !is ClientesActivity) {
                    menu?.close()
                    val intent = Intent(this, ClientesActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                return true
            }
            R.id.action_image -> {
                if (this !is ClientesActivity) {
                    menu?.close()
                    val intent = Intent(this, Image_logo::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                return true
            }
            R.id.action_services -> {
                if (this !is ServicesActivity) {
                    val intent = Intent(this, ServicesActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                return true
            }
            R.id.action_new_invoice -> {
                if (this !is NewSaleActivity) {
                    if (!isDayOpen()) {
                        showClosedDayNotification()
                        return true
                    }
                    if (!isValidDayToOpen()) {
                        showNotValidDayToOpen()
                        return true
                    }

                    if (hasInvoiceToAdd()) {
                        val intent = Intent(this, NewSaleActivity::class.java)
                        startActivity(intent)
                    } else {
                        showNoInvoiceNumberToAddMessage()
                    }
                }
                return true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                return true
            }
            R.id.action_bills -> {
                if (this !is BillsActivity) {
                    val intent = Intent(this, BillsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }

                return true
            }
            R.id.action_report -> {
                if (this !is ReportActivity){
                    val intent = Intent(this, ReportActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }

                return true
            }
            R.id.action_license -> {
                val intent = Intent(this, LicenseActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)

                return true
            }
            R.id.action_client_debts -> {
                val intent = Intent(this, ClientDebtsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)

                return true
            }
            R.id.action_whats_new -> {
                val alert = AlertDialog.Builder(this)
                val view = this.layoutInflater.inflate(R.layout.layout_whats_new, null)

                alert.setPositiveButton("Ok", {
                    d, _ ->
                    d.dismiss()
                })

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
                        Toast.makeText(this@BaseActivity,
                                "Error obteniendo las novedades. Verifique su conexión a internet",
                                Toast.LENGTH_LONG).show()
                    }
                }

                webView.loadUrl("http://softmedsas.com/invoiceapp/novedades.html")

                alert.setView(view)
                alert.show()

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun addToTotalInvoiced(add: Double) {
        val manager = preferenceManager()
        val saved = manager.getFloat(getString(R.string.pref_value_total_day), 0F)
        val editor = manager.edit()
        editor.putFloat(getString(R.string.pref_value_total_day), (saved + add).toFloat())
        editor.apply()
    }

    fun addToTotalBillsToday(add: Int = 1) {
        val manager = preferenceManager()
        val total = manager.getInt(getString(R.string.pref_value_total_bills_day), 0)
        val editor = manager.edit()
        editor.putInt(getString(R.string.pref_value_total_bills_day), total + add)
        editor.apply()
    }

    fun addToTotalInCreditToday(add: Double) {
        val manager = preferenceManager()
        val saved = manager.getFloat(getString(R.string.pref_value_total_in_credit_day), 0F)
        val editor = manager.edit()
        editor.putFloat(getString(R.string.pref_value_total_day), (saved + add).toFloat())
        editor.apply()
    }

    fun addPurchaseToTotalPaidToday(add: Double) {
        val manager = preferenceManager()
        val saved = manager.getFloat(getString(R.string.pref_value_purchase_total_paid_day), 0F)
        val editor = manager.edit()
        editor.putFloat(getString(R.string.pref_value_purchase_total_paid_day), (saved + add).toFloat())
        editor.apply()
    }

    fun checkForWritePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val arrayOfPermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, arrayOfPermission, PERMISSION_CODE)
        }
    }

    fun getAppLastVersion(): String? = getDefaultPreference()
            .getString(getString(R.string.pref_value_app_last_version), "")

    fun getControlDate(): Date {
        val preferences = getDefaultPreference()
        val time = preferences.getLong(getString(R.string.pref_value_control_day_time), 0L)

        return Date(time)
    }

    fun getInvoiceNumberToUse(): Long {
        val number = getLastInvoiceNumber()

        if (number == 0L) {
            val manager = getDefaultPreference()
            val from = manager.getString(getString(R.string.pref_key_bills_from), "1")!!.toLong()
            return if(from == 0L) 1 else from
        }

        return number + 1
    }

    private fun getLastInvoiceNumber(): Long {
        val preferences: SharedPreferences = preferenceManager()

        return preferences.getLong(getString(R.string.pref_key_invoice_number), 0)
    }

    fun getTotalBillsToday(): Int {
        return preferenceManager().getInt(getString(R.string.pref_value_total_bills_day), 0)
    }

    fun getTotalInvoicedToday(): Float {
        return preferenceManager().getFloat(getString(R.string.pref_value_total_day), 0F)
    }

    fun getTotalInCreditToday(): Float {
        return preferenceManager().getFloat(getString(R.string.pref_value_total_in_credit_day), 0F)
    }

    fun getTotalInCreditDepositToday(): Float {
        return preferenceManager().getFloat(getString(R.string.pref_value_total_in_credit_deposit_day), 0F)
    }

    fun getPurchaseTotalPaidToday(): Float {
        return preferenceManager().getFloat(getString(R.string.pref_value_purchase_total_paid_day), 0F)
    }

    fun hasInvoiceToAdd(): Boolean {
        val manager = getDefaultPreference()
        val minNumber = manager.getString(getString(R.string.pref_key_bills_from), "0")!!.toLong()
        val maxNumber = manager.getString(getString(R.string.pref_key_bills_to), "0")!!.toLong()
        val nextNumber = getLastInvoiceNumber()

        if (minNumber == 0L && maxNumber == 0L) return true
        if (minNumber != 0L && maxNumber == 0L) return false
        if (nextNumber > maxNumber) return false

        return true
    }

    fun isAppRegistered(): Boolean {
        val preferences = preferenceManager()
        return preferences.getBoolean(getString(R.string.pref_value_is_application_registered), false)
    }

    fun isApplicationExpired(): Boolean {
        val preference = getDefaultPreference()
        val expirationTime = preference.getLong(getString(R.string.pref_value_license_expiration_date), 0)
        val date = Date().time

        return date > expirationTime
    }

    fun isApplicationLimited(): Boolean {
        val preference = preferenceManager()
        return preference.getBoolean(getString(R.string.pref_value_application_is_limited), true)
    }

    fun isDayOpen(): Boolean {
        val preference = getDefaultPreference()
        return preference.getBoolean(getString(R.string.pref_value_is_day_open), false)
    }

    fun isValidInvoiceNumber(number: Long): Boolean {
        val manager = getDefaultPreference()
        val minNumber = manager.getString(getString(R.string.pref_key_bills_from), "0")!!.toLong()
        val maxNumber = manager.getString(getString(R.string.pref_key_bills_to), "0")!!.toLong()

        if (minNumber == 0L && maxNumber == 0L) {
            return true
        }

        return number <= maxNumber
    }

    fun setLastInvoiceNumber(number: Long) {
        val preferences: SharedPreferences = preferenceManager()
        val editor = preferences.edit()
        editor.putLong(getString(R.string.pref_key_invoice_number), number)
        editor.apply()
    }

    fun showNoInvoiceNumberToAddMessage() {
        Toast.makeText(this,getString(R.string.error_invoice_number_maxed), Toast.LENGTH_LONG).show()
    }

    fun showClosedDayNotification() {
        Toast.makeText(this,getString(R.string.error_day_is_closed_message), Toast.LENGTH_LONG).show()
    }

    fun showNotValidDayToOpen() {
        Toast.makeText(this, getString(R.string.error_day_to_open_is_invalid), Toast.LENGTH_LONG).show()
    }

    fun updateLastVersion(lastVersion: String) {
        val editor = getDefaultPreference().edit()
        editor.putString(getString(R.string.pref_value_app_last_version), lastVersion)
        editor.apply()
    }

    private fun getDefaultPreference() =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)

    fun openDay() {
        if (!isValidDayToOpen()) {
            showNotValidDayToOpen()
            return
        }
        val preference = getDefaultPreference()
        val calendar = Calendar.getInstance()
        val date = DateTimeHelper.getStarOfDay(calendar)

        val editor = preference.edit()
        editor.putBoolean(getString(R.string.pref_value_is_day_open), true)
        editor.putLong(getString(R.string.pref_value_control_day_time), date.timeInMillis)
        editor.apply()

        initializeDay(date.time)
    }

    fun closeDay() {
        val preference = getDefaultPreference()
        val editor = preference.edit()
        editor.putBoolean(getString(R.string.pref_value_is_day_open), false)
        //editor.putLong(getString(R.string.pref_value_control_day_time), 0L)
        editor.apply()
    }

    fun validateApplication() {
        if (!isAppRegistered()) {
            goToLoginActivity()
            finish()
        }

        val preference = preferenceManager()
        with(preference.edit()) {
            putBoolean(
                    getString(R.string.pref_value_application_is_limited),
                    isApplicationExpired()
            )
            apply()
        }
    }

    fun goToLoginActivity() {
        val preference = preferenceManager()

        with(preference.edit()) {
            putBoolean(getString(R.string.pref_value_is_application_registered), false)
            putBoolean(getString(R.string.pref_value_application_is_limited), true)
            apply()
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    fun isValidDayToOpen(): Boolean {
        val controlDay = getControlDate()
        return Date() > controlDay
    }

    fun onNullifyInvoice(invoice: InvoiceContract.Invoice, nullify: Int) {
        val date = getControlDate()
        if (DateTimeHelper.dateToString(date) == invoice.date) {
            val value = if (nullify == 0) invoice.total else invoice.total * -1
            addToTotalInvoiced(value)
            if (nullify == 0) addToTotalBillsToday(1) else addToTotalBillsToday(-1)
        }
    }

    fun updateTodayData(
            callback: (
                    totalDay: Float,
                    totalBills: Int,
                    totalInCredit: Float,
                    creditDepositTotalDay: Float
            ) -> Unit
    ) {
        val controlDate = getControlDate()
        val preferences = preferenceManager()
        val editor = preferences.edit()

        doAsync {
            val db = InvoiceDbHelper.getInstance(applicationContext)
            val invoices = db.getAllAtDay(DateTimeHelper.dateToString(controlDate))

            val transactionDb = TransactionDbHelper.getInstance(applicationContext)
            val totalDay = transactionDb.getPaymentTotalDay(controlDate.time)
            val creditDepositTotalDay = transactionDb.getCreditDepositTotalDay(controlDate.time)

            var billSize = 0
            var totalInCredit = 0.0

            if (invoices != null) {
                billSize = invoices.size
                // Se resta lo que se ha pagado para calcular el crédito
                totalInCredit = invoices.sumByDouble { if (it.isCredit == 1) it.total - it.totalPaid else 0.0 }
            }

            editor.putFloat(getString(R.string.pref_value_total_day), totalDay.toFloat())
            editor.putFloat(getString(R.string.pref_value_total_in_credit_deposit_day), creditDepositTotalDay.toFloat())
            editor.putInt(getString(R.string.pref_value_total_bills_day), billSize)
            editor.putFloat(getString(R.string.pref_value_total_in_credit_day), totalInCredit.toFloat())

            editor.apply()
            callback(totalDay.toFloat(), billSize, totalInCredit.toFloat(), creditDepositTotalDay.toFloat())
        }
    }

    fun getBirthdayOfTheDay(): JSONArray {
        val birthdayList = preferenceManager().getString(getString(R.string.pref_value_birthday_of_the_day), "[]")
        return JSONArray(birthdayList)
    }

    fun getCurrentLanguage(): String {
        return when(Locale.getDefault().language) {
            "es" -> "es"
            else -> "en"
        }
    }

    private fun initializeDay(date: Date) {
        val preferences = preferenceManager()
        val editor = preferences.edit()
        editor.putString(getString(R.string.pref_value_control_date), DateTimeHelper.dateToString(date))
        editor.putFloat(getString(R.string.pref_value_purchase_total_paid_day), 0F)
        editor.putFloat(getString(R.string.pref_value_total_day), 0F)
        editor.putInt(getString(R.string.pref_value_total_bills_day), 0)
        editor.apply()
    }

    private fun preferenceManager() = getSharedPreferences(SHARED_PREFERENCE_FILE, 0)

    companion object {
        const val DATABASE_NAME = "PaymentDB"
        const val DATABASE_VERSION = 10
        const val SHARED_PREFERENCE_FILE = "sp_file"
        const val PERMISSION_CODE = 100
        const val LICENSE_URL = "http://licenses.softmedsas.com"
        const val BIRTHDAY_JSON_NAME = "name"
        const val BIRTHDAY_JSON_AGE = "age"
        const val BIRTHDAY_JSON_MONTH_DAY = "monthDay"
        lateinit var instance: BaseActivity
    }
}
