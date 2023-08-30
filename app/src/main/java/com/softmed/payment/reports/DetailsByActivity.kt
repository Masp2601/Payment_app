package com.softmed.payment.reports

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.softmed.payment.storage.*
import kotlinx.android.synthetic.main.activity_details_by.*
import org.jetbrains.anko.*
import java.io.File
import java.util.*


class DetailsByActivity : BaseActivity() {

    private lateinit var listOfFilter: List<String>
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mAdapter: BillsListRVAdapter
    private lateinit var mLayoutManager: LinearLayoutManager

    private var clients: List<ClientesContract.Cliente>? = null
    private var services: List<ServiciosContract.Service>? = null

    private var filterBy: Int? = null
    private var clientSelected: ClientesContract.Cliente? = null
    private var serviceSelected: ServiciosContract.Service? = null

    private var calendarSince: Calendar = Calendar.getInstance()
    private var calendarEnd: Calendar = Calendar.getInstance()

    private var invoices: List<InvoiceContract.Invoice>? = null
    var fileToSend: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_by)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        if (savedInstanceState != null) {
            filterBy = savedInstanceState.getInt(FILTER_BY)
            clientSelected = savedInstanceState.getParcelable(CLIENT_SELECTED)
            serviceSelected = savedInstanceState.getParcelable(SERVICE_SELECTED)
            calendarSince.timeInMillis = savedInstanceState.getLong(DATE_SINCE)
            calendarEnd.timeInMillis = savedInstanceState.getLong(DATE_END)

            showResume()
        }

        listOfFilter = resources.getStringArray(R.array.report_filter_by_items).toList()
        mAdapter = BillsListRVAdapter()
        mLayoutManager = LinearLayoutManager(this)
        rvBills.layoutManager = mLayoutManager
        rvBills.adapter = mAdapter
        rvBills.addItemDecoration(DividerItemDecoration(this))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOfFilter)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterBySpinner.adapter = adapter
        filterBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                when(position) {
                    0 -> {
                        serviceSelected = null
                        filterBy = 0
                        if (clients == null) {
                            val mutable = mutableListOf<ClientesContract.Cliente>()
                            mutable.add(0, ClientesContract.Cliente(0, getString(R.string.report_client_unregister)))
                            val ref = ClientesDbHelper.getInstance(applicationContext).getAll()
                            if (ref != null) mutable.addAll(ref)
                            clients = mutable
                        }
                        val clAdapter = ArrayAdapter(
                                this@DetailsByActivity,
                                android.R.layout.simple_spinner_item,
                                clients?.map { "${it.name} ${it.lastname}" } as MutableList<String>
                        )
                        clAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        selectedFilterText.text = listOfFilter[0]
                        selectedFilterSpinner.adapter = clAdapter

                        if (clientSelected != null) {
                            selectedFilterSpinner.setSelection(clients!!.indexOf(clientSelected!!))
                        }

                        selectedFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(p0: AdapterView<*>?) {
                                return
                            }

                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                clientSelected = clients?.get(position)
                            }

                        }
                    }
                    1 -> {
                        clientSelected = null
                        filterBy = 1
                        if (services == null) {
                            services = ServiciosDbHelper.getInstance(applicationContext).getAll()
                        }
                        val sAdapater = ArrayAdapter(
                                this@DetailsByActivity,
                                android.R.layout.simple_spinner_item,
                                services?.map { it.name } as MutableList<String>
                        )
                        sAdapater.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        selectedFilterText.text = listOfFilter[1]
                        selectedFilterSpinner.adapter = sAdapater

                        if (serviceSelected != null) selectedFilterSpinner.setSelection(services!!.indexOf(serviceSelected!!))

                        selectedFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(p0: AdapterView<*>?) {
                                return
                            }

                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                serviceSelected = services?.get(position)
                            }

                        }
                    }
                    else -> return
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                return
            }

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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            FilesHelper.deleteFileWithDelay(fileToSend)
        } catch (e: Exception) {
            error(e.message)
        }
    }

    private fun showResume() {
        progressBar.visibility = View.VISIBLE

        if (isApplicationLimited()) {
            fabSendEmail.hide()
        } else {
            fabSendEmail.show()
        }

        val end = DateTimeHelper.getEndDate(calendarEnd)

        when (filterBy) {
            0 -> {
                if (clientSelected != null) {
                    doAsync {
                        val resume = InvoiceDbHelper.getInstance(applicationContext).filterByClientIdAndDate(clientSelected!!.id, calendarSince.timeInMillis, end.timeInMillis)
                        setResumen(resume)
                    }
                }
            }
            1 -> {
                if (serviceSelected != null) {
                    doAsync {
                        val numbers = InvoiceItemsDbHelper.getInstance(applicationContext).getInvoiceNumbersByItem(serviceSelected!!.name)
                        if (numbers != null) {
                            val resume = InvoiceDbHelper.getInstance(applicationContext).getInvoicesByNumbers(numbers, calendarSince.timeInMillis, end.timeInMillis)
                            setResumen(resume)
                        } else {
                            setResumen(null)
                        }
                    }
                }
            }
        }
    }

    private fun setResumen(resume: List<InvoiceContract.Invoice>?) {
        runOnUiThread {
            val totalPaid = resume?.sumByDouble {
                if (it.nullify == 0)
                    it.total
                else
                    0.0
            } ?: 0.0
            val totalSales = resume?.size ?: 0
            reportTotalOfTicket.text = "$totalSales"
            reportTotalPaid.setText(totalPaid)
            mAdapter.update(resume)
            rvBills.scheduleLayoutAnimation()

            progressBar.visibility = View.GONE
        }
        invoices = resume
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

    private fun checkLicense(isValid: Boolean) {
        if (isValid) {
            if (invoices == null) return
            val filename = getString(R.string.file_name_report_filter_by, listOfFilter[filterBy!!], DateTimeHelper.dateToString(calendarSince.time), DateTimeHelper.dateToString(calendarEnd.time))
            val fileHelper = FilesHelper(applicationContext)
            val file = fileHelper.getCacheFile(filename) ?: return
            val outputStream = fileHelper.getCacheStream(file) ?: return

            doAsync {
                val success = ReportExcelHelpers(applicationContext).reportInvoices(invoices!!, outputStream)

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
        outState?.putInt(FILTER_BY, filterBy ?: 0)
        outState?.putParcelable(CLIENT_SELECTED, clientSelected)
        outState?.putParcelable(SERVICE_SELECTED, serviceSelected)
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
        private val CLIENT_SELECTED = "client_selected"
        private val SERVICE_SELECTED = "service_selected"
        private val FILTER_BY = "filter_by"
        private val SEND_FILE_CODE = 102
    }
}
