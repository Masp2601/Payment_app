package com.softmed.payment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.SalesAdapterRV
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.helpers.DividerItemDecoration
import com.softmed.payment.helpers.FilesHelper
import com.softmed.payment.reports.DatePickerFragment
import com.softmed.payment.storage.*
import kotlinx.android.synthetic.main.activity_bills.*
import kotlinx.android.synthetic.main.content_sales_rv.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*

class BillsActivity : BaseActivity(),
        SalesAdapterRV.OnMenuItemClickListener,
        ShowBillFragment.OnFragmentInteractionListener {

    override fun onSendFileIntent(intent: Intent?, file: File) {
        fileToSend = file
        startActivityForResult(intent, SEND_FILE_CODE)
    }

    override fun onToggleNullify(id: Long) {
        val bill = bills?.first { it.id == id }

        if (bill != null) {
            val builder = AlertDialog.Builder(this)
            val billNumber = String.format("%010d", bill.invoiceNumber)
            val message = if (bill.nullify == 0) getString(R.string.bills_nullify_alert_message, billNumber) else getString(R.string.bills_validate_alert_message, billNumber)
            builder.setMessage(message)

            builder.setPositiveButton(R.string.alert_dialog_button_accept, { _, _ ->
                doAsync {
                    val db = InvoiceDbHelper.getInstance(applicationContext)
                    val value = if (bill.nullify == 0) 1 else 0
                    db.nullifyInvoice(bill.id, value)
                    refreshList()
                    onNullifyInvoice(bill, value)
                }
            })

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onShowTicket(id: Long) {
        val bill = bills!!.first { it.id == id }

        doAsync {
            val items = InvoiceItemsDbHelper.getInstance(applicationContext).get(bill.invoiceNumber)
            val services = items?.map {
                ServiciosContract.Service(
                        id = it.id,
                        name = it.itemName,
                        price = it.itemPrice,
                        iva = it.itemIva,
                        discount = it.itemDiscount,
                        amount = it.itemAmount
                )
            }

            val client = ClientesDbHelper.getInstance(applicationContext).get(bill.clientID)
            val payment = TransactionDbHelper.getInstance(applicationContext).getTransaction(bill.invoiceNumber)

            val fragment = ShowBillFragment.newInstance(client, ArrayList(services),
                    bill.subTotal, bill.iva, bill.total, bill.invoiceNumber, payment!!)

            uiThread {
                fragment.show(supportFragmentManager, "ShowBill")
            }
        }
    }

    private var fileToSend: File? = null
    private var bills: List<InvoiceContract.Invoice>? = null
    private var filterBy: Int = 0
    private var clientId: Long = 0
    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mAdapter: SalesAdapterRV
    private lateinit var mLinearLayoutManager: LinearLayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bills)
        setSupportActionBar(toolbar)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "Bills List", null)

        mAdapter = SalesAdapterRV(this)
        mLinearLayoutManager =
            LinearLayoutManager(this)
        rvSales.layoutManager = mLinearLayoutManager
        rvSales.adapter = mAdapter
        rvSales.addItemDecoration(DividerItemDecoration(this))

        val spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.bills_filter_array, android.R.layout.simple_spinner_item)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = spinnerAdapter

        btnSaleFilter.setOnClickListener {
            filterBy = spinnerFilter.selectedItemPosition
            when(filterBy) {
                0 -> {
                    doAsync {
                        val clients = ClientesDbHelper.getInstance(applicationContext).getAll()
                        val names = clients?.map { "${it.name} ${it.lastname}" }

                        uiThread {
                            val builder = AlertDialog.Builder(this@BillsActivity)
                            val alertView = this@BillsActivity.layoutInflater.inflate(R.layout.fragment_sale_client_filter, null)

                            val clientAdapter = ArrayAdapter(alertView.context, android.R.layout.simple_spinner_item, names as MutableList<String>)
                            clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            val spinnerClient = alertView.findViewById<Spinner>(R.id.spinnerClients)
                            spinnerClient.adapter = clientAdapter

                            builder.setView(alertView)
                            val alert = builder.create()
                            alert.show()

                            alertView.findViewById<Button>(R.id.btnFilter).setOnClickListener {
                                clientId = clients!![spinnerClient.selectedItemPosition].id
                                alert.dismiss()
                                refreshList()
                            }
                        }
                    }
                }
                1 -> {
                    val picker = DatePickerFragment.getInstance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                    picker.setOnDataSetListener(onDateSelected)
                    picker.show(fragmentManager, "DatePicker")
                }
            }
        }

        showTotal()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEND_FILE_CODE){
            FilesHelper.deleteFileWithDelay(fileToSend)
        }
    }

    private fun showTotal() {
        doAsync {
            val total = InvoiceDbHelper.getInstance(applicationContext).totalSales()
            uiThread {
                totalSalesValue.text = "$total"
            }
        }
    }

    private fun refreshList() {
        showProgressBar()
        doAsync {
            val list: List<InvoiceContract.Invoice>? = when(filterBy) {
                0 -> {
                    InvoiceDbHelper.getInstance(applicationContext).filterByClientId(clientId)
                }
                1 -> {
                    InvoiceDbHelper.getInstance(applicationContext).getAllAtDayIncludeNullify(DateTimeHelper.dateToString(calendar.time))
                }
                else -> null
            }

            uiThread {
                mAdapter.update(list)
                rvSales.scheduleLayoutAnimation()

                if(list == null || list.isEmpty()) {
                    Snackbar.make(rvSales, R.string.no_sales_text, Snackbar.LENGTH_LONG)
                            .show()
                }
            }
            bills = list
            hideProgressBar()
        }
    }

    private fun showProgressBar() {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        runOnUiThread {
            progressBar.visibility = View.GONE
        }
    }

    private val onDateSelected = object : DatePickerFragment.DatePickerListener {
        override fun onDateSet(year: Int, month: Int, day: Int) {
            val date: Date = DateTimeHelper.parseStringToDate("$year.${month + 1}.$day", DateTimeHelper.PATTERN_SHORT)
            calendar.time = date

            refreshList()
        }

    }

    companion object {
        val BILL_PARCELABLE = "bill_parcelable"
        val SEND_FILE_CODE = 112
    }
}
