package com.softmed.payment.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.softmed.payment.R
import com.softmed.payment.storage.ClientesContract

class ClientListSelectAdapter(private val activity: Activity, private val clients: List<ClientesContract.Cliente>): BaseAdapter(), Filterable {
    var filteredList = clients
    var filter: ClientsFilter? = null

    init {
        getFilter()
    }

    override fun getFilter(): Filter {
        if (filter == null){
            filter = ClientsFilter()
        }

        return filter!!
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val view: View = convertView ?: activity.layoutInflater.inflate(R.layout.layout_list_client_select, null)

        view.findViewById<TextView>(R.id.clientName).text = "${filteredList[position].name} ${filteredList[position].lastname}"
        view.findViewById<TextView>(R.id.clientNit).text = filteredList[position].nit

        return view
    }

    override fun getItem(position: Int): ClientesContract.Cliente {
        return filteredList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return filteredList.size
    }

    fun getClientSelected(position: Int): ClientesContract.Cliente {
        return filteredList[position]
    }

    inner class ClientsFilter: Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResult = FilterResults()

            if (constraint != null && constraint.isNotEmpty()) {
                val filtered = clients.filter {
                    it.name.contains(constraint, true)
                            || it.lastname.contains(constraint, true)
                            || it.nit.contains(constraint, true)
                }

                filterResult.count = filtered.size
                filterResult.values = filtered
            } else {
                filterResult.count = clients.size
                filterResult.values = clients
            }

            return filterResult
        }

        override fun publishResults(p0: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            filteredList = results?.values as List<ClientesContract.Cliente>
            notifyDataSetChanged()
        }

    }
}