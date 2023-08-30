package com.softmed.payment.storage

import android.annotation.SuppressLint
import android.os.Parcelable
import android.provider.BaseColumns
import kotlinx.android.parcel.Parcelize

class ClientesContract internal constructor () {
    companion object: BaseColumns {
        val TABLE_NAME: String = "Clientes"
        val _ID: String = "_id"
        val COLUMN_NAME: String = "Name"
        val COLUMN_LASTNAME: String = "LastName"
        val COLUMN_NIT_CEDULA: String = "Nit"
        val COLUMN_PHONE_NUMBER: String = "PhoneNumber"
        val COLUMN_EMAIL: String = "Email"
        val COLUMN_DIRECTION: String = "Direction"
        val COLUMN_BIRTHDAY = "Birthday"
        val COLUMN_BIRTHDAY_MONTH_DAY = "Birthday_Day_Month"
    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class Cliente(val id: Long,
                       val name: String,
                       val lastname: String = "",
                       val nit: String = "",
                       val email: String = "",
                       val phoneNumber: String = "",
                       val direction: String = "",
                       val birthday: Long = 0,
                       val birthday_month_day: String = "") : Parcelable
}