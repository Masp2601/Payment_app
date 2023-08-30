package com.softmed.payment.helpers

import android.text.Editable
import java.math.BigDecimal
import java.text.NumberFormat


class CurrencyHelper {
    companion object {
        private val replaceable = String.format(
                "[%s,.\\s]",
                NumberFormat.getCurrencyInstance().currency.symbol)

        @JvmStatic
        fun toDoubleOrNull(text: String): Double? {
            return try {
                val clean = text.replace(replaceable.toRegex(), "")
                val parsed = BigDecimal(clean).setScale(2, BigDecimal.ROUND_FLOOR).divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)

                parsed.toDouble()
            } catch (e: Exception) {
                null
            }
        }

        @JvmStatic
        fun toDoubleOrNull(text: Editable): Double? = toDoubleOrNull(text.toString())

        @JvmStatic
        fun toDoubleOrNull(text: CharSequence): Double? = toDoubleOrNull(text.toString())

        @JvmStatic
        fun cleanFormat(text: String) : String {
            return try {
                val clean = text.replace(replaceable.toRegex(), "")
                val parsed = BigDecimal(clean).setScale(2, BigDecimal.ROUND_FLOOR).divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)

                 parsed.toString()
            } catch (e: Exception) {
                ""
            }
        }
    }
}