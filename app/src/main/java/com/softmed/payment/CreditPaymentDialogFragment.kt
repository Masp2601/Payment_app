package com.softmed.payment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.View
import com.softmed.payment.helpers.CurrencyEditText
import com.softmed.payment.helpers.CurrencyTextView
import org.jetbrains.anko.layoutInflater

class CreditPaymentDialogFragment: DialogFragment() {

    private lateinit var mListener: OnPaymentListener
    private var invoiceNumber: Long = 0
    private var creditTotal: Double = 0.0
    private var payment: Double = 0.0
    private lateinit var alertView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            invoiceNumber = arguments!!.getLong(ARG_INVOICE_NUMBER)
            payment = arguments!!.getDouble(ARG_PAYMENT)
            creditTotal = arguments!!.getDouble(ARG_CREDIT_TOTAL)
        }
    }

    override fun onResume() {
        super.onResume()
        val alert = dialog as AlertDialog
        alert.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
            _ ->
            val paymentEdit = alertView.findViewById<CurrencyEditText>(R.id.paymentCreditPaymentValue)
            val payment = paymentEdit.value.toDoubleOrNull() ?: 0.0

            if (payment > creditTotal) {
                paymentEdit.error = getString(R.string.debts_client_detail_invalid_payment_error)
                return@setOnClickListener
            }
            mListener.onButtonAccept(invoiceNumber, payment)
            alert.dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val view = context!!.layoutInflater.inflate(R.layout.fragment_payment_credit, null)

        view.findViewById<CurrencyTextView>(R.id.paymentCreditTotalValue).setText(creditTotal)
        view.findViewById<CurrencyEditText>(R.id.paymentCreditPaymentValue).setText(payment)

        builder.setView(view)
                .setPositiveButton(R.string.alert_dialog_button_accept, {
                    _, _ ->

                })

        alertView = view
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CreditPaymentDialogFragment.OnPaymentListener) {
            mListener = context
        }
        else {
            throw RuntimeException(context!!.toString() + " must implement " +
                    "PaymentDialogFragment.OnPaymentListener")
        }

    }

    interface OnPaymentListener {
        fun onButtonAccept(invoiceNumber: Long, payment: Double)
    }

    companion object {
        private val ARG_INVOICE_NUMBER = "invoice_number"
        private val ARG_PAYMENT = "payment"
        private val ARG_CREDIT_TOTAL = "credit_total"

        fun newInstance(invoiceNumber: Long,
                        creditTotal: Double,
                        payment: Double): CreditPaymentDialogFragment {
            val bundle = Bundle()
            bundle.putLong(ARG_INVOICE_NUMBER, invoiceNumber)
            bundle.putDouble(ARG_PAYMENT, payment)
            bundle.putDouble(ARG_CREDIT_TOTAL, creditTotal)

            val fragment = CreditPaymentDialogFragment()
            fragment.arguments = bundle

            return fragment
        }
    }
}