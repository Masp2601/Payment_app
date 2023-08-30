package com.softmed.payment.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.softmed.payment.helpers.CurrencyTextView
import com.softmed.payment.R
import com.softmed.payment.storage.InvoiceContract


class BillsListAdapter(private val activity: Activity, private val bills: List<InvoiceContract.Invoice>): BaseAdapter() {
    override fun getView(position: Int, contentView: View?, viewGroup: ViewGroup?): View {
        var view = contentView
        if (view == null){
            view = activity.layoutInflater.inflate(R.layout.layout_bills_item_list, null)
        }

        val invoiceNumber = view?.findViewById<TextView>(R.id.billInvoiceNumberValue)
        invoiceNumber?.text = String.format("%010d", bills[position].invoiceNumber)

        val clientName = view?.findViewById<TextView>(R.id.billClientNameValue)
        clientName?.text = bills[position].clientName

        val total = view?.findViewById<CurrencyTextView>(R.id.billInvoiceTotalValue)
        total?.text = bills[position].total.toString()

        val date = view?.findViewById<TextView>(R.id.billInvoiceDateValue)
        date?.text = bills[position].date

        if (bills[position].nullify == 0) {
            view?.findViewById<TextView>(R.id.billNullifyText)?.visibility = View.GONE
        }

        return view!!
    }

    override fun getItem(position: Int): InvoiceContract.Invoice {
        return bills[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return bills.size
    }
}