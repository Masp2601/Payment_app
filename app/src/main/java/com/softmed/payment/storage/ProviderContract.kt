package com.softmed.payment.storage

import android.annotation.SuppressLint
import android.os.Parcelable
import android.provider.BaseColumns
import kotlinx.android.parcel.Parcelize


class ProviderContract internal constructor(){
    companion object: BaseColumns {
        val TABLE_NAME = "Providers"
        val _ID = "_id"
        val COLUMN_NAME = "Nombre"
        val COLUMN_NIT_CEDULA: String = "Nit"
        val COLUMN_PHONE_NUMBER: String = "PhoneNumber"
        val COLUMN_EMAIL: String = "Email"
        val COLUMN_DIRECTION: String = "Direction"
    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class Provider(
            val id: Long,
            val name: String,
            val nit: String = "",
            val phoneNumber: String = "",
            val email: String = "",
            val direction: String = ""
    ) : Parcelable
}