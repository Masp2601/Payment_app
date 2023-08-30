package com.softmed.payment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.SalesItemsAdapterRV
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.helpers.DividerItemDecoration
import com.softmed.payment.helpers.FilesHelper
import com.softmed.payment.storage.*
import kotlinx.android.synthetic.main.activity_new_sale_resume.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.defaultSharedPreferences
import java.io.File
import java.util.*

class AddInvoiceItemsActivity : BaseActivity(),
        ServiceEditFragment.ServiceEditDialogListener,
        ShowBillFragment.OnFragmentInteractionListener,
        PaymentDialogFragment.OnPaymentListener,
        SalesItemsAdapterRV.OnItemClickListener{

    override fun onItemClick(service: ServiciosContract.Service) {
        indexItemSelected = selectedItems?.indexOf(service)
        val dialog = ServiceEditFragment.newInstance(service.name, service.price, service.discount, service.amount)
        dialog.show(supportFragmentManager, "Modificar")
    }

    override fun onSendFileIntent(intent: Intent?, file: File) {
        fileToSend = file
        startActivityForResult(intent, SEND_FILE_CODE)
    }

    override fun paymentAccepted(paymentDialogData: TransactionContract.PaymentDialogData) {
        val controlDate = getControlDate()
        payment = TransactionContract.Transaction(
                id = 0,
                invoiceNumber = paymentDialogData.invoiceNumber,
                invoiceTotal = paymentDialogData.invoiceTotal,
                paymentType = paymentDialogData.paymentMethod.ordinal,
                paymentTotal = paymentDialogData.paymentTotal,
                paymentCardReference = paymentDialogData.cardReference,
                paymentCreditDeposit = paymentDialogData.paymentTotal,
                paymentDate = DateTimeHelper.dateToString(controlDate),
                paymentDateTime = Date().time,
                paymentControlTime = controlDate.time,
                paymentCheckNumber = paymentDialogData.checkNumber,
                paymentCheckBankName = paymentDialogData.checkBankName
        )

        if (savePayment()){
            enableButtons()
            showPaymentMethod(paymentDialogData.paymentMethod)
            showPaymentInformation(paymentDialogData.paymentMethod, paymentDialogData)
        } else {
            payment = null
        }
    }

    override fun onDialogPositiveClick(price: Double, discount: Double, amount: Double) {
        if (indexItemSelected == null) return
        val copy = selectedItems!![indexItemSelected!!].copy(price = price, discount = discount, amount = amount)
        selectedItems!![indexItemSelected!!] = copy

        refreshList()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        indexItemSelected = null
    }

    override fun onBackPressed() {
        if (payment != null) {
            goToMain()
        } else {
            super.onBackPressed()
        }
    }

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mAdapter: SalesItemsAdapterRV
    private lateinit var mLayoutManager: LinearLayoutManager

    private var selectedItems: ArrayList<ServiciosContract.Service>? = null
    private var indexItemSelected: Int? = null
    private var clientSelected: ClientesContract.Cliente? = null
    private var payment: TransactionContract.Transaction? = null
    private var fileToSend: File? = null

    private var subtotal: Double = 0.0
    private var iva: Double = 0.0
    private var total: Double = 0.0
    private var invoiceNumber: Long? = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_sale_resume)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "New Invoice Payment Activity", null)

        selectedItems = intent.getParcelableArrayListExtra<ServiciosContract.Service>(NewSaleActivity.ItemsSelectedExtra)
        invoiceNumber = getInvoiceNumberToUse()

        if (savedInstanceState != null){
            val client = savedInstanceState.getParcelable<ClientesContract.Cliente?>(INTENT_CLIENT)
            if (client != null) setClientSelected(client)

            val pay = savedInstanceState.getParcelable<TransactionContract.Transaction?>(INTENT_PAYMENT)
            if (pay != null) {
                payment = pay
                enableButtons()

                val method = TransactionContract.PaymentMethods.values()[pay.paymentType]
                showPaymentMethod(method)
                showPaymentInformation(method, pay.invoiceTotal, pay.paymentTotal,
                        pay.paymentCardReference, pay.paymentCheckNumber, pay.paymentCheckBankName,
                        pay.paymentCreditDeposit)
            }
        }

        mAdapter = SalesItemsAdapterRV(this)
        mLayoutManager = LinearLayoutManager(this)
        rvNewSaleItems.layoutManager = mLayoutManager
        rvNewSaleItems.adapter = mAdapter
        rvNewSaleItems.addItemDecoration(DividerItemDecoration(rvNewSaleItems.context))

        refreshList()

        btnSelectClient.setOnClickListener {
            val intent = Intent(this, SelectClientActivity::class.java)
            startActivityForResult(intent, ACTIVITY_CODE)
        }

        bnvNewSaleOptions.setOnNavigationItemSelectedListener {
            item: MenuItem ->
            when(item.itemId) {
                R.id.action_save_sale -> {
                    goToMain()
                }
                R.id.action_show_ticket -> {
                    val showChooseLanguageDialog = defaultSharedPreferences.getBoolean(getString(R.string.pref_value_ticket_language_enabled), false)

                    if (showChooseLanguageDialog) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(getString(R.string.title_choose_supported_language))
                                .setItems(R.array.pref_supported_language_labels) { dialog, which ->
                                    val language = when(which) {
                                        0 -> "en"
                                        1 -> "es"
                                        else -> "en"
                                    }

                                    val fragment = ShowBillFragment.newInstance(clientSelected, selectedItems!!,
                                            subtotal, iva, total, invoiceNumber!!, payment!!, language)
                                    dialog.dismiss()
                                    fragment.show(supportFragmentManager, "ShowBill")
                                }
                        val dialog = builder.create()
                        dialog.show()
                    } else {
                        val fragment = ShowBillFragment.newInstance(clientSelected, selectedItems!!,
                                subtotal, iva, total, invoiceNumber!!, payment!!)
                        fragment.show(supportFragmentManager, "ShowBill")
                    }
                }
                R.id.action_add_payment -> {

                    val paymentData = TransactionContract.PaymentDialogData(
                            invoiceId = 0,
                            invoiceNumber = invoiceNumber ?: 0,
                            invoiceTotal = total
                    )

                    val paymentMethods = ArrayList<String>(3)
                    paymentMethods.add(0, getString(R.string.payment_method_cash))
                    paymentMethods.add(1, getString(R.string.payment_method_card))
                    paymentMethods.add(2, getString(R.string.payment_method_check))
                    paymentMethods.add(3, getString(R.string.payment_method_credit))

                    val dialog = PaymentDialogFragment.newInstance(paymentData, paymentMethods)
                    dialog.show(supportFragmentManager, "PaymentDialog")
                }
                else -> {
                    return@setOnNavigationItemSelectedListener true
                }
            }
            return@setOnNavigationItemSelectedListener true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTIVITY_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null){
                    val client = data.getParcelableExtra<ClientesContract.Cliente>(INTENT_CLIENT)
                    if (client != null) {
                        setClientSelected(client)
                    }
                }
            }
        }

        if (requestCode == SEND_FILE_CODE){
            FilesHelper.deleteFileWithDelay(fileToSend)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(INTENT_CLIENT, clientSelected)
        outState.putParcelable(INTENT_PAYMENT, payment)
        super.onSaveInstanceState(outState)
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun savePayment() : Boolean {
        val date = Date()
        if (invoiceNumber == null) return false

        if (isValidInvoiceNumber(invoiceNumber!!)) {
            if (payment?.paymentType == TransactionContract.PaymentMethods.Cash.ordinal &&
                    payment != null && payment!!.paymentTotal < payment!!.invoiceTotal) {
                Toast.makeText(this, getString(R.string.new_invoice_pay_under_total_error), Toast.LENGTH_LONG).show()
                return false
            }

            if(payment?.paymentType == TransactionContract.PaymentMethods.Credit.ordinal &&
                    clientSelected == null) {
                Toast.makeText(this, getString(R.string.new_invoice_credit_not_client_selected_error), Toast.LENGTH_LONG).show()
                return false
            }

            insertInvoice(invoiceNumber!!, date)
            insertInvoiceItems(invoiceNumber!!)
            insertTransaction(payment!!)

            val totalToAdd = when(payment?.paymentType) {
                null -> 0.0
                TransactionContract.PaymentMethods.Credit.ordinal -> payment?.paymentCreditDeposit ?: 0.0
                else -> total
            }

            val creditsToAdd = when(payment?.paymentType) {
                TransactionContract.PaymentMethods.Credit.ordinal -> payment?.invoiceTotal ?: 0.0
                else -> 0.0
            }

            setLastInvoiceNumber(invoiceNumber!!)
            addToTotalInvoiced(totalToAdd)
            addToTotalBillsToday()
            addToTotalInCreditToday(creditsToAdd)
            return true
        } else {
            Toast.makeText(this, getString(R.string.new_invoice_invoice_number_error), Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun refreshList() {
        mAdapter.update(selectedItems!!.toList())
        calculateTotals()
    }

    private fun calculateTotals() {
        if (selectedItems == null) return
        val total: Double
        var subtotal = 0.0
        var iva = 0.0

        for (items: ServiciosContract.Service in selectedItems!!) {
            val sub = items.price * (1 - (items.discount / 100)) * items.amount
            subtotal += sub
            iva += sub * items.iva / 100
        }
        total = subtotal + iva

        resumenSubtotalValue.text = subtotal.toString()
        resumenIvaValue.text = iva.toString()
        resumenTotalValue.text = total.toString()

        this.subtotal = subtotal
        this.iva = iva
        this.total = total
    }

    private fun enableButtons() {
        bnvNewSaleOptions.menu.findItem(R.id.action_save_sale).isEnabled = true
        bnvNewSaleOptions.menu.findItem(R.id.action_show_ticket).isEnabled = true
    }

    private fun insertInvoice(invoiceNumber: Long, date: Date) {
        val controlTime = getControlDate()
        val isCredit = if (payment?.paymentType == TransactionContract.PaymentMethods.Credit.ordinal) 1 else 0
        val invoice = InvoiceContract.Invoice(
                id = 0,
                invoiceNumber = invoiceNumber,
                clientName = getString(R.string.cliente_fullname, clientSelected?.name ?: "", clientSelected?.lastname ?: ""),
                clientID = clientSelected?.id ?: 0,
                subTotal = subtotal,
                iva = iva,
                total = total,
                date = DateTimeHelper.dateToString(controlTime),
                timeMS = date.time,
                controlTimeMS = controlTime.time,
                isCredit = isCredit,
                totalPaid = payment?.paymentCreditDeposit ?: 0.0
        )
        InvoiceDbHelper.getInstance(applicationContext).insert(invoice)
    }

    private fun insertInvoiceItems(invoiceNumber: Long) {
        if (selectedItems == null) return

        val items = InvoiceItemsContract.convertServicesToItems(selectedItems!!.toList(), invoiceNumber)

        InvoiceItemsDbHelper.getInstance(applicationContext).insert(items)
    }

    private fun insertTransaction(transaction: TransactionContract.Transaction) {
        // Guardamos el pago total como el valor total de factura
        // Hay que modificarlo para los reportes.
        val toRecord =
                if (transaction.paymentType == TransactionContract.PaymentMethods.Cash.ordinal)
                    transaction.copy(paymentTotal = transaction.invoiceTotal)
        else
                    transaction
        TransactionDbHelper.getInstance(applicationContext).insert(toRecord)
    }

    private fun setClientSelected(client: ClientesContract.Cliente) {
        resumenClienteName.text = getString(R.string.cliente_fullname, client.name, client.lastname)

        clientSelected = client
    }

    private fun showPaymentMethod(method: TransactionContract.PaymentMethods) {
        resumenPayMethodText.visibility = View.VISIBLE
        resumenPayMethodValue.visibility = View.VISIBLE
        resumenPayMethodValue.text = when (method) {
            TransactionContract.PaymentMethods.Cash -> getString(R.string.payment_method_cash)
            TransactionContract.PaymentMethods.Card -> getString(R.string.payment_method_card)
            TransactionContract.PaymentMethods.Credit -> getString(R.string.payment_method_credit)
            TransactionContract.PaymentMethods.Check -> getString(R.string.payment_method_check)
        }
    }

    private fun setCashVisibility(visibility: Int) {
        resumenPayCashText.visibility = visibility
        resumenPayCashValue.visibility = visibility
        resumenChange.visibility = visibility
        resumenChangeValue.visibility = visibility
    }

    private fun setCardReferenceVisibility(visibility: Int) {
        resumenCardReferenceText.visibility = visibility
        resumenCardReferenceValue.visibility = visibility
    }

    private fun setBankVisibility(visibility: Int) {
        resumenCheckNumberText.visibility = visibility
        resumenCheckNumberValue.visibility = visibility
        resumenCheckBankNameText.visibility = visibility
        resumenCheckBankNameValue.visibility = visibility
    }

    private fun setCreditDepositVisibility(visibility: Int) {
        resumenCreditDepositText.visibility = visibility
        resumenCreditDepositValue.visibility = visibility
    }

    private fun showPaymentInformation(method: TransactionContract.PaymentMethods, total: Double,
                                       cash: Double?, reference: String?,
                                       checkNumber: String?, checkBankName: String?,
                                       initialDeposit: Double?) {
        when(method) {
            TransactionContract.PaymentMethods.Cash -> {
                val pay = cash ?: total
                setCardReferenceVisibility(View.GONE)
                setBankVisibility(View.GONE)
                setCreditDepositVisibility(View.GONE)
                setCashVisibility(View.VISIBLE)

                resumenPayCashValue.text = pay.toString()
                resumenChangeValue.text = (pay - total).toString()
            }
            TransactionContract.PaymentMethods.Card -> {
                setBankVisibility(View.GONE)
                setCashVisibility(View.GONE)
                setCreditDepositVisibility(View.GONE)
                setCardReferenceVisibility(View.VISIBLE)

                resumenCardReferenceValue.text = reference
            }
            TransactionContract.PaymentMethods.Credit -> {
                setBankVisibility(View.GONE)
                setCashVisibility(View.GONE)
                setCardReferenceVisibility(View.GONE)
                setCreditDepositVisibility(View.VISIBLE)

                resumenCreditDepositValue.text = initialDeposit.toString()
            }
            TransactionContract.PaymentMethods.Check -> {
                setCashVisibility(View.GONE)
                setCardReferenceVisibility(View.GONE)
                setCreditDepositVisibility(View.GONE)
                setBankVisibility(View.VISIBLE)

                resumenCheckNumberValue.text = checkNumber
                resumenCheckBankNameValue.text = checkBankName
            }
        }
    }

    private fun showPaymentInformation(method: TransactionContract.PaymentMethods, paymentDialogData: TransactionContract.PaymentDialogData) {
        when(method) {
            TransactionContract.PaymentMethods.Cash -> {
                val pay = paymentDialogData.paymentTotal
                setCardReferenceVisibility(View.GONE)
                setBankVisibility(View.GONE)
                setCreditDepositVisibility(View.GONE)
                setCashVisibility(View.VISIBLE)

                resumenPayCashValue.text = pay.toString()
                resumenChangeValue.text = (pay - total).toString()
            }
            TransactionContract.PaymentMethods.Card -> {
                setBankVisibility(View.GONE)
                setCashVisibility(View.GONE)
                setCreditDepositVisibility(View.GONE)
                setCardReferenceVisibility(View.VISIBLE)

                resumenCardReferenceValue.text = paymentDialogData.cardReference
            }
            TransactionContract.PaymentMethods.Credit -> {
                setBankVisibility(View.GONE)
                setCashVisibility(View.GONE)
                setCardReferenceVisibility(View.GONE)
                setCreditDepositVisibility(View.VISIBLE)

                resumenCreditDepositValue.text = "${paymentDialogData.paymentTotal}"
            }
            TransactionContract.PaymentMethods.Check -> {
                setCashVisibility(View.GONE)
                setCardReferenceVisibility(View.GONE)
                setCreditDepositVisibility(View.GONE)
                setBankVisibility(View.VISIBLE)

                resumenCheckNumberValue.text = paymentDialogData.checkNumber
                resumenCheckBankNameValue.text = paymentDialogData.checkBankName
            }
        }
    }

    companion object {
        const val ACTIVITY_CODE = 100
        const val SEND_FILE_CODE = 112
        const val INTENT_CLIENT = "Cliente"
        const val INTENT_PAYMENT = "Payment"
    }
}
