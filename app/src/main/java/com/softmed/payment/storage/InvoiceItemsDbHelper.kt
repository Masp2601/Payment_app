package com.softmed.payment.storage

import android.content.Context
import android.telephony.IccOpenLogicalChannelResponse
import org.jetbrains.anko.db.*
import org.jetbrains.anko.info

class InvoiceItemsDbHelper(ctx: Context): BaseDBHelper(ctx) {
    companion object {
        private var instance: InvoiceItemsDbHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): InvoiceItemsDbHelper {
            if (instance == null) {
                instance = InvoiceItemsDbHelper(ctx)
            }

            return instance!!
        }
    }

    fun insert(item: InvoiceItemsContract.InvoiceItems) {
        instance?.use {
            insert(InvoiceItemsContract.TABLE_NAME,
                    InvoiceItemsContract.COLUMN_INVOICE_NUMBER to item.invoiceNumber,
                    InvoiceItemsContract.COLUMN_INVOICE_ITEM_NAME to item.itemName,
                    InvoiceItemsContract.COLUMN_INVOICE_ITEM_PRICE to item.itemPrice,
                    InvoiceItemsContract.COLUMN_INVOICE_ITEM_IVA to item.itemIva,
                    InvoiceItemsContract.COLUMN_INVOICE_ITEM_DISCOUNT to item.itemDiscount,
                    InvoiceItemsContract.COLUMN_INVOICE_ITEM_AMOUNT to item.itemAmount)
        }
    }

    fun insert(items: List<InvoiceItemsContract.InvoiceItems>) {
        for (item in items) {
            insert(item)
        }
    }

    fun get(invoiceNumber: Long): List<InvoiceItemsContract.InvoiceItems>? {
        val rowParser = classParser<InvoiceItemsContract.InvoiceItems>()

        return instance?.use {
           return@use select(InvoiceItemsContract.TABLE_NAME).whereSimple("${InvoiceItemsContract.COLUMN_INVOICE_NUMBER} = ?", "$invoiceNumber").parseList(rowParser)
        }
    }

    fun getAll(): List<InvoiceItemsContract.InvoiceItems>? {
        return instance?.use {
            val rowParser = classParser<InvoiceItemsContract.InvoiceItems>()
            val invoices = select(InvoiceItemsContract.TABLE_NAME).parseList(rowParser)

            return@use invoices
        }
    }

    fun getInvoiceNumbersByItem(name: String): List<Long>? {
        return instance?.use {
            val rowParser = classParser<InvoiceItemsContract.InvoiceItems>()
            val invoices = select(InvoiceItemsContract.TABLE_NAME)
                    .whereSimple("${InvoiceItemsContract.COLUMN_INVOICE_ITEM_NAME} = ?", name)
                    .parseList(rowParser)

            return@use invoices.map { it.invoiceNumber }
        }
    }

    fun getResumenItemSoldByDate(since: Long, end: Long) : List<InvoiceItemsContract.InvoiceItemsTotalResumen>? {
       return instance?.use {
            val query = "SELECT " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_NUMBER}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_NAME}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_PRICE}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_DISCOUNT}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_IVA}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_AMOUNT}, " +
                    "IC.${InvoiceContract.COLUMN_CLIENT_NAME} " +
                    ",IC.${InvoiceContract.COLUMN_CONTROL_TIME} " +
                    "FROM ${InvoiceItemsContract.TABLE_NAME} IIC " +
                    "INNER JOIN ${InvoiceContract.TABLE_NAME} IC ON IIC.${InvoiceItemsContract.COLUMN_INVOICE_NUMBER} = IC.${InvoiceContract.COLUMN_INVOICE_NUMBER} " +
                    "WHERE IC.${InvoiceContract.COLUMN_CONTROL_TIME} >= ? AND IC.${InvoiceContract.COLUMN_CONTROL_TIME} < ? " +
                    "AND IC.${InvoiceContract.COLUMN_NULLIFY} = 0"

            val resumen = mutableListOf<InvoiceItemsContract.InvoiceItemsTotalResumen>()
            val result = rawQuery(query, arrayOf(since.toString(), end.toString()))
            result.asSequence().forEach { sequence ->
                val item = resumen.firstOrNull { it.itemName == sequence[1] }
                val amount = (sequence[5] as Double)
                val price = (sequence[2] as Double) * (sequence[5] as Double)
                val discount = price * (sequence[3] as Double) / 100
                val iva = (price - discount) * (sequence[4] as Double) / 100
                val total = price + iva - discount

                if (item != null) {
                    item.itemAmountTotal += amount
                    item.itemDiscountTotal += discount
                    item.itemIvaTotal += iva
                    item.itemPriceTotal += total
                } else {
                    resumen.add(InvoiceItemsContract.InvoiceItemsTotalResumen(
                            itemName = sequence[1] as String,
                            itemPriceTotal = total,
                            itemIvaTotal = iva,
                            itemDiscountTotal = discount,
                            itemAmountTotal = amount))
                }
            }
            result.close()

           return@use resumen.toList()
        }
    }

    fun getItemsByDate(since: Long, end: Long): List<InvoiceItemsContract.InvoiceItems>? {
        return instance?.use {
            val query = "SELECT " +
                    "IIC.${InvoiceItemsContract._ID}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_NUMBER}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_NAME}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_PRICE}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_IVA}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_DISCOUNT}, " +
                    "IIC.${InvoiceItemsContract.COLUMN_INVOICE_ITEM_AMOUNT} " +
                    "FROM ${InvoiceItemsContract.TABLE_NAME} IIC " +
                    "INNER JOIN ${InvoiceContract.TABLE_NAME} IC ON IIC.${InvoiceItemsContract.COLUMN_INVOICE_NUMBER} = IC.${InvoiceContract.COLUMN_INVOICE_NUMBER} " +
                    "WHERE IC.${InvoiceContract.COLUMN_CONTROL_TIME} >= ? AND IC.${InvoiceContract.COLUMN_CONTROL_TIME} < ?"

            return@use rawQuery(query, arrayOf(since.toString(), end.toString())).parseList(classParser<InvoiceItemsContract.InvoiceItems>())
        }
    }
}