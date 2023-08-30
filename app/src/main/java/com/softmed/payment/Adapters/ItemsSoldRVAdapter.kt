package com.softmed.payment.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.softmed.payment.R
import com.softmed.payment.storage.InvoiceItemsContract
import kotlinx.android.synthetic.main.layout_resumen_items_sold.view.*

class ItemsSoldRVAdapter() : RecyclerView.Adapter<ItemsSoldRVAdapter.ItemsHolder>() {

    private var mItems: MutableList<InvoiceItemsContract.InvoiceItemsTotalResumen> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsHolder =
            ItemsHolder(parent.inflate(R.layout.layout_resumen_items_sold))

    override fun onBindViewHolder(holder: ItemsHolder, position: Int) =
            holder.bind(mItems[position])

    override fun getItemCount(): Int  = mItems.size

    fun update(items: List<InvoiceItemsContract.InvoiceItemsTotalResumen>?) {
        mItems.clear()

        if (items != null) mItems.addAll(items)

        mItems.sortByDescending { it.itemAmountTotal }
        notifyDataSetChanged()
    }

    inner class ItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: InvoiceItemsContract.InvoiceItemsTotalResumen) {

            with(itemView) {
                itemName.text = item.itemName
                itemAmount.text = "${item.itemAmountTotal}"
                itemTotalPaid.setText(item.itemPriceTotal)
            }
        }
    }
}