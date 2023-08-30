package com.softmed.payment.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.softmed.payment.R
import com.softmed.payment.storage.InvoiceItemsContract

/**
 * Created by nelso on 9/22/2017.
 */
class InvoiceItemsListAdapter(val activity: Activity, private val items: List<InvoiceItemsContract.InvoiceItems>): BaseAdapter() {
    override fun getView(index: Int, convertView: View?, viewGroup: ViewGroup?): View {
        var view = convertView
        if (view == null){
            val inflater = activity.layoutInflater
            view = inflater.inflate(R.layout.layout_servicios_item, null)
        }

        val name = view?.findViewById<TextView>(R.id.serviceName)
        name?.text = items[index].itemName

        val price = view?.findViewById<TextView>(R.id.servicePrice)
        price?.text = "${items[index].itemPrice}"

        val iva = view?.findViewById<TextView>(R.id.serviceIva)
        iva?.text = "${items[index].itemIva}"

        val discount = view?.findViewById<TextView>(R.id.serviceDiscount)
        discount?.text = "${items[index].itemDiscount}"

        val amount = view?.findViewById<TextView>(R.id.serviceAmount)
        amount?.text = "${items[index].itemAmount}"

        return view!!
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return items.size
    }
}