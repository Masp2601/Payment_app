package com.softmed.payment.helpers

import android.content.Context
import android.preference.PreferenceManager
import androidx.appcompat.widget.AppCompatTextView
import android.util.AttributeSet
import com.softmed.payment.R
import java.math.BigDecimal
import java.text.NumberFormat

class CurrencyTextView: AppCompatTextView {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(context, attributeSet, defStyle)

    private val fraction : Int
        get() {
            val value = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_key_currency_digits),
                            context.getString(R.string.pref_default_currency_digits))
            return value?.toInt() ?: 2
        }

    private val scale
        get() = Math.pow(10.0, fraction.toDouble()).toInt()

    override fun setText(text: CharSequence?, type: BufferType?) {
        try {
            val parsed = BigDecimal(text.toString()).setScale(fraction, BigDecimal.ROUND_FLOOR)
            val formatted = getCurrencyFormat().format(parsed)

            super.setText(formatted, type)
        } catch(e: Exception) {
            super.setText(text, type)
        }
    }

    fun setText(value: Double) {
        val formatted = getCurrencyFormat().format(value)
        super.setText(formatted)
    }

    fun setText(value: Float) {
        val formatted = getCurrencyFormat().format(value)
        super.setText(formatted)
    }

    private fun getCurrencyFormat(): NumberFormat {
        val format = NumberFormat.getCurrencyInstance()
        format.minimumFractionDigits = fraction
        format.maximumFractionDigits = fraction
        return format;
    }
}