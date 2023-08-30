package com.softmed.payment.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.softmed.payment.BaseActivity
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.db.*

open class BaseDBHelper(ctx: Context): ManagedSQLiteOpenHelper(ctx, BaseActivity.DATABASE_NAME, null, BaseActivity.DATABASE_VERSION), AnkoLogger {
    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(ClientesContract.TABLE_NAME, true,
                ClientesContract._ID to INTEGER + PRIMARY_KEY,
                ClientesContract.COLUMN_NAME to TEXT,
                ClientesContract.COLUMN_LASTNAME to TEXT,
                ClientesContract.COLUMN_NIT_CEDULA to TEXT,
                ClientesContract.COLUMN_EMAIL to TEXT,
                ClientesContract.COLUMN_PHONE_NUMBER to TEXT,
                ClientesContract.COLUMN_DIRECTION to TEXT,
                ClientesContract.COLUMN_BIRTHDAY to INTEGER,
                ClientesContract.COLUMN_BIRTHDAY_MONTH_DAY to TEXT)

        db.createTable(InvoiceContract.TABLE_NAME, true,
                InvoiceContract._ID to INTEGER + PRIMARY_KEY,
                InvoiceContract.COLUMN_INVOICE_NUMBER to INTEGER,
                InvoiceContract.COLUMN_CLIENT_NAME to TEXT,
                InvoiceContract.COLUMN_CLIENT_ID to INTEGER,
                InvoiceContract.COLUMN_SUBTOTAL to REAL,
                InvoiceContract.COLUMN_IVA to REAL,
                InvoiceContract.COLUMN_TOTAL to REAL,
                InvoiceContract.COLUMN_DISCOUNT_TYPE to INTEGER,
                InvoiceContract.COLUMN_DISCOUNT_VALUE to REAL,
                InvoiceContract.COLUMN_DATE to TEXT,
                InvoiceContract.COLUMN_TIME to INTEGER,
                InvoiceContract.COLUMN_NULLIFY to INTEGER,
                InvoiceContract.COLUMN_CONTROL_TIME to INTEGER,
                InvoiceContract.COLUMN_IS_CREDIT to INTEGER,
                InvoiceContract.COLUMN_CREDIT_TOTAL_PAID to REAL)

        db.createTable(InvoiceItemsContract.TABLE_NAME, true,
                InvoiceItemsContract._ID to INTEGER + PRIMARY_KEY,
                InvoiceItemsContract.COLUMN_INVOICE_NUMBER to INTEGER,
                InvoiceItemsContract.COLUMN_INVOICE_ITEM_NAME to TEXT,
                InvoiceItemsContract.COLUMN_INVOICE_ITEM_PRICE to REAL,
                InvoiceItemsContract.COLUMN_INVOICE_ITEM_IVA to REAL,
                InvoiceItemsContract.COLUMN_INVOICE_ITEM_DISCOUNT to REAL,
                InvoiceItemsContract.COLUMN_INVOICE_ITEM_AMOUNT to REAL)

        db.createTable(ServiciosContract.TABLE_NAME, true,
                ServiciosContract._ID to INTEGER + PRIMARY_KEY,
                ServiciosContract.COLUMN_NAME to TEXT,
                ServiciosContract.COLUMN_DESCRIPTION to TEXT,
                ServiciosContract.COLUMN_PRICE to REAL,
                ServiciosContract.COLUMN_IVA to REAL,
                ServiciosContract.COLUMN_DISCOUNT to REAL,
                ServiciosContract.COLUMN_AMOUNT to REAL)

        db.createTable(TransactionContract.TABLE_NAME, true,
                TransactionContract._ID to INTEGER + PRIMARY_KEY,
                TransactionContract.COLUMN_INVOICE_NUMBER to INTEGER,
                TransactionContract.COLUMN_INVOICE_TOTAL to REAL,
                TransactionContract.COLUMN_PAYMENT_TYPE to INTEGER,
                TransactionContract.COLUMN_PAYMENT_TOTAL to REAL,
                TransactionContract.COLUMN_PAYMENT_CARD_REFERENCE_NUMBER to TEXT,
                TransactionContract.COLUMN_PAYMENT_CREDIT_DEPOSIT to REAL,
                TransactionContract.COLUMN_PAYMENT_DATE to TEXT,
                TransactionContract.COLUMN_PAYMENT_DATETIME to INTEGER,
                TransactionContract.COLUMN_PAYMENT_CONTROL_TIME to INTEGER,
                TransactionContract.COLUMN_PAYMENT_CHECK_NUMBER to TEXT,
                TransactionContract.COLUMN_PAYMENT_CHECK_BANK_NAME to TEXT)

        db.createTable(ExpenseContract.TABLE_NAME, true,
                ExpenseContract._ID to INTEGER + PRIMARY_KEY,
                ExpenseContract.COLUMN_PROVIDER_NAME to TEXT,
                ExpenseContract.COLUMN_OBSERVATION to TEXT,
                ExpenseContract.COLUMN_ORDER_NUMBER to INTEGER,
                ExpenseContract.COLUMN_TOTAL_ORDER to REAL,
                ExpenseContract.COLUMN_TOTAL_PAID to REAL,
                ExpenseContract.COLUMN_FULL_PAID to INTEGER,
                ExpenseContract.COLUMN_DATE to TEXT,
                ExpenseContract.COLUMN_TIME to INTEGER,
                ExpenseContract.COLUMN_NULLIFY to INTEGER,
                ExpenseContract.COLUMN_DELIVERY_DATE to TEXT,
                ExpenseContract.COLUMN_DELIVERY_TIME to INTEGER,
                ExpenseContract.COLUMN_CONTROL_TIME to INTEGER)

