package com.softmed.payment.storage

import android.content.Context
import com.softmed.payment.helpers.DateTimeHelper
import org.jetbrains.anko.db.*
import org.jetbrains.anko.info
import java.util.*

class TransactionDbHelper(ctx: Context) : BaseDBHelper(ctx) {
    companion object {
        private var instance: TransactionDbHelper? = null

        fun getInstance(ctx: Context): TransactionDbHelper {
            if (instance == null){
                instance = TransactionDbHelper(ctx)
            }
            return instance!!
        }
    }

    fun insert(transaction: TransactionContract.Transaction) {
        instance?.use {
            insert(TransactionContract.TABLE_NAME,
                    TransactionContract.COLUMN_INVOICE_NUMBER to transaction.invoiceNumber,
                    TransactionContract.COLUMN_INVOICE_TOTAL to transaction.invoiceTotal,
                    TransactionContract.COLUMN_PAYMENT_TYPE to transaction.paymentType,
                    TransactionContract.COLUMN_PAYMENT_TOTAL to transaction.paymentTotal,
                    TransactionContract.COLUMN_PAYMENT_CARD_REFERENCE_NUMBER to transaction.paymentCardReference,
                    TransactionContract.COLUMN_PAYMENT_CREDIT_DEPOSIT to transaction.paymentCreditDeposit,
                    TransactionContract.COLUMN_PAYMENT_DATE to transaction.paymentDate,
                    TransactionContract.COLUMN_PAYMENT_DATETIME to transaction.paymentDateTime,
                    TransactionContract.COLUMN_PAYMENT_CONTROL_TIME to transaction.paymentControlTime,
                    TransactionContract.COLUMN_PAYMENT_CHECK_NUMBER to transaction.paymentCheckNumber,
                    TransactionContract.COLUMN_PAYMENT_CHECK_BANK_NAME to transaction.paymentCheckBankName)
        }
    }

    fun update(transaction: TransactionContract.Transaction) {
        instance?.use {
            update(TransactionContract.TABLE_NAME,
                    TransactionContract.COLUMN_PAYMENT_TOTAL to transaction.paymentTotal,
                    TransactionContract.COLUMN_PAYMENT_DATE to transaction.paymentDate,
                    TransactionContract.COLUMN_PAYMENT_DATETIME to transaction.paymentDateTime)
                    .whereSimple("${TransactionContract._ID} = ?",
                            "${transaction.id}")
                    .exec()
        }
    }

    fun processCreditPayment(transaction: TransactionContract.Transaction) {
        instance?.use {
            val reference = select(TransactionContract.TABLE_NAME)
                    .whereSimple("${TransactionContract.COLUMN_INVOICE_NUMBER} = ? " +
                            "and ${TransactionContract.COLUMN_PAYMENT_CONTROL_TIME} = ?",
                            "${transaction.invoiceNumber}",
                            "${transaction.paymentControlTime}"
                    )
                    .parseOpt(classParser<TransactionContract.Transaction>())

            if (reference == null){
                insert(transaction)
            } else {
                val paymentUpdated = transaction.paymentTotal + reference.paymentTotal
                update(reference.copy(paymentTotal = paymentUpdated))
            }
        }
    }

    fun getAll(): List<TransactionContract.Transaction>? {
        return instance?.use {
            val rowParser = classParser<TransactionContract.Transaction>()
            val payments = select(TransactionContract.TABLE_NAME).parseList(rowParser)

            return@use payments
        }
    }

    fun getTransaction(invoiceNumber: Long): TransactionContract.Transaction? {
        return instance?.use {
            val rowParser = classParser<TransactionContract.Transaction>()
            val transaction = select(TransactionContract.TABLE_NAME).whereSimple("${TransactionContract.COLUMN_INVOICE_NUMBER} = ?", "$invoiceNumber").parseList(rowParser)

            if (transaction.isNotEmpty()){
                return@use transaction[0]
            }

            return@use null
        }
    }

