package com.softmed.payment.storage

import android.annotation.SuppressLint
import android.os.Parcelable
import android.provider.BaseColumns
import android.text.BoringLayout
import kotlinx.android.parcel.Parcelize

class ExpenseContract internal constructor(){
    companion object : BaseColumns {
        val TABLE_NAME = "Expenses"
        val _ID = "_id"
        val COLUMN_PROVIDER_NAME = "ProviderName"
        val COLUMN_OBSERVATION = "Observation"
        val COLUMN_ORDER_NUMBER = "OrderNumber"
        val COLUMN_TOTAL_ORDER = "TotalOrder"
        val COLUMN_TOTAL_PAID = "TotalPaid"
        val COLUMN_FULL_PAID = "FullPaid"
        val COLUMN_DATE = "Date"
        val COLUMN_TIME = "TimeMS"
        val COLUMN_NULLIFY = "Nullify"
        val COLUMN_DELIVERY_DATE = "DeliveryDate"
        val COLUMN_DELIVERY_TIME = "DeliveryTimeMS"
        val COLUMN_CONTROL_TIME = "ControlTimeMS"

    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class Expense(
            val id: Long,
            val providerName: String,
            val observation: String,
            val orderNumber: Long,
            val totalOrder: Double,
            val totalPaid: Double,
            val fullPaid: Int = 1,
            val date: String = "",
            val timeMS: Long = 0,
            val nullify: Int = 0,
            val deliveryDate: String = "",
            val deliveryTimeMS: Long = 0,
            val controlTimeMS: Long = 0
    ) : Parcelable
}