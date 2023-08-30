package com.softmed.payment.adapters

import androidx.recyclerview.widget.RecyclerView
import android.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import com.softmed.payment.ClientDebtsDetailsActivity
import com.softmed.payment.R
import com.softmed.payment.storage.InvoiceContract
import kotlinx.android.synthetic.main.layout_client_debts_details_items.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


class ClientDebtsDetailsAdapter(private var items: List<InvoiceContract.InvoiceDebtDetail>,
                                private val onPaymentDialog: ClientDebtsDetailsActivity.OnInvoiceDebtPaymentDialog):
        RecyclerView.Adapter<ClientDebtsDetailsAdapter.DetailViewHolder>(), AnkoLogger{

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(parent.inflate(R.layout.layout_client_debts_details_items))
    }

    fun updateInvoices(invoices: List<InvoiceContract.InvoiceDebtDetail>) {
        items = invoices
    }

    inner class DetailViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        init {
            val toolbar = itemView.findViewById<Toolbar>(R.id.toolbar)
            toolbar.inflateMenu(R.menu.menu_client_debts_details)
        }

        fun bind(item: InvoiceContract.InvoiceDebtDetail) {
            with(itemView) {
                debtsDetailInvoiceNumber.text = String.format("%05d", item.invoiceNumber)
                debtsDetailInvoiceDate.text = item.date
                debtsDetailTotalInvoice.setText(item.total)
                debtsDetailTotalPaid.setText(item.totalPaid)
                debtsDetailTotalInCredit.setText(item.total - item.totalPaid)
                debtsDetailTotalPayment.setText(item.payment)

                toolbar.setOnMenuItemClickListener {
                    menuItem ->
                    val inCredit = item.total - item.totalPaid
                    when(menuItem.itemId) {
                        R.id.action_debt_pay -> {
                            onPaymentDialog.show(item.invoiceNumber, inCredit, item.payment)

                            return@setOnMenuItemClickListener true
                        }
                        R.id.action_debt_pay_all -> {
                            onPaymentDialog.payInvoice(item.invoiceNumber, inCredit)
                            return@setOnMenuItemClickListener true
                        }
                        R.id.action_debt_show_invoice_detail -> {
                            onPaymentDialog.showInvoiceDetail(item.invoiceNumber)
                            return@setOnMenuItemClickListener true
                        }
                        else -> {
                            info("else ${menuItem.itemId}")
                            return@setOnMenuItemClickListener true
                        }
                    }
                }
            }
        }
    }
}