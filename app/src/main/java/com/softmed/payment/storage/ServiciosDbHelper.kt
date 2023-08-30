package com.softmed.payment.storage

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.helpers.DateTimeHelper
import org.jetbrains.anko.db.*
import java.util.*

class ServiciosDbHelper(ctx: Context) : BaseDBHelper(ctx) {
    companion object {
        private var instance: ServiciosDbHelper? = null
        private lateinit var mFirebaseAnalytics: FirebaseAnalytics

        @Synchronized
        fun getInstance(ctx: Context): ServiciosDbHelper {
            if (instance == null) {
                instance = ServiciosDbHelper(ctx)
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
            }

            return instance!!
        }
    }

    fun insert(servicio: ServiciosContract.Service) {
        instance?.use {
            insert(ServiciosContract.TABLE_NAME,
                    ServiciosContract.COLUMN_NAME to servicio.name,
                    ServiciosContract.COLUMN_PRICE to servicio.price,
                    ServiciosContract.COLUMN_IVA to servicio.iva,
                    ServiciosContract.COLUMN_DISCOUNT to servicio.discount,
                    ServiciosContract.COLUMN_DESCRIPTION to servicio.description,
                    ServiciosContract.COLUMN_AMOUNT to servicio.amount)
        }

        val bundle = Bundle()
        bundle.putString("time", DateTimeHelper.dateToString(Date(), DateTimeHelper.PATTERN_REQUEST_RESPONSE))
        mFirebaseAnalytics.logEvent("service_added", bundle)

    }

    fun update(id: Long, servicio: ServiciosContract.Service) {
        instance?.use {
            update(ServiciosContract.TABLE_NAME,
                    ServiciosContract.COLUMN_NAME to servicio.name,
                    ServiciosContract.COLUMN_PRICE to servicio.price,
                    ServiciosContract.COLUMN_IVA to servicio.iva,
                    ServiciosContract.COLUMN_DISCOUNT to servicio.discount,
                    ServiciosContract.COLUMN_DESCRIPTION to servicio.description,
                    ServiciosContract.COLUMN_AMOUNT to servicio.amount)
                    .whereSimple("${ServiciosContract._ID} = ?", "$id")
                    .exec()
        }
    }

    fun delete(id: Long) {
        val selection = "${ServiciosContract._ID} = ?"
        val selectionArgs = arrayOf("$id")

        instance?.use {
            delete(ServiciosContract.TABLE_NAME, selection, selectionArgs)
        }
    }

    fun getAll(): List<ServiciosContract.Service>? {
        return instance?.use {
            val rowParser = classParser<ServiciosContract.Service>()
            val servicios = select(ServiciosContract.TABLE_NAME).orderBy(ServiciosContract.COLUMN_NAME).parseList(rowParser)

            return@use servicios
        }
    }

}