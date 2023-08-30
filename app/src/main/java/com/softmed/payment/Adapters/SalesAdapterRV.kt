package com.softmed.payment.adapters

import android.graphics.Paint
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.softmed.payment.R
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.storage.InvoiceContract
import kotlinx.android.synthetic.main.layout_sale_list_item.view.*

class SalesAdapterRV(private val mListener: OnMenuItemClickListener): RecyclerView.Adapter<SalesAdapterRV.SalesViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val invoices: MutableList<InvoiceContract.Invoice> = mutableListOf()

    override fun onBindViewHolder(holder: SalesViewHolder, position: Int) {
        holder.bind(invoices[position])
    }

    override fun getItemId(position: Int): Long = invoices[position].id

    override fun getItemCount(): Int = invoices.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesViewHolder =
            SalesViewHolder(parent.inflate(R.layout.layout_sale_list_item))

    fun update(invoices: List<InvoiceContract.Invoice>?) {
        this.invoices.clear()

        if (invoices != null) this.invoices.addAll(invoices)

        notifyDataSetChanged()
    }

    inner class SalesViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(sale: InvoiceContract.Invoice) {
            with(itemView) {
                billDateValue.text = DateTimeHelper.dateToString(sale.controlTimeMS)
                billTicketNumberValue.text = String.format("%010d", sale.invoiceNumber)
                billClientValue.text = sale.clientName
                billNullifyText.visibility = if (sale.nullify == 1) View.VISIBLE else View.GONE

                if (sale.nullify == 1) {
                    billTotalValue.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    billTotalValue.paintFlags = Paint.ANTI_ALIAS_FLAG
                }
                billTotalValue.setText(sale.total)

                popupMenu.setOnClickListener {
                    btnView ->
                    val popup = PopupMenu(this.context, btnView)
                    popup.inflate(R.menu.menu_bills)
                    popup.show()
                    popup.setOnMenuItemClickListener {
                        menuItem ->
                        when(menuItem.itemId) {
                            R.id.billsActionShowTicket -> {
                                mListener.onShowTicket(sale.id)
                            }
                            R.id.billsActionNullify -> {
                                mListener.onToggleNullify(sale.id)
                            }
                        }
                        return@setOnMenuItemClickListener true
                    }
                }
            }
        }
    }

    interface OnMenuItemClickListener {
        fun onToggleNullify(id: Long)
        fun onShowTicket(id: Long)
    }
}