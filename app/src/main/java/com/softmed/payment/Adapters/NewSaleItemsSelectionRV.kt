package com.softmed.payment.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.Visibility
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.softmed.payment.R
import com.softmed.payment.storage.ServiciosContract
import kotlinx.android.synthetic.main.layout_new_sale_item_selection.view.*

class NewSaleItemsSelectionRV(selection: MutableList<ServiciosContract.Service>) : RecyclerView.Adapter<NewSaleItemsSelectionRV.ItemsSelectionHolder>(),
        Filterable {

    private val services: MutableList<ServiciosContract.Service> = mutableListOf()
    private var filteredList: List<ServiciosContract.Service> = listOf()
    private var selectedServices: MutableList<ServiciosContract.Service> = mutableListOf()

    init {
        setHasStableIds(true)
        selectedServices = selection
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsSelectionHolder {
        return ItemsSelectionHolder(parent.inflate(R.layout.layout_new_sale_item_selection))
    }

    override fun onBindViewHolder(holder: ItemsSelectionHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    override fun getItemId(position: Int): Long = filteredList[position].id

    override fun getFilter(): Filter {
        return object: Filter() {
            override fun performFiltering(chars: CharSequence?): FilterResults {
                filteredList = if (chars == null || chars.isEmpty()) {
                    services.toList()
                } else {
                    services.filter { it.name.contains(chars, true) }
                }

                val result = FilterResults()
                result.count = filteredList.size
                result.values = filteredList

                return result
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                notifyDataSetChanged()
            }

        }
    }

    fun update(services: List<ServiciosContract.Service>?) {
        this.services.clear()

        if (services != null) {
            this.services.addAll(services)
        }

        this.filteredList = this.services.toList()
        notifyDataSetChanged()
    }

    fun getSelectedList(): ArrayList<ServiciosContract.Service>  = ArrayList(selectedServices.sortedBy { it.name })

    inner class ItemsSelectionHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(service: ServiciosContract.Service) {

            with(itemView) {
                newSaleItemName.text = service.name
                newSaleItemPriceValue.setText(service.price)
                newSaleItemAmountValue.text = "${service.amount}"

                btnNewSaleSelect.setOnClickListener { _ -> itemSelection(service.id) }
                btnNewSaleAddAmount.setOnClickListener{ _ -> showDialogAmount(service.id)}

                val idx = selectedServices.indexOfFirst { it.id == service.id }
                if ( idx > -1) {
                    setInformationVisibility(View.VISIBLE)
                    newSaleItemAmountValue.text = "${selectedServices[idx].amount}"
                }
            }
        }

        private fun itemSelection(id: Long) {
            val service = selectedServices.find { it.id == id }
            TransitionManager.beginDelayedTransition(itemView as CardView, Fade().setDuration(300))
            if (service == null) {
                setInformationVisibility(View.VISIBLE)
                selectedServices.add(services.first { it.id == id })
            } else {
                setInformationVisibility(View.GONE)
                selectedServices.remove(service)
            }
        }

        private fun setInformationVisibility(visibility: Int) {
            with(itemView) {
                imgSelected.visibility = visibility
                btnNewSaleAddAmount.visibility = visibility
                newSaleItemAmountText.visibility = visibility
                newSaleItemAmountValue.visibility = visibility
            }
        }

        @SuppressLint("WrongViewCast")
        private fun showDialogAmount(id: Long) {
            val service = selectedServices.find { it.id == id }

            if (service != null) {
                itemView as CardView
                val alertBuilder = AlertDialog.Builder(itemView.context)
                val view = itemView.inflate(R.layout.fragment_item_amount_change)

                with(view) {
                    findViewById<TextView>(R.id.fmAmountTitle).text = service.name
                    findViewById<EditText>(R.id.fmAmountValue).setText("${service.amount}")
                }

                alertBuilder.setView(view)
                alertBuilder.setPositiveButton(R.string.alert_dialog_button_accept, DialogInterface.OnClickListener {
                    _, _ ->
                    val chars = view.findViewById<EditText>(R.id.fmAmountValue).text.toString()
                    val amount = chars.toDoubleOrNull() ?: 1.0

                    if (amount != service.amount) {
                        service.amount = amount
                        filteredList.first { it.id == id }.amount = amount
                        notifyDataSetChanged()
                    }
                })

                val alert = alertBuilder.create()
                alert.show()
            }
        }
    }
}