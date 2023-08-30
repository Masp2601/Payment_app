package com.softmed.payment.adapters

import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.telephony.PhoneNumberUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import com.softmed.payment.R
import com.softmed.payment.storage.ClientesContract
import kotlinx.android.synthetic.main.layout_clientes_list.view.*
import org.jetbrains.anko.AnkoLogger
import java.util.*

class ClientsAdapterRV(private val mListener: OnMenuActionListener):
        RecyclerView.Adapter<ClientsAdapterRV.ClientHolder>(), Filterable, AnkoLogger {

    private var clients: MutableList<ClientesContract.Cliente> = mutableListOf()
    private var filteredClients: MutableList<ClientesContract.Cliente> = mutableListOf()

    init {
        setHasStableIds(true)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(chars: CharSequence): FilterResults {
                filteredClients.clear()
                if (chars.isEmpty()) {
                    filteredClients.addAll(clients)
                }
                else {
                    val filtered = clients.filter {
                        it.name.contains(chars, true)
                        || it.lastname.contains(chars, true) }

                    filteredClients.addAll(filtered)
                }

                val results = FilterResults()
                results.count = filteredClients.size
                results.values = filteredClients

                return results
            }

            override fun publishResults(p0: CharSequence, p1: FilterResults) {
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientHolder {
        return ClientHolder(parent.inflate(R.layout.layout_clientes_list))
    }

    override fun onBindViewHolder(holder: ClientHolder, position: Int) {
        holder.bind(filteredClients[position])
    }

    override fun getItemCount(): Int = filteredClients.size

    override fun getItemId(position: Int): Long {
        return filteredClients[position].id
    }

    fun updateClients(clients: List<ClientesContract.Cliente>) {
        this.clients.clear()
        this.clients.addAll(clients)
        filteredClients.clear()
        filteredClients.addAll(clients)
        notifyDataSetChanged()

    }

    inner class ClientHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            with(itemView) {
                toolbar.inflateMenu(R.menu.menu_clientes)
            }
        }
        fun bind(item: ClientesContract.Cliente) {
            with(itemView) {
                val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales.get(0)
                } else {
                    context.resources.configuration.locale
                }

                clienteName.text = context.getString(R.string.cliente_fullname, item.name, item.lastname)
                clientePhoneValue.text = PhoneNumberUtils.formatNumber(item.phoneNumber, locale.country)
                clienteNitValue.text = item.nit

                setToolbarOptions(item.id)
            }
        }

        private fun setToolbarOptions(id: Long) {
            with(itemView) {
                toolbar.setOnMenuItemClickListener { item ->
                    when(item.itemId) {
                        R.id.clienteActionEdit -> {
                            mListener.onEditSelected(id)
                            return@setOnMenuItemClickListener true
                        }
                        R.id.clienteActionDelete -> {
                            mListener.onDeleteSelected(id)
                            return@setOnMenuItemClickListener true
                        }
                        R.id.clienteActionDetails -> {
                            mListener.onDetailsSelected(id)
                            return@setOnMenuItemClickListener true
                        }
                        else -> return@setOnMenuItemClickListener true
                    }
                }
            }
        }
    }

    interface OnMenuActionListener {
        fun onEditSelected(id: Long)
        fun onDeleteSelected(id: Long)
        fun onDetailsSelected(id: Long)
    }
}