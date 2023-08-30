package com.softmed.payment.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.softmed.payment.ClientesActivity
import com.softmed.payment.R
import com.softmed.payment.storage.ClientesContract

class ClientesListAdapter(val activity: ClientesActivity, val clientes: List<ClientesContract.Cliente>): BaseAdapter(), Filterable {
    var filteredList = clientes
    var filter: ClientesFilter? = null

    init {
        getFilter()
    }

    override fun getFilter(): Filter {
        if (filter == null){
            filter = ClientesFilter()
        }

        return filter!!
    }

    override fun getView(index: Int, convertView: View?, viewGroup: ViewGroup?): View {
        var view: View? = convertView
        if (view == null){
            val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.layout_clientes_list, null)
        }

        val name = view?.findViewById<TextView>(R.id.clienteName)
        name?.text = "${filteredList[index].name} ${filteredList[index].lastname}"

        val phone = view?.findViewById<TextView>(R.id.clientePhoneValue)
        phone?.text = "${filteredList[index].phoneNumber}"

        val nit = view?.findViewById<TextView>(R.id.clienteNitValue)
        nit?.text = "${filteredList[index].nit}"

        return view!!
    }

    override fun getItem(index: Int): Any {
        return filteredList[index]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return filteredList.size
    }

    inner class ClientesFilter: Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResult = FilterResults()

            if (constraint != null && constraint.isNotEmpty()) {
                val filtered = clientes.filter {
                    it.name.contains(constraint, true)
                    || it.lastname.contains(constraint, true)
                    || it.nit.contains(constraint, true)
                }

                filterResult.count = filtered.size
                filterResult.values = filtered
            } else {
                filterResult.count = clientes.size
                filterResult.values = clientes
            }

            return filterResult
        }

        override fun publishResults(p0: CharSequence?, results: FilterResults?) {
            filteredList = results?.values as List<ClientesContract.Cliente>
            notifyDataSetChanged()
        }

    }
}