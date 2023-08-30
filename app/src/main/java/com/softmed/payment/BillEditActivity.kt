package com.softmed.payment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.InvoiceItemsListAdapter
import com.softmed.payment.storage.InvoiceContract
import com.softmed.payment.storage.InvoiceItemsDbHelper
import com.softmed.payment.storage.TransactionContract
import com.softmed.payment.storage.TransactionDbHelper
import kotlinx.android.synthetic.main.activity_bill_edit.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class BillEditActivity : AppCompatActivity() {
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill_edit)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val intent = intent
        val bill = intent.getParcelableExtra<InvoiceContract.Invoice>(BillsActivity.BILL_PARCELABLE)
        if (bill != null) {
            setDataOnView(bill)
        }
    }

    private fun setDataOnView(invoice: InvoiceContract.Invoice) {
        resumenInvoiceNumberValue.text = String.format("%010d", invoice.invoiceNumber)
        invoiceDate.text = invoice.date
        resumenClienteName.text = invoice.clientName
        resumenTotalValue.text = invoice.total.toString()
        resumenSubtotalValue.text = invoice.subTotal.toString()
        resumenIvaValue.text = invoice.iva.toString()

        setListItem(invoice.invoiceNumber)
        setPaymentData(invoice.invoiceNumber)
    }

    private fun setListItem(invoiceNumber: Long){
        doAsync {
            val list = InvoiceItemsDbHelper.getInstance(applicationContext).get(invoiceNumber)

            uiThread {
                if (list != null) {
                    listServicesSelected.adapter = InvoiceItemsListAdapter(this@BillEditActivity, list)
                }
            }
        }
    }

    private fun setPaymentData(invoiceNumber: Long) {
        doAsync {
            val payment = TransactionDbHelper.getInstance(applicationContext).getTransaction(invoiceNumber)

            if (payment != null) {
                when(payment.paymentType) {
                    TransactionContract.PaymentMethods.Cash.ordinal -> {
                        resumenPayMethod.visibility = View.VISIBLE
                        resumenPayMethodValue.visibility = View.VISIBLE
                        resumenPayMethodValue.text = getString(R.string.payment_method_cash)
                        resumenPayCash.visibility = View.VISIBLE
                        resumenPayCashValue.visibility = View.VISIBLE
                        resumenPayCashValue.text = payment.paymentTotal.toString()
                        resumenChange.visibility = View.VISIBLE
                        resumenChangeValue.visibility = View.VISIBLE
                        resumenChangeValue.text = (payment.paymentTotal - payment.invoiceTotal).toString()

                        resumenCardReference.visibility = View.GONE
                        resumenCardReferenceValue.visibility = View.GONE
                    }
                    TransactionContract.PaymentMethods.Card.ordinal -> {
                        resumenPayMethod.visibility = View.VISIBLE
                        resumenPayMethodValue.visibility = View.VISIBLE
                        resumenPayMethodValue.text = getString(R.string.payment_method_card)

                        resumenPayCash.visibility = View.GONE
                        resumenPayCashValue.visibility = View.GONE
                        resumenChange.visibility = View.GONE
                        resumenChangeValue.visibility = View.GONE

                        resumenCardReference.visibility = View.VISIBLE
                        resumenCardReferenceValue.visibility = View.VISIBLE
                        resumenCardReferenceValue.text = payment.paymentCardReference
                    }
                    TransactionContract.PaymentMethods.Credit.ordinal -> {
                        resumenPayMethod.visibility = View.VISIBLE
                        resumenPayMethodValue.visibility = View.VISIBLE
                        resumenPayMethodValue.text = getString(R.string.payment_method_credit)

                        resumenPayCash.visibility = View.GONE
                        resumenPayCashValue.visibility = View.GONE
                        resumenChange.visibility = View.GONE
                        resumenChangeValue.visibility = View.GONE
                        resumenCardReference.visibility = View.GONE
                        resumenCardReferenceValue.visibility = View.GONE
                    }
                }
            }
        }
    }
}
