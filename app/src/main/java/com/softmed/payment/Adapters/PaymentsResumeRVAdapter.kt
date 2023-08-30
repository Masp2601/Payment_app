package com.softmed.payment.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.softmed.payment.R
import com.softmed.payment.storage.TransactionContract
import kotlinx.android.synthetic.main.layout_resume_payments_list.view.*

class PaymentsResumeRVAdapter : RecyclerView.Adapter<PaymentsResumeRVAdapter.ResumeHolder>() {

    private var mTransactions: MutableList<TransactionContract.Transaction> = mutableListOf()

    init {
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: ResumeHolder, position: Int) =
            holder.bind(mTransactions[position])

    override fun getItemCount(): Int = mTransactions.size

    override fun getItemId(position: Int): Long = mTransactions[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResumeHolder =
            ResumeHolder(parent.inflate(R.layout.layout_resume_payments_list))

    fun update(transaction: List<TransactionContract.Transaction>?) {
        mTransactions.clear()

        if(transaction != null) mTransactions.addAll(transaction)

        notifyDataSetChanged()
    }

    inner class ResumeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(transaction: TransactionContract.Transaction) {
            with(itemView) {
                billInvoiceNumberValue.text = String.format("%010d", transaction.invoiceNumber)
                billInvoiceTotalValue.setText(transaction.invoiceTotal)

                when(transaction.paymentType) {

                    TransactionContract.PaymentMethods.Cash.ordinal -> {
                        paymentMethodValue.text = context.getString(R.string.payment_method_cash)
                    }

                    TransactionContract.PaymentMethods.Card.ordinal -> {
                        paymentMethodValue.text = context.getString(R.string.payment_method_card)
                        billCardReferenceText.visibility = View.VISIBLE
                        billCardReferenceValue.visibility = View.VISIBLE
                        billCardReferenceValue.text = transaction.paymentCardReference
                    }

                    TransactionContract.PaymentMethods.Check.ordinal -> {
                        paymentMethodValue.text = context.getString(R.string.payment_method_check)
                        billCheckNumberText.visibility = View.VISIBLE
                        billCheckBankNameText.visibility = View.VISIBLE
                        billCheckNumberValue.visibility = View.VISIBLE
                        billCheckNumberValue.text = transaction.paymentCheckNumber
                        billCheckBankNameValue.visibility = View.VISIBLE
                        billCheckBankNameValue.text = transaction.paymentCheckBankName
                    }

                    TransactionContract.PaymentMethods.Credit.ordinal -> {
                        paymentMethodValue.text = context.getString(R.string.payment_method_credit)
                        billCreditFirstDepositText.visibility = View.VISIBLE
                        billCreditFirstDepositValue.visibility = View.VISIBLE
                        billCreditFirstDepositValue.setText(transaction.paymentCreditDeposit)
                    }
                }
            }
        }
    }
}