    fun getPayments(calendar: Calendar): List<TransactionContract.Transaction>? {
        return instance?.use {
            val startOfTheDay = DateTimeHelper.getStarOfDay(calendar)
            val endOfTheDay = DateTimeHelper.getEndDate(calendar)
            val query = "SELECT " +
                    "TC.${TransactionContract._ID}, " +
                    "TC.${TransactionContract.COLUMN_INVOICE_NUMBER}, " +
                    "TC.${TransactionContract.COLUMN_INVOICE_TOTAL}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_TYPE}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_TOTAL}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_CARD_REFERENCE_NUMBER}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_CREDIT_DEPOSIT}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_DATE}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_DATETIME}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_CONTROL_TIME}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_CHECK_NUMBER}, " +
                    "TC.${TransactionContract.COLUMN_PAYMENT_CHECK_BANK_NAME} " +
                    "FROM ${TransactionContract.TABLE_NAME} TC " +
                    "INNER JOIN ${InvoiceContract.TABLE_NAME} IC ON IC.${InvoiceContract.COLUMN_INVOICE_NUMBER} = TC.${InvoiceContract.COLUMN_INVOICE_NUMBER} " +
                    "WHERE TC.${TransactionContract.COLUMN_PAYMENT_CONTROL_TIME} >= ? " +
                    "AND TC.${TransactionContract.COLUMN_PAYMENT_CONTROL_TIME} < ? " +
                    "AND IC.${InvoiceContract.COLUMN_NULLIFY} = 0"
            
            val transactionList: MutableList<TransactionContract.Transaction> = mutableListOf()
            val result = rawQuery(query, arrayOf("${startOfTheDay.time.time}", "${endOfTheDay.time.time}"))
            result.asSequence().forEach {
                transactionList.add(TransactionContract.Transaction(
                        id = it[0] as Long,
                        invoiceNumber = it[1] as Long,
                        invoiceTotal = it[2] as Double,
                        paymentType = (it[3] as Long).toInt(),
                        paymentTotal = it[4] as Double,
                        paymentCardReference = it[5] as String,
                        paymentCreditDeposit = it[6] as Double,
                        paymentDate = it[7] as String,
                        paymentDateTime = it[8] as Long,
                        paymentControlTime = it[9] as Long,
                        paymentCheckNumber = it[10] as String,
                        paymentCheckBankName = it[11] as String
                ))
            }
            result.close()

            return@use transactionList
        }
    }

    fun getPaymentTotalDay(date: Long): Double {
        return instance?.use {

            var total = 0.0

            val query = "SELECT " +
                    "sum(TC.${TransactionContract.COLUMN_PAYMENT_TOTAL}) " +
                    "FROM ${TransactionContract.TABLE_NAME} TC " +
                    "INNER JOIN ${InvoiceContract.TABLE_NAME} IC " +
                    "ON IC.${InvoiceContract.COLUMN_INVOICE_NUMBER} = TC.${TransactionContract.COLUMN_INVOICE_NUMBER} " +
                    "WHERE IC.${InvoiceContract.COLUMN_NULLIFY} = 0 " +
                    "AND TC.${TransactionContract.COLUMN_PAYMENT_CONTROL_TIME} = ?"

            val result = rawQuery(query, arrayOf("$date"))
            result.asSequence().forEach {
                total = if (it[0] == null) 0.0 else it[0] as Double
            }
            result.close()

            return@use total
        } ?: 0.0
    }

    fun getCreditDepositTotalDay(date: Long): Double {
        return instance?.use {

            var total = 0.0

            val query = "SELECT " +
                    "sum(TC.${TransactionContract.COLUMN_PAYMENT_TOTAL}) " +
                    "FROM ${TransactionContract.TABLE_NAME} TC " +
                    "INNER JOIN ${InvoiceContract.TABLE_NAME} IC " +
                    "ON IC.${InvoiceContract.COLUMN_INVOICE_NUMBER} = TC.${TransactionContract.COLUMN_INVOICE_NUMBER} " +
                    "WHERE IC.${InvoiceContract.COLUMN_NULLIFY} = 0 " +
                    "AND TC.${TransactionContract.COLUMN_PAYMENT_CONTROL_TIME} = ? " +
                    "AND IC.${InvoiceContract.COLUMN_IS_CREDIT} = 1"

            val result = rawQuery(query, arrayOf("$date"))
            result.asSequence().forEach {
                total = if (it[0] == null) 0.0 else it[0] as Double
            }
            result.close()

            return@use total
        } ?: 0.0
    }
}