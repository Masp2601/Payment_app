package com.softmed.payment.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.softmed.payment.storage.ServiciosContract
import com.softmed.payment.R

class ServicesListAdapter(val activity: Activity, val services: List<ServiciosContract.Service>): BaseAdapter(), Filterable {
    var filteredList = services
    var filter: ServiceFilter? = null

    init {
        getFilter()
    }
    override fun getFilter(): Filter {
        if (filter == null) {
            filter = ServiceFilter()
        }

        return filter!!
    }

    override fun getView(index: Int, convertView: View?, viewGroup: ViewGroup?): View {
        var view = convertView
        if (view == null){
            val inflater = activity.layoutInflater
            view = inflater.inflate(R.layout.layout_servicios_item, null)
        }

        val name = view?.findViewById<TextView>(R.id.serviceName)
        name?.text = filteredList[index].name

        val price = view?.findViewById<TextView>(R.id.servicePrice)
        price?.text = "${filteredList[index].price}"

        val iva = view?.findViewById<TextView>(R.id.serviceIva)
        iva?.text = "${filteredList[index].iva}"

        val discount = view?.findViewById<TextView>(R.id.serviceDiscount)
        discount?.text = "${filteredList[index].discount}"

        val amount = view?.findViewById<TextView>(R.id.serviceAmount)
        amount?.text = "${filteredList[index].amount}"

        return view!!
    }

    override fun getItem(position: Int): Any {
        return filteredList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return filteredList.size
    }

    inner class ServiceFilter: Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResult = FilterResults()

            if (constraint != null && constraint.isNotEmpty()) {
                val filtered = services.filter { it.name.contains(constraint, true) }
                filterResult.count = filtered.size
                filterResult.values = filtered
            } else {
                filterResult.count = services.size
                filterResult.values = services
            }

            return filterResult
        }

        override fun publishResults(constraint: CharSequence?, filtered: FilterResults?) {
            filteredList = filtered?.values as List<ServiciosContract.Service>
            notifyDataSetChanged()
        }

    }
}