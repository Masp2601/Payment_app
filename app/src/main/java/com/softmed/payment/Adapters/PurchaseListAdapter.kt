package com.softmed.payment.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.softmed.payment.R
import com.softmed.payment.storage.ExpenseContract

class PurchaseListAdapter(private val activity: Activity, private val expenses: List<ExpenseContract.Expense>): BaseAdapter() {
    override fun getView(position: Int, contentView: View?, viewGroup: ViewGroup?): View {
        val view = contentView ?: activity.layoutInflater.inflate(R.layout.layout_purchase_list, null)

        val orderNumber = view.findViewById<TextView>(R.id.purchaseOrderNumberValue)
        orderNumber?.text = String.format("%010d", expenses[position].orderNumber)

        val provider = view.findViewById<TextView>(R.id.purchaseProviderValue)
        provider?.text = expenses[position].providerName

        val date = view.findViewById<TextView>(R.id.purchaseDateValue)
        date?.text = expenses[position].date

        val total = view.findViewById<TextView>(R.id.purchaseTotalValue)
        total?.text = expenses[position].totalOrder.toString()

        return view
    }

    override fun getItem(position: Int): ExpenseContract.Expense {
        return expenses[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return expenses.size
    }
}