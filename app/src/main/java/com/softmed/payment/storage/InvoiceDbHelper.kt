package com.softmed.payment.storage

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.helpers.DateTimeHelper
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.db.*
import org.jetbrains.anko.info
import java.util.*

class InvoiceDbHelper(ctx: Context) : BaseDBHelper(ctx), AnkoLogger {
    companion object {
        private var instance: InvoiceDbHelper? = null
        private lateinit var mFirebaseAnalytics: FirebaseAnalytics
        private lateinit var transactionInstance: TransactionDbHelper

        @Synchronized
        fun getInstance(ctx: Context): InvoiceDbHelper {
            if (instance == null) {
                instance = InvoiceDbHelper(ctx)
                transactionInstance = TransactionDbHelper.getInstance(ctx)
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
            }

            return instance!!
        }
    }

    fun insert(invoice: InvoiceContract.Invoice) {
        instance?.use {
            insert(InvoiceContract.TABLE_NAME,
                    InvoiceContract.COLUMN_INVOICE_NUMBER to invoice.invoiceNumber,
                    InvoiceContract.COLUMN_CLIENT_NAME to invoice.clientName,
                    InvoiceContract.COLUMN_CLIENT_ID to invoice.clientID,
                    InvoiceContract.COLUMN_SUBTOTAL to invoice.subTotal,
                    InvoiceContract.COLUMN_IVA to invoice.iva,
                    InvoiceContract.COLUMN_TOTAL to invoice.total,
                    InvoiceContract.COLUMN_DISCOUNT_TYPE to invoice.discountType,
                    InvoiceContract.COLUMN_DISCOUNT_VALUE to invoice.discountValue,
                    InvoiceContract.COLUMN_DATE to invoice.date,
                    InvoiceContract.COLUMN_TIME to invoice.timeMS,
                    InvoiceContract.COLUMN_NULLIFY to invoice.nullify,
                    InvoiceContract.COLUMN_CONTROL_TIME to invoice.controlTimeMS,
                    InvoiceContract.COLUMN_IS_CREDIT to invoice.isCredit,
                    InvoiceContract.COLUMN_CREDIT_TOTAL_PAID to invoice.totalPaid)

            val bundle = Bundle()
            bundle.putString("invoice_time", DateTimeHelper.dateToString(Date(invoice.timeMS), DateTimeHelper.PATTERN_REQUEST_RESPONSE))

            mFirebaseAnalytics.logEvent("invoice_added", bundle)
        }
    }

    fun update(id: Long, invoice: InvoiceContract.Invoice) {
        instance?.use {
            update(InvoiceContract.TABLE_NAME,
                    InvoiceContract.COLUMN_INVOICE_NUMBER to invoice.invoiceNumber,
                    InvoiceContract.COLUMN_CLIENT_NAME to invoice.clientName,
                    InvoiceContract.COLUMN_CLIENT_ID to invoice.clientID,
                    InvoiceContract.COLUMN_SUBTOTAL to invoice.subTotal,
                    InvoiceContract.COLUMN_IVA to invoice.iva,
                    InvoiceContract.COLUMN_TOTAL to invoice.total,
                    InvoiceContract.COLUMN_DISCOUNT_TYPE to invoice.discountType,
                    InvoiceContract.COLUMN_DISCOUNT_VALUE to invoice.discountValue,
                    InvoiceContract.COLUMN_DATE to invoice.date,
                    InvoiceContract.COLUMN_TIME to invoice.timeMS,
                    InvoiceContract.COLUMN_NULLIFY to invoice.nullify,
                    InvoiceContract.COLUMN_CONTROL_TIME to invoice.controlTimeMS,
                    InvoiceContract.COLUMN_IS_CREDIT to invoice.isCredit,
                    InvoiceContract.COLUMN_CREDIT_TOTAL_PAID to invoice.totalPaid)
                    .whereSimple("${InvoiceContract._ID} = ?", "$id")
                    .exec()
        }
    }

