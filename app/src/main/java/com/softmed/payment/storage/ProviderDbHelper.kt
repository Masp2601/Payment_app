package com.softmed.payment.storage

import android.content.Context
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update


class ProviderDbHelper(ctx: Context) : BaseDBHelper(ctx) {
    companion object {
        private var instance: ProviderDbHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): ProviderDbHelper {
            if (instance == null) {
                instance = ProviderDbHelper(ctx)
            }

            return instance!!
        }
    }

    fun insert(provider: ProviderContract.Provider) {
        instance?.use {
            insert(ProviderContract.TABLE_NAME,
                    ProviderContract.COLUMN_NAME to provider.name,
                    ProviderContract.COLUMN_NIT_CEDULA to provider.nit,
                    ProviderContract.COLUMN_EMAIL to provider.email,
                    ProviderContract.COLUMN_DIRECTION to provider.direction,
                    ProviderContract.COLUMN_PHONE_NUMBER to provider.phoneNumber)
        }
    }

    fun update(id: Long, provider: ProviderContract.Provider) {
        instance?.use{
            update(ProviderContract.TABLE_NAME,
                    ProviderContract.COLUMN_NAME to provider.name,
                    ProviderContract.COLUMN_NIT_CEDULA to provider.nit,
                    ProviderContract.COLUMN_EMAIL to provider.email,
                    ProviderContract.COLUMN_DIRECTION to provider.direction,
                    ProviderContract.COLUMN_PHONE_NUMBER to provider.phoneNumber)
                    .whereSimple("${ProviderContract._ID} = ?", "$id")
                    .exec()
        }
    }

    fun getAll() : List<ProviderContract.Provider>? {
        return instance?.use {
            val rowParser = classParser<ProviderContract.Provider>()
            val providers = select(ProviderContract.TABLE_NAME).parseList(rowParser)

            return@use providers
        }
    }

    fun get(id: Long) : ProviderContract.Provider? {
        return instance?.use {
            val rowParser = classParser<ProviderContract.Provider>()
            val provider = select(ProviderContract.TABLE_NAME)
                    .whereSimple("${ProviderContract._ID} = ?", "$id")
                    .parseSingle(rowParser)

            return@use provider
        }
    }
}