        db.createTable(ProviderContract.TABLE_NAME, true,
                ProviderContract._ID to INTEGER + PRIMARY_KEY,
                ProviderContract.COLUMN_NAME to TEXT,
                ProviderContract.COLUMN_NIT_CEDULA to TEXT,
                ProviderContract.COLUMN_PHONE_NUMBER to TEXT,
                ProviderContract.COLUMN_EMAIL to TEXT,
                ProviderContract.COLUMN_DIRECTION to TEXT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
        if (oldVersion <= 6) {
            db.execSQL("ALTER TABLE ${ClientesContract.TABLE_NAME} ADD COLUMN ${ClientesContract.COLUMN_BIRTHDAY} INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE ${ClientesContract.TABLE_NAME} ADD COLUMN ${ClientesContract.COLUMN_BIRTHDAY_MONTH_DAY} TEXT DEFAULT 'N'")
        }
        if (oldVersion <= 7) {
            db.execSQL("ALTER TABLE ${TransactionContract.TABLE_NAME} ADD COLUMN ${TransactionContract.COLUMN_PAYMENT_CHECK_NUMBER} TEXT DEFAULT 'N'")
            db.execSQL("ALTER TABLE ${TransactionContract.TABLE_NAME} ADD COLUMN ${TransactionContract.COLUMN_PAYMENT_CHECK_BANK_NAME} TEXT DEFAULT 'N'")
        }
        if (oldVersion <= 8) {
            db.execSQL("ALTER TABLE ${InvoiceContract.TABLE_NAME} ADD COLUMN ${InvoiceContract.COLUMN_IS_CREDIT} INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE ${InvoiceContract.TABLE_NAME} ADD COLUMN ${InvoiceContract.COLUMN_CREDIT_TOTAL_PAID} REAL DEFAULT 0")
        }
        if (oldVersion <= 9) {
            val temp1 = "${ServiciosContract.TABLE_NAME}_temp"
            db.transaction {
                createTable(temp1, true,
                        ServiciosContract._ID to INTEGER + PRIMARY_KEY,
                        ServiciosContract.COLUMN_NAME to TEXT,
                        ServiciosContract.COLUMN_DESCRIPTION to TEXT,
                        ServiciosContract.COLUMN_PRICE to REAL,
                        ServiciosContract.COLUMN_IVA to REAL,
                        ServiciosContract.COLUMN_DISCOUNT to REAL,
                        ServiciosContract.COLUMN_AMOUNT to REAL)
                execSQL("INSERT INTO $temp1 SELECT ${ServiciosContract._ID}, ${ServiciosContract.COLUMN_NAME}, ${ServiciosContract.COLUMN_DESCRIPTION}, ${ServiciosContract.COLUMN_PRICE}, ${ServiciosContract.COLUMN_IVA}, ${ServiciosContract.COLUMN_DISCOUNT}, ${ServiciosContract.COLUMN_AMOUNT} FROM ${ServiciosContract.TABLE_NAME}")
                execSQL("DROP TABLE ${ServiciosContract.TABLE_NAME}")
                execSQL("ALTER TABLE $temp1 RENAME TO ${ServiciosContract.TABLE_NAME}")
            }

            val temp2 = "${InvoiceItemsContract.TABLE_NAME}_temp"
            db.transaction {
                createTable(temp2, true,
                        InvoiceItemsContract._ID to INTEGER + PRIMARY_KEY,
                        InvoiceItemsContract.COLUMN_INVOICE_NUMBER to INTEGER,
                        InvoiceItemsContract.COLUMN_INVOICE_ITEM_NAME to TEXT,
                        InvoiceItemsContract.COLUMN_INVOICE_ITEM_PRICE to REAL,
                        InvoiceItemsContract.COLUMN_INVOICE_ITEM_IVA to REAL,
                        InvoiceItemsContract.COLUMN_INVOICE_ITEM_DISCOUNT to REAL,
                        InvoiceItemsContract.COLUMN_INVOICE_ITEM_AMOUNT to REAL)
                execSQL("INSERT INTO $temp2 SELECT ${InvoiceItemsContract._ID}, ${InvoiceItemsContract.COLUMN_INVOICE_NUMBER}, ${InvoiceItemsContract.COLUMN_INVOICE_ITEM_NAME}, ${InvoiceItemsContract.COLUMN_INVOICE_ITEM_PRICE}, ${InvoiceItemsContract.COLUMN_INVOICE_ITEM_IVA}, ${InvoiceItemsContract.COLUMN_INVOICE_ITEM_DISCOUNT}, ${InvoiceItemsContract.COLUMN_INVOICE_ITEM_AMOUNT} FROM ${InvoiceItemsContract.TABLE_NAME}")
                execSQL("DROP TABLE ${InvoiceItemsContract.TABLE_NAME}")
                execSQL("ALTER TABLE $temp2 RENAME TO ${InvoiceItemsContract.TABLE_NAME}")
            }
        }
    }
}