package com.softmed.payment.adapters

import android.content.res.Configuration
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.softmed.payment.R
import com.softmed.payment.storage.ServiciosContract
import kotlinx.android.synthetic.main.layout_servicios_item.view.*
import java.util.*

class SalesItemsAdapterRV(
        private val mListener: OnItemClickListener?,
        private val language: String? = null) : RecyclerView.Adapter<SalesItemsAdapterRV.InvoiceItemsHolder>() {

    private var items: List<ServiciosContract.Service> = listOf()

    init {
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: InvoiceItemsHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceItemsHolder {
        return InvoiceItemsHolder(parent.inflate(R.layout.layout_servicios_item))
    }

    override fun getItemId(position: Int): Long = items[position].id

    fun update(items: List<ServiciosContract.Service>) {
        this.items = items
        notifyDataSetChanged()
    }

    inner class InvoiceItemsHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(service: ServiciosContract.Service) {
            with(itemView) {
                serviceName.text = service.name
                servicePrice.text = "${service.price}"
                serviceIva.text = "${service.iva}"
                serviceDiscount.text = "${service.discount}"
                serviceAmount.text = "${service.amount}"

                itemView.setOnClickListener { mListener?.onItemClick(service) }

                if (!language.isNullOrEmpty()) {
                    val configuration = Configuration(context!!.resources.configuration)
                    configuration.setLocale(Locale(language))
                    val resource = context!!.createConfigurationContext(configuration).resources

                    findViewById<TextView>(R.id.stringAmount).text = resource.getString(R.string.layout_service_amount)
                    findViewById<TextView>(R.id.stringPrice).text = resource.getString(R.string.layout_service_price)
                    findViewById<TextView>(R.id.stringIva).text = resource.getString(R.string.layout_service_iva)
                    findViewById<TextView>(R.id.stringDiscount).text = resource.getString(R.string.layout_service_discount)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(service: ServiciosContract.Service)
    }
}