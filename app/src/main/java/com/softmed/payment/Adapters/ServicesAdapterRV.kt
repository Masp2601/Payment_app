package com.softmed.payment.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.PopupMenu
import com.softmed.payment.R
import com.softmed.payment.storage.ServiciosContract
import kotlinx.android.synthetic.main.layout_services_list.view.*
import org.jetbrains.anko.AnkoLogger

class ServicesAdapterRV(private val mListener: ServicesAdapterRV.OnMenuItemClick): RecyclerView.Adapter<ServicesAdapterRV.ServiceHolder>(),
        Filterable, AnkoLogger {

    private val services: MutableList<ServiciosContract.Service> = mutableListOf()
    private var filteredList: List<ServiciosContract.Service> = listOf()

    init {
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: ServiceHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceHolder {
        return ServiceHolder(parent.inflate(R.layout.layout_services_list))
    }

    override fun getItemId(position: Int): Long = filteredList[position].id

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(chars: CharSequence): FilterResults {
                filteredList = if (chars.isEmpty()) {
                    services.toList()
                } else {
                    services.filter { it.name == chars }
                }

                val result = FilterResults()
                result.count = filteredList.size
                result.values = filteredList

                return result
            }

            override fun publishResults(p0: CharSequence, p1: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }

    fun update(services: List<ServiciosContract.Service>?) {
        this.services.clear()

        if (services != null) {
            this.services.addAll(services)
        }

        filteredList = this.services.toList()

        notifyDataSetChanged()
    }

    inner class ServiceHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        fun bind(service: ServiciosContract.Service) {
            with(itemView) {
                serviceName.text = service.name
                servicePrice.setText(service.price)
                serviceIva.text = "${service.iva}"
                serviceDiscount.text = "${service.discount}"

                popupMenu.setOnClickListener {
                    btnView ->
                    val popup = PopupMenu(this.context, btnView)
                    popup.inflate(R.menu.menu_services)
                    popup.show()
                    popup.setOnMenuItemClickListener {
                        menuItem ->
                        when(menuItem.itemId) {
                            R.id.serviceActionEdit -> {
                                mListener.onEditSelected(service.id)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.serviceActionDelete -> {
                                mListener.onDeleteSelected(service.id)
                                return@setOnMenuItemClickListener true
                            }
                            else -> return@setOnMenuItemClickListener true
                        }
                    }
                }
            }
        }
    }

    interface OnMenuItemClick {
        fun onEditSelected(id: Long)
        fun onDeleteSelected(id: Long)
    }
}