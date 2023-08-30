package com.softmed.payment.adapters

import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.softmed.payment.R
import com.softmed.payment.storage.InvoiceItemsContract

class ItemsSoldListAdapter(private val activity: AppCompatActivity, private val items: List<InvoiceItemsContract.InvoiceItemsTotalResumen>) : BaseAdapter() {
    override fun getView(index: Int, contextView: View?, viewGroup: ViewGroup?): View {
        val view = contextView ?: activity.layoutInflater.inflate(R.layout.layout_resumen_items_sold, null)

        val nameText = view.findViewById<TextView>(R.id.itemName)
        nameText?.text = items[index].itemName

        val amountText = view.findViewById<TextView>(R.id.itemAmount)
        amountText?.text = items[index].itemAmountTotal.toString()

        val totalText = view.findViewById<TextView>(R.id.itemTotalPaid)
        totalText?.text = items[index].itemPriceTotal.toString()

        return view
    }

    override fun getItem(index: Int): InvoiceItemsContract.InvoiceItemsTotalResumen = items[index]

    override fun getItemId(index: Int): Long = index.toLong()

    override fun getCount(): Int = items.size
}