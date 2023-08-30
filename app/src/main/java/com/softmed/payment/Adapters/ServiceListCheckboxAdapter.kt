package com.softmed.payment.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.softmed.payment.R
import com.softmed.payment.storage.ServiciosContract

class ServiceListCheckboxAdapter(
        val ctx: Activity,
        val layoutId: Int,
        val services: List<ServiciosContract.Service>,
        private val selectedList: ArrayList<ServiciosContract.Service> = arrayListOf()): BaseAdapter(), Filterable {

    private var filteredList: List<ServiciosContract.Service> = services
    private var filter: ServiceFilter? = null

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = ServiceFilter()
        }
        return filter!!
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView

        if (convertView == null){
            val inflater = ctx.layoutInflater
            view = inflater.inflate(layoutId, null)
        }

        val checkbox = view?.findViewById<CheckBox>(R.id.chkServiceName)
        checkbox?.text = filteredList[position].name
        checkbox?.isClickable = false
        checkbox?.isLongClickable = false
        checkbox?.isChecked = selectedList.indexOf(filteredList[position]) > -1

        checkbox?.setOnClickListener { v ->
            if (checkbox.isChecked){
                selectedList.add(filteredList[position])
            } else {
                selectedList.remove(filteredList[position])
            }
        }

        val price = view?.findViewById<TextView>(R.id.servicePrice)
        price?.text = filteredList[position].price.toString()

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

    fun getSelectedList(): ArrayList<ServiciosContract.Service> {
        return selectedList
    }

    inner class ServiceFilter: Filter(){
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

        override fun publishResults(p0: CharSequence?, filtered: FilterResults?) {
            filteredList = filtered?.values as List<ServiciosContract.Service>
            notifyDataSetChanged()
        }

    }
}