package com.softmed.payment.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.softmed.payment.helpers.CurrencyTextView
import com.softmed.payment.storage.TransactionContract
import org.jetbrains.anko.layoutInflater
import com.softmed.payment.R

class PaymentsResumeListAdapter(private val ctx: Context, private val payments: List<TransactionContract.Transaction>): BaseAdapter() {
    override fun getView(index: Int, contextView: View?, viewGroup: ViewGroup?): View {
        var view = contextView
        if (view == null){
            view = ctx.layoutInflater.inflate(R.layout.layout_resume_payments_list, null)
        }

        view!!.findViewById<TextView>(R.id.billInvoiceNumberValue).text = String.format("%010d", payments[index].invoiceNumber)
        view.findViewById<TextView>(R.id.billInvoiceTotalValue).text = payments[index].paymentTotal.toString()

        when (payments[index].paymentType) {
            TransactionContract.PaymentMethods.Cash.ordinal -> view.findViewById<TextView>(R.id.paymentMethodValue).text = ctx.getString(R.string.payment_method_cash)
            TransactionContract.PaymentMethods.Card.ordinal -> {
                view.findViewById<TextView>(R.id.paymentMethodValue).text = ctx.getString(R.string.payment_method_card)
                view.findViewById<TextView>(R.id.billCardReferenceText).visibility = View.VISIBLE
                val billCardReferenceValue = view.findViewById<TextView>(R.id.billCardReferenceValue)
                billCardReferenceValue.visibility = View.VISIBLE
                billCardReferenceValue.text = payments[index].paymentCardReference
            }
            TransactionContract.PaymentMethods.Check.ordinal -> {
                view.findViewById<TextView>(R.id.paymentMethodValue).text = ctx.getString(R.string.payment_method_check)
                view.findViewById<TextView>(R.id.billCheckNumberText).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.billCheckBankNameText).visibility = View.VISIBLE

                val checkNumber = view.findViewById<TextView>(R.id.billCheckNumberValue)
                checkNumber.visibility = View.VISIBLE
                checkNumber.text = payments[index].paymentCheckNumber

                val checkBank = view.findViewById<TextView>(R.id.billCheckBankNameValue)
                checkBank.visibility = View.VISIBLE
                checkBank.text = payments[index].paymentCheckBankName
            }
            TransactionContract.PaymentMethods.Credit.ordinal -> {
                view.findViewById<TextView>(R.id.paymentMethodValue).text = ctx.getString(R.string.payment_method_credit)
                view.findViewById<TextView>(R.id.billInvoiceTotalValue).text = payments[index].invoiceTotal.toString()
                view.findViewById<CurrencyTextView>(R.id.billCreditFirstDepositValue).setText(payments[index].paymentCreditDeposit)

                view.findViewById<TextView>(R.id.billCreditFirstDepositText).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.billCreditFirstDepositValue).visibility = View.VISIBLE
            }
        }

        return view
    }

    override fun getItem(index: Int): TransactionContract.Transaction {
        return payments[index]
    }

    override fun getItemId(index: Int): Long {
        return payments[index].id
    }

    override fun getCount(): Int {
        return payments.size
    }
}