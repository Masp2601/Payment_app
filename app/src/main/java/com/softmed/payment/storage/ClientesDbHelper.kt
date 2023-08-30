package com.softmed.payment.storage

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.BaseActivity
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.R
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update
import java.util.*

class ClientesDbHelper(private val ctx: Context) : BaseDBHelper(ctx) {
    companion object {
        private var instance: ClientesDbHelper? = null
        private lateinit var mFirebaseAnalytics: FirebaseAnalytics

        @Synchronized
        fun getInstance(ctx: Context): ClientesDbHelper {
            if (instance == null){
                instance = ClientesDbHelper(ctx)
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
            }

            return instance!!
        }
    }

    fun insert(name: String,
               lastname: String = "",
               nit: String = "",
               phoneNumber: String = "",
               email: String = "",
               direction: String = "",
               birthday: Long = 0,
               birthday_month_day: String = "") {

        instance?.use { insert(ClientesContract.TABLE_NAME,
                ClientesContract.COLUMN_NAME to name,
                ClientesContract.COLUMN_LASTNAME to lastname,
                ClientesContract.COLUMN_NIT_CEDULA to nit,
                ClientesContract.COLUMN_EMAIL to email,
                ClientesContract.COLUMN_PHONE_NUMBER to phoneNumber,
                ClientesContract.COLUMN_DIRECTION to direction,
                ClientesContract.COLUMN_BIRTHDAY to birthday,
                ClientesContract.COLUMN_BIRTHDAY_MONTH_DAY to birthday_month_day) }

        val bundle = Bundle()
        bundle.putString("invoice_time", DateTimeHelper.dateToString(Date(), DateTimeHelper.PATTERN_REQUEST_RESPONSE))
        mFirebaseAnalytics.logEvent("client_added", bundle)

        val preferences = ctx.getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
        val clients = preferences.getInt(ctx.getString(R.string.pref_value_helper_total_clients), 0)
        val editor = preferences.edit()
        editor.putInt(ctx.getString(R.string.pref_value_helper_total_clients), clients + 1)
        editor.apply()
    }

    fun get(id: Long): ClientesContract.Cliente? {
        return instance?.use {
            return@use select(ClientesContract.TABLE_NAME)
                    .whereSimple("${ClientesContract._ID} = ?", "$id")
                    .parseOpt(classParser<ClientesContract.Cliente>())
        }
    }

    fun getAll(): List<ClientesContract.Cliente>? {
        return instance?.use {
            val rowParser = classParser<ClientesContract.Cliente>()
            val clientes = select(ClientesContract.TABLE_NAME).orderBy(ClientesContract.COLUMN_NAME).parseList(rowParser)

            return@use clientes
        }
    }

    fun update(id: Long, cliente: ClientesContract.Cliente) {
        instance?.use {
            update(ClientesContract.TABLE_NAME,
                    ClientesContract.COLUMN_NAME to cliente.name,
                    ClientesContract.COLUMN_LASTNAME to cliente.lastname,
                    ClientesContract.COLUMN_NIT_CEDULA to cliente.nit,
                    ClientesContract.COLUMN_EMAIL to cliente.email,
                    ClientesContract.COLUMN_PHONE_NUMBER to cliente.phoneNumber,
                    ClientesContract.COLUMN_DIRECTION to cliente.direction,
                    ClientesContract.COLUMN_BIRTHDAY to cliente.birthday,
                    ClientesContract.COLUMN_BIRTHDAY_MONTH_DAY to cliente.birthday_month_day)
                    .whereSimple("${ClientesContract._ID} = ?", "$id")
                    .exec()
        }
    }

    fun delete(id: Long) {
        val selection = "${ClientesContract._ID} = ?"
        val selectionArgs: Array<String> = arrayOf("$id")

        instance?.use {
            delete(ClientesContract.TABLE_NAME, selection, selectionArgs)
        }

        val preferences = ctx.getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
        val clients = preferences.getInt(ctx.getString(R.string.pref_value_helper_total_clients), 0)
        val editor = preferences.edit()
        editor.putInt(ctx.getString(R.string.pref_value_helper_total_clients), clients - 1)
        editor.apply()
    }

    fun getBirthdayOfTheDay(): List<ClientesContract.Cliente>? {
        val date = DateTimeHelper.dateToString(Date(), DateTimeHelper.PATTERN_BIRTHDAY_MONTH_DAY)

        return instance?.use {
            val rowParser = classParser<ClientesContract.Cliente>()
            val clientes = select(ClientesContract.TABLE_NAME)
                    .whereSimple("${ClientesContract.COLUMN_BIRTHDAY_MONTH_DAY} = ?", date)
                    .orderBy(ClientesContract.COLUMN_BIRTHDAY)
                    .parseList(rowParser)

            return@use clientes
        }
    }

    fun getBirthdayOfTheDay(monthDay: String): List<ClientesContract.Cliente>? {
        return instance?.use {
            val rowParser = classParser<ClientesContract.Cliente>()
            val clientes = select(ClientesContract.TABLE_NAME)
                    .whereSimple("${ClientesContract.COLUMN_BIRTHDAY_MONTH_DAY} = ?", monthDay)
                    .orderBy(ClientesContract.COLUMN_BIRTHDAY)
                    .parseList(rowParser)

            return@use clientes
        }
    }

    fun getBirthdaysFor3Days(): List<ClientesContract.Cliente>? {
        val calendar = Calendar.getInstance()

        return instance?.use {
            val clients = mutableListOf<ClientesContract.Cliente>()

            for (i in 0..2) {
                val date = DateTimeHelper.dateToString(calendar.time, DateTimeHelper.PATTERN_BIRTHDAY_MONTH_DAY)
                val birthdays = getBirthdayOfTheDay(date)
                if (birthdays != null) {
                    clients.addAll(birthdays)
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            clients.sortBy { it.birthday_month_day }

            return@use clients
        }
    }
}