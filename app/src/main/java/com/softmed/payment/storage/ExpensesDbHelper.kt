package com.softmed.payment.storage

import android.content.Context
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update


class ExpensesDbHelper(ctx: Context) : BaseDBHelper(ctx) {
    companion object {
        private var instance: ExpensesDbHelper? = null

        @Synchronized
        fun getInstance(ctx: Context) : ExpensesDbHelper {
            if (instance == null) {
                instance = ExpensesDbHelper(ctx)
            }

            return instance!!
        }
    }

    fun insert(expense: ExpenseContract.Expense) {
        instance?.use {
            insert(ExpenseContract.TABLE_NAME,
                    ExpenseContract.COLUMN_PROVIDER_NAME to expense.providerName,
                    ExpenseContract.COLUMN_OBSERVATION to expense.observation,
                    ExpenseContract.COLUMN_ORDER_NUMBER to expense.orderNumber,
                    ExpenseContract.COLUMN_TOTAL_ORDER to expense.totalOrder,
                    ExpenseContract.COLUMN_TOTAL_PAID to expense.totalPaid,
                    ExpenseContract.COLUMN_FULL_PAID to expense.fullPaid,
                    ExpenseContract.COLUMN_DATE to expense.date,
                    ExpenseContract.COLUMN_TIME to expense.timeMS,
                    ExpenseContract.COLUMN_NULLIFY to expense.nullify,
                    ExpenseContract.COLUMN_DELIVERY_DATE to expense.deliveryDate,
                    ExpenseContract.COLUMN_DELIVERY_TIME to expense.deliveryTimeMS,
                    ExpenseContract.COLUMN_CONTROL_TIME to expense.controlTimeMS)
        }
    }

    fun update(id: Long, expense: ExpenseContract.Expense) {
        instance?.use {
            update(ExpenseContract.TABLE_NAME,
                    ExpenseContract.COLUMN_PROVIDER_NAME to expense.providerName,
                    ExpenseContract.COLUMN_OBSERVATION to expense.observation,
                    ExpenseContract.COLUMN_ORDER_NUMBER to expense.orderNumber,
                    ExpenseContract.COLUMN_TOTAL_ORDER to expense.totalOrder,
                    ExpenseContract.COLUMN_TOTAL_PAID to expense.totalPaid,
                    ExpenseContract.COLUMN_FULL_PAID to expense.fullPaid,
                    ExpenseContract.COLUMN_DATE to expense.date,
                    ExpenseContract.COLUMN_TIME to expense.timeMS,
                    ExpenseContract.COLUMN_NULLIFY to expense.nullify,
                    ExpenseContract.COLUMN_DELIVERY_DATE to expense.deliveryDate,
                    ExpenseContract.COLUMN_DELIVERY_TIME to expense.deliveryTimeMS,
                    ExpenseContract.COLUMN_CONTROL_TIME to expense.controlTimeMS)
                    .whereSimple("${ExpenseContract._ID} = ?", "$id")
                    .exec()
        }
    }

    fun delete(id: Long) {
        val selection = "${ExpenseContract._ID} = ?"
        val selectionArgs = arrayOf("$id")

        instance?.use {
            delete(ExpenseContract.TABLE_NAME, selection, selectionArgs)
        }
    }

    fun getAll(): List<ExpenseContract.Expense>? {
        return instance?.use {
            val rowParser = classParser<ExpenseContract.Expense>()
            val purchase = select(ExpenseContract.TABLE_NAME).parseList(rowParser)

            return@use purchase
        }
    }

    fun filterByDates(since: Long, end: Long): List<ExpenseContract.Expense>? {
        return instance?.use {
            val rowParser = classParser<ExpenseContract.Expense>()
            val list = select(ExpenseContract.TABLE_NAME)
                    .whereSimple("${ExpenseContract.COLUMN_CONTROL_TIME} >= ? and ${ExpenseContract.COLUMN_CONTROL_TIME} < ?", since.toString(), end.toString())
                    .parseList(rowParser)

            return@use list
        }
    }
}