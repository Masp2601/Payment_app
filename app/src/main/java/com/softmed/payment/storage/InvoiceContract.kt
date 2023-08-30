package com.softmed.payment.storage

import android.annotation.SuppressLint
import android.os.Parcelable
import android.provider.BaseColumns
import kotlinx.android.parcel.Parcelize

class InvoiceContract internal constructor(){
    companion object: BaseColumns {
        val TABLE_NAME = "Invoice"
        val _ID = "_id"
        val COLUMN_INVOICE_NUMBER = "InvoiceNumber"
        val COLUMN_CLIENT_NAME = "ClientName"
        val COLUMN_CLIENT_ID = "ClientID"
        val COLUMN_SUBTOTAL = "Subtotal"
        val COLUMN_IVA = "Iva"
        val COLUMN_TOTAL = "Total"
        val COLUMN_DISCOUNT_TYPE = "DiscountType"
        val COLUMN_DISCOUNT_VALUE = "DiscountValue"
        val COLUMN_DATE = "Date"
        val COLUMN_TIME = "TimeMS"
        val COLUMN_NULLIFY = "Nullify"
        val COLUMN_CONTROL_TIME = "ControlTimeMS"
        val COLUMN_IS_CREDIT = "IsCredit"
        val COLUMN_CREDIT_TOTAL_PAID = "CreditTotalPaid"
    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class Invoice(val id: Long,
                       val invoiceNumber: Long,
                       val clientName: String,
                       val clientID: Long,
                       val subTotal: Double,
                       val iva: Double,
                       val total: Double,
                       val discountType: Int = 0,
                       val discountValue: Double = 0.0,
                       val date: String = "",
                       val timeMS: Long = 0,
                       val nullify: Int = 0,
                       val controlTimeMS: Long = 0,
                       val isCredit: Int = 0,
                       val totalPaid: Double = 0.0) : Parcelable

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class ClientDebts(
            val clientId: Long,
            val name: String,
            val debts: Double,
            val paid: Double,
            val totalInvoice: Long) : Parcelable

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class InvoiceDebtDetail(
            val id: Long,
            var invoiceNumber: Long,
            var date: String,
            var total: Double,
            var totalPaid: Double = 0.0,
            var payment: Double = 0.0
    ) : Parcelable
}