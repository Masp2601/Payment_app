package com.softmed.payment.storage

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

class ServiciosContract internal constructor(){
    companion object {
        const val TABLE_NAME = "Servicios"
        const val _ID = "_id"
        const val COLUMN_NAME = "Name"
        const val COLUMN_DESCRIPTION = "Description"
        const val COLUMN_PRICE = "Price"
        const val COLUMN_IVA = "Iva"
        const val COLUMN_DISCOUNT = "Discount"
        const val COLUMN_AMOUNT = "Amount"
    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class Service(
            val id: Long,
            val name: String,
            val description: String = "",
            var price: Double = 0.0,
            var iva: Double = 0.0,
            var discount: Double = 0.0,
            var amount: Double = 1.0) : Parcelable
}