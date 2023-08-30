package com.softmed.payment.storage

import android.annotation.SuppressLint
import android.os.Parcelable
import android.provider.BaseColumns
import kotlinx.android.parcel.Parcelize

class TransactionContract {
    companion object: BaseColumns {
        val TABLE_NAME = "Payment"
        val _ID = "_id"
        val COLUMN_INVOICE_NUMBER = "InvoiceNumber"
        val COLUMN_INVOICE_TOTAL = "InvoiceTotal"
        val COLUMN_PAYMENT_TYPE = "PaymentType"
        val COLUMN_PAYMENT_TOTAL = "PaymentTotal"
        val COLUMN_PAYMENT_CARD_REFERENCE_NUMBER = "PaymentCardReference"
        val COLUMN_PAYMENT_CREDIT_DEPOSIT = "PaymentCreditDeposit"
        val COLUMN_PAYMENT_DATE = "PaymentDate"
        val COLUMN_PAYMENT_DATETIME = "PaymentDateTime"
        val COLUMN_PAYMENT_CONTROL_TIME = "PaymentControlTimeMS"
        val COLUMN_PAYMENT_CHECK_NUMBER = "PaymentCheckNumber"
        val COLUMN_PAYMENT_CHECK_BANK_NAME = "PaymentCheckBankName"
    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class Transaction(
            val id: Long,
            val invoiceNumber: Long,
            val invoiceTotal: Double,
            val paymentType: Int,
            val paymentTotal: Double,
            val paymentCardReference: String = "",
            val paymentCreditDeposit: Double = 0.0,
            val paymentDate: String = "",
            val paymentDateTime: Long = 0L,
            val paymentControlTime: Long = 0L,
            val paymentCheckNumber: String = "",
            val paymentCheckBankName: String = ""
    ) : Parcelable

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class PaymentDialogData(
            val invoiceId: Long,
            val invoiceNumber: Long,
            val invoiceTotal: Double,
            val paymentMethod: TransactionContract.PaymentMethods = TransactionContract.PaymentMethods.Cash,
            val paymentTotal: Double = 0.0,
            val cardReference: String = "",
            val checkNumber: String = "",
            val checkBankName: String = ""
    ) : Parcelable

    enum class PaymentMethods {
        Cash,
        Card,
        Check,
        Credit
    }
}