    fun updateCredit(id: Long, totalPaid: Double) {
        instance?.use {
            update(InvoiceContract.TABLE_NAME,
                    InvoiceContract.COLUMN_CREDIT_TOTAL_PAID to totalPaid)
                    .whereSimple("${InvoiceContract._ID} = ?", "$id")
                    .exec()
        }

    }

    fun delete(id: Long) {
        val selection = "${InvoiceContract._ID} = ?"
        val selectionArgs = arrayOf("$id")

        instance?.use {
            delete(InvoiceContract.TABLE_NAME, selection, selectionArgs)
        }
    }

    fun totalSales(): Long {
        return instance?.use {

            var total = 0L
            val query = "SELECT count(${InvoiceContract._ID}) FROM ${InvoiceContract.TABLE_NAME}"
            val cr: Cursor = rawQuery(query, null)
            cr.asSequence().forEach { total = it[0] as Long }
            cr.close()

            return@use total
        } ?: 0L
    }

    fun getAll(): List<InvoiceContract.Invoice>? {
        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            val invoices = select(InvoiceContract.TABLE_NAME).parseList(rowParser)

            return@use invoices
        }
    }

    fun getAllAtDay(controlDate: String): List<InvoiceContract.Invoice>? {
        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            val invoices = select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_DATE} = ? and ${InvoiceContract.COLUMN_NULLIFY} == 0", controlDate)
                    .parseList(rowParser)

            return@use invoices
        }
    }

    fun find(invoiceNumber: Long): InvoiceContract.Invoice? {
        return instance?.use {
            return@use select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_INVOICE_NUMBER} = ?",
                            "$invoiceNumber")
                    .parseOpt(classParser<InvoiceContract.Invoice>())
        }
    }

    fun getAllAtDayIncludeNullify(controlDate: String): List<InvoiceContract.Invoice>? {
        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            val invoices = select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_DATE} = ?", controlDate)
                    .parseList(rowParser)

            return@use invoices
        }
    }

    fun filterByDates(since: Long, end: Long): List<InvoiceContract.Invoice>? {
        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            val invoices = select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_CONTROL_TIME} >= ? and ${InvoiceContract.COLUMN_CONTROL_TIME} < ?", since.toString(), end.toString())
                    .parseList(rowParser)

            return@use invoices
        }
    }

    fun filterByClientId(id: Long) : List<InvoiceContract.Invoice>? {
        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            val invoices = select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_CLIENT_ID} = ?", "$id")
                    .parseList(rowParser)

            return@use invoices
        }
    }

    fun filterByClientIdAndDate(id: Long, since: Long, end: Long) : List<InvoiceContract.Invoice>? {
        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            val invoices = select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_CLIENT_ID} = ? and ${InvoiceContract.COLUMN_CONTROL_TIME} >= ? and ${InvoiceContract.COLUMN_CONTROL_TIME} < ?", "$id", "$since", "$end")
                    .parseList(rowParser)

            return@use invoices
        }
    }

    fun getInvoicesByNumbers(numbers: List<Long>): List<InvoiceContract.Invoice>? {
        val list = mutableListOf<InvoiceContract.Invoice>()

        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            for (number in numbers){
                val invoices = select(InvoiceContract.TABLE_NAME)
                        .whereSimple("${InvoiceContract.COLUMN_INVOICE_NUMBER} = ?", "$number")
                        .parseList(rowParser)
                list.addAll(invoices)
            }
            return@use list
        }
    }

    fun getInvoicesByNumbers(numbers: List<Long>, since: Long, end: Long): List<InvoiceContract.Invoice>? {
        val list = mutableListOf<InvoiceContract.Invoice>()

        return instance?.use {
            val rowParser = classParser<InvoiceContract.Invoice>()
            for (number in numbers){
                val invoices = select(InvoiceContract.TABLE_NAME)
                        .whereSimple("${InvoiceContract.COLUMN_INVOICE_NUMBER} = ? and ${InvoiceContract.COLUMN_CONTROL_TIME} >= ? and ${InvoiceContract.COLUMN_CONTROL_TIME} < ?", "$number", "$since", "$end")
                        .parseList(rowParser)
                list.addAll(invoices)
            }
            return@use list
        }
    }

    fun nullifyInvoice(id: Long, value: Int) {
        instance?.use {
            update(InvoiceContract.TABLE_NAME,
                    InvoiceContract.COLUMN_NULLIFY to value)
                    .whereSimple("${InvoiceContract._ID} = ?", "$id")
                    .exec()
        }
    }

    fun getClientDebts(): List<InvoiceContract.ClientDebts>? {
        return instance?.use {

            val debts = select(InvoiceContract.TABLE_NAME)
                    .columns(InvoiceContract.COLUMN_CLIENT_ID,
                            InvoiceContract.COLUMN_CLIENT_NAME,
                            "sum(${InvoiceContract.COLUMN_TOTAL})",
                            "sum(${InvoiceContract.COLUMN_CREDIT_TOTAL_PAID})",
                            "count(${InvoiceContract.COLUMN_CLIENT_NAME})")
                    .whereSimple("${InvoiceContract.COLUMN_IS_CREDIT} = ? " +
                            "and ${InvoiceContract.COLUMN_TOTAL} != ${InvoiceContract.COLUMN_CREDIT_TOTAL_PAID}", "1")
                    .groupBy(InvoiceContract.COLUMN_CLIENT_ID)
                    .parseList(object : RowParser<InvoiceContract.ClientDebts> {
                        override fun parseRow(columns: Array<Any?>): InvoiceContract.ClientDebts {
                            val paid = columns[3] as Double
                            val totalInvoice = columns[2] as Double
                            return InvoiceContract.ClientDebts(
                                    columns[0] as Long,
                                    columns[1].toString(),
                                    totalInvoice - paid,
                                    paid,
                                    columns[4] as Long
                            )
                        }
                    })

            return@use debts
        }
    }

    fun getAllDebtsForClient(clientId: Long): List<InvoiceContract.Invoice>? {
        return instance?.use {
            val parser = classParser<InvoiceContract.Invoice>()

            return@use select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_IS_CREDIT} = ? " +
                            "and ${InvoiceContract.COLUMN_TOTAL} != ${InvoiceContract.COLUMN_CREDIT_TOTAL_PAID} " +
                            "and ${InvoiceContract.COLUMN_CLIENT_ID} = ?", "1", "$clientId")
                    .parseList(parser)
        }
    }

    fun payAllDebtsForClient(name: String, dateControl: Long): Boolean? {
        return instance?.use {
            val parser = classParser<InvoiceContract.Invoice>()
            val invoices = select(InvoiceContract.TABLE_NAME)
                    .whereSimple("${InvoiceContract.COLUMN_IS_CREDIT} = ? " +
                            "and ${InvoiceContract.COLUMN_TOTAL} != ${InvoiceContract.COLUMN_CREDIT_TOTAL_PAID} " +
                            "and ${InvoiceContract.COLUMN_CLIENT_NAME} = ?", "1", name)
                    .parseList(parser)

            val paymentDate = Date()

            invoices.forEach {
                val payment = it.total - it.totalPaid

                val transaction = TransactionContract.Transaction(
                        id = 0,
                        invoiceNumber = it.invoiceNumber,
                        invoiceTotal = it.total,
                        paymentType = TransactionContract.PaymentMethods.Cash.ordinal,
                        paymentTotal = payment,
                        paymentCreditDeposit = 0.0,
                        paymentDate = DateTimeHelper.dateToString(paymentDate),
                        paymentDateTime = paymentDate.time,
                        paymentControlTime = dateControl
                )

                update(InvoiceContract.TABLE_NAME,
                        InvoiceContract.COLUMN_CREDIT_TOTAL_PAID to it.total)
                        .whereSimple("${InvoiceContract._ID} = ?", "${it.id}")
                        .exec()

                transactionInstance.insert(transaction)
            }

            return@use true
        }
    }
}