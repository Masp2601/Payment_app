package com.softmed.payment.storage

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

class InvoiceItemsContract {
    companion object {
        const val TABLE_NAME = "InvoiceItems"
        const val _ID = "_id"
        const val COLUMN_INVOICE_NUMBER = "InvoiceNumber"
        const val COLUMN_INVOICE_ITEM_NAME = "ItemName"
        const val COLUMN_INVOICE_ITEM_PRICE = "ItemPrice"
        const val COLUMN_INVOICE_ITEM_DISCOUNT = "ItemDiscount"
        const val COLUMN_INVOICE_ITEM_IVA = "ItemIva"
        const val COLUMN_INVOICE_ITEM_AMOUNT = "ItemAmount"

        fun convertServicesToItems(services: List<ServiciosContract.Service>, invoiceNumber: Long = 0) : List<InvoiceItems> {
            return services.map { InvoiceItems(
                    id = 0,
                    invoiceNumber = invoiceNumber,
                    itemName = it.name,
                    itemPrice = it.price,
                    itemDiscount = it.discount,
                    itemIva = it.iva,
                    itemAmount = it.amount
            ) }
        }
    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class InvoiceItems(
            val id: Long,
            val invoiceNumber: Long,
            val itemName: String,
            val itemPrice: Double,
            val itemIva: Double,
            val itemDiscount: Double,
            val itemAmount: Double) : Parcelable

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class InvoiceItemsTotalResumen(
            var itemName: String = "",
            var itemPriceTotal: Double = 0.0,
            var itemIvaTotal: Double = 0.0,
            var itemDiscountTotal: Double = 0.0,
            var itemAmountTotal: Double = 0.0 ) : Parcelable


}