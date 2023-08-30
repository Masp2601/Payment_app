package com.softmed.payment.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.softmed.payment.R
import com.softmed.payment.storage.InvoiceContract
import kotlinx.android.synthetic.main.layout_bills_item_list.view.*

class BillsListRVAdapter : RecyclerView.Adapter<BillsListRVAdapter.BillsHolder>() {

    private var mInvoice: MutableList<InvoiceContract.Invoice> = mutableListOf()

    init {
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: BillsHolder, position: Int) =
            holder.bind(mInvoice[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillsHolder =
            BillsHolder(parent.inflate(R.layout.layout_bills_item_list))

    override fun getItemCount(): Int = mInvoice.size

    override fun getItemId(position: Int): Long = mInvoice[position].id

    fun update(invoices: List<InvoiceContract.Invoice>?) {
        mInvoice.clear()

        if (invoices != null) mInvoice.addAll(invoices)

        notifyDataSetChanged()
    }

    inner class BillsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(invoice: InvoiceContract.Invoice) {

            with(itemView) {
                billInvoiceNumberValue.text = String.format("%010d", invoice.invoiceNumber)
                billClientNameValue.text = invoice.clientName
                billInvoiceTotalValue.setText(invoice.total)
                billInvoiceDateValue.text = invoice.date

                billNullifyText.visibility = if (invoice.nullify == 0) View.GONE else View.VISIBLE
            }
        }
    }
}