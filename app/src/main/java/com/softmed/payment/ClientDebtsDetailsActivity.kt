package com.softmed.payment

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.softmed.payment.adapters.ClientDebtsDetailsAdapter
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.storage.InvoiceContract
import com.softmed.payment.storage.InvoiceDbHelper
import com.softmed.payment.storage.TransactionContract
import com.softmed.payment.storage.TransactionDbHelper
import kotlinx.android.synthetic.main.activity_client_debts_details.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

class ClientDebtsDetailsActivity : AppCompatActivity(),
        PaymentDialogFragment.OnPaymentListener,
        CreditPaymentDialogFragment.OnPaymentListener, AnkoLogger {

    override fun onButtonAccept(invoiceNumber: Long, payment: Double) {
        if (debtsResume != null) {
            val detail = debtsResume?.find { it.invoiceNumber == invoiceNumber }
            detail?.payment = payment

            rvAdapter.updateInvoices(debtsResume!!)
            rvAdapter.notifyDataSetChanged()

            setTotalToPay(debtsResume!!.sumByDouble { it.payment })
        }
    }

    override fun paymentAccepted(paymentDialogData: TransactionContract.PaymentDialogData) {
        paymentData = paymentDialogData
        debtsSaveButton.isEnabled = true
    }

    private lateinit var clientDebts: InvoiceContract.ClientDebts
    private lateinit var rvAdapter: ClientDebtsDetailsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var debtsResume: List<InvoiceContract.InvoiceDebtDetail>? = null
    private var totalToPay: Double = 0.0
    private var paymentData: TransactionContract.PaymentDialogData? = null

    private val dateControl by lazy {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val time = preferenceManager.getLong(getString(R.string.pref_value_control_day_time), 0L)

        Date(time)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_debts_details)

        clientDebts = intent.getParcelableExtra<InvoiceContract.ClientDebts>(CLIENT_DEBTS_PARCELABLE)!!

        linearLayoutManager =
            LinearLayoutManager(this)
        clientDebtsDetailsRV.layoutManager = linearLayoutManager

        setData()
        setAdapter()
        setButtonSaveOnClickListener()
        setPayOnClickListener()
    }

    private fun setAdapter() {
        doAsync {
            val invoices = InvoiceDbHelper.getInstance(applicationContext).getAllDebtsForClient(clientDebts.clientId)
            if (invoices != null) {
                val debtsResume: List<InvoiceContract.InvoiceDebtDetail> = invoices.map {
                    InvoiceContract.InvoiceDebtDetail(it.id, it.invoiceNumber, it.date, it.total, it.totalPaid)
                }
               rvAdapter = ClientDebtsDetailsAdapter(debtsResume, showInvoicePaymentDialog)
                this@ClientDebtsDetailsActivity.debtsResume = debtsResume
                uiThread {
                    clientDebtsDetailsRV.adapter = rvAdapter
                }
            }
        }
    }

    private fun setData() {
        clientName.text = clientDebts.name
        debtsTotalInDebts.setText(clientDebts.debts)
        debtsTotalToPay.setText(totalToPay)
    }

    private fun setButtonSaveOnClickListener() {
        debtsSaveButton.setOnClickListener {
            _ ->
            val debtsToUpdate = debtsResume?.filter { it.payment > 0 }

            if (debtsToUpdate != null && debtsToUpdate.isNotEmpty()) {
                doAsync {
                    val invoiceDbHelper = InvoiceDbHelper.getInstance(applicationContext)
                    val transactionDbHelper = TransactionDbHelper.getInstance(applicationContext)
                    val date = Date()

                    debtsToUpdate.forEach {
                        invoiceDbHelper.updateCredit(it.id, it.totalPaid + it.payment)

                        val transaction = TransactionContract.Transaction(
                                id = 0,
                                invoiceNumber = it.invoiceNumber,
                                invoiceTotal = it.total,
                                paymentType = paymentData?.paymentMethod?.ordinal ?: TransactionContract.PaymentMethods.Cash.ordinal,
                                paymentTotal = it.payment,
                                paymentCardReference = paymentData?.cardReference ?: "",
                                paymentCheckNumber = paymentData?.checkNumber ?: "",
                                paymentCheckBankName = paymentData?.checkBankName ?: "",
                                paymentDateTime = date.time,
                                paymentDate = DateTimeHelper.dateToString(date),
                                paymentControlTime = dateControl.time
                        )

                        transactionDbHelper.processCreditPayment(transaction)
                    }

                    uiThread {
                        this@ClientDebtsDetailsActivity.finish()
                    }
                }
            }
        }
    }

    private fun setPayOnClickListener() {
        debtsPaymentButton.setOnClickListener {
            _ ->
            val paymentData = TransactionContract.PaymentDialogData(
                    invoiceId = 0,
                    invoiceNumber = 0,
                    invoiceTotal = totalToPay
            )
            showPaymentDialog.showDialog(paymentData)
        }
    }

    private fun setTotalToPay(payment: Double) {
        totalToPay = payment
        debtsTotalToPay.setText(totalToPay)
        debtsPaymentButton.isEnabled = payment > 0.0
    }

    private val showInvoicePaymentDialog = object: OnInvoiceDebtPaymentDialog {
        override fun showInvoiceDetail(invoiceNumber: Long) {
            doAsync {
                val invoice = InvoiceDbHelper.getInstance(applicationContext).find(invoiceNumber)
                if (invoice != null) {
                    uiThread {
                        val intent = Intent(this@ClientDebtsDetailsActivity, BillEditActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        intent.putExtra(BillsActivity.BILL_PARCELABLE, invoice)
                        startActivity(intent)
                    }
                }
            }
        }

        override fun payInvoice(invoiceNumber: Long, payment: Double) {
            onButtonAccept(invoiceNumber, payment)
        }

        override fun show(invoiceNumber: Long, creditTotal: Double, payment: Double) {
            val paid = if (payment == 0.0) creditTotal else payment
            val dialog = CreditPaymentDialogFragment.newInstance(invoiceNumber, creditTotal, paid)
            dialog.show(supportFragmentManager, "InvoicePaymentDialog")
        }

    }

    private val showPaymentDialog = object : OnShowPaymentDialog {
        override fun showDialog(paymentData: TransactionContract.PaymentDialogData) {
            val paymentMethods = ArrayList<String>(3)
            paymentMethods.add(0, getString(R.string.payment_method_cash))
            paymentMethods.add(1, getString(R.string.payment_method_card))
            paymentMethods.add(2, getString(R.string.payment_method_check))

            val dialog = PaymentDialogFragment.newInstance(paymentData, paymentMethods)
            dialog.show(supportFragmentManager, "PaymentDialog")
        }

    }

    interface OnInvoiceDebtPaymentDialog {
        fun show(invoiceNumber: Long, creditTotal: Double, payment: Double)
        fun payInvoice(invoiceNumber: Long, payment: Double)
        fun showInvoiceDetail(invoiceNumber: Long)
    }

    interface OnShowPaymentDialog {
        fun showDialog(paymentData: TransactionContract.PaymentDialogData)
    }

    companion object {
        val CLIENT_DEBTS_PARCELABLE = "client_debts"
    }
}
