package com.softmed.payment.adapters

import android.app.AlertDialog
import android.content.Intent
import android.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.softmed.payment.ClientDebtsDetailsActivity
import com.softmed.payment.ClientDebtsDetailsActivity.Companion.CLIENT_DEBTS_PARCELABLE
import com.softmed.payment.R
import com.softmed.payment.storage.InvoiceContract
import com.softmed.payment.storage.InvoiceDbHelper
import kotlinx.android.synthetic.main.layout_client_debts_item.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync

class ClientDebtsAdapter(private val clientDebts: List<InvoiceContract.ClientDebts>,
                         private val onItemsActions: OnItemsActions):
        RecyclerView.Adapter<ClientDebtsAdapter.ClientHolder>(), AnkoLogger {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientHolder {
        return ClientHolder(parent.inflate(R.layout.layout_client_debts_item))
    }

    override fun getItemCount(): Int = clientDebts.size

    override fun onBindViewHolder(holder: ClientHolder, position: Int) {
        holder.bind(clientDebts[position])
    }

    inner class ClientHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: InvoiceContract.ClientDebts) {
            with(itemView) {
                clientName.text = item.name
                debtsTotalInvoice.text = item.totalInvoice.toString()
                debtsTotalPaid.setText(item.paid)
                debtsTotalInDebts.setText(item.debts)
                debtToolbar.inflateMenu(R.menu.menu_debt_client)
                debtToolbar.setOnMenuItemClickListener { menuItem ->
                    when(menuItem.itemId) {
                        R.id.action_debt_invoices -> {
                            val intent = Intent(context, ClientDebtsDetailsActivity::class.java)
                            intent.putExtra(CLIENT_DEBTS_PARCELABLE, item)
                            context.startActivity(intent)

                            return@setOnMenuItemClickListener true
                        }
                        R.id.action_debt_pay_all -> {
                            val builder = AlertDialog.Builder(context)
                            val message = context.getString(R.string.debts_pay_all_message, item.name)
                            builder.setMessage(message)

                            builder.setPositiveButton(R.string.alert_dialog_button_accept, {
                                _, _ ->
                                doAsync {
                                    val control = PreferenceManager
                                            .getDefaultSharedPreferences(context.applicationContext)
                                            .getLong(context.getString(R.string.pref_value_control_day_time), 0L)
                                    val success = InvoiceDbHelper.getInstance(context.applicationContext)
                                            .payAllDebtsForClient(item.name, control)
                                    if (success == true) onItemsActions.onPayAllSuccess()
                                }
                            })

                            val dialog = builder.create()
                            dialog.show()

                            return@setOnMenuItemClickListener true
                        }
                        else -> return@setOnMenuItemClickListener true
                    }
                }
            }
        }
    }

    interface OnItemsActions {
        fun onPayAllSuccess()
    }
}

fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}
