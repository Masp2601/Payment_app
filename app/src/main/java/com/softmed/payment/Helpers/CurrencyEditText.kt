package com.softmed.payment.helpers

import android.content.Context
import android.graphics.Rect
import android.preference.PreferenceManager
import androidx.appcompat.widget.AppCompatEditText
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText
import com.softmed.payment.R
import org.jetbrains.anko.AnkoLogger
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.text.NumberFormat
import kotlin.math.pow

class CurrencyEditText : AppCompatEditText, AnkoLogger {

    private val mTextWatcher = MoneyFormatterWatcherHelper(this)
    private val replaceable = String.format(
            "[%s,.\\s]",
            getCurrencySymbol())

    private val fraction : Int
        get() {
            val value = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_key_currency_digits),
                            context.getString(R.string.pref_default_currency_digits))
            return value?.toInt() ?: 2
        }

    private val scale
        get() = 10.0.pow(fraction.toDouble()).toInt()

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {

    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, androidx.appcompat.R.attr.editTextStyle) {}

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            this.addTextChangedListener(mTextWatcher)
        }
        else {
            this.removeTextChangedListener(mTextWatcher)
        }
    }

    val value: String get() {
        val formatted = this.text.toString()
        val clean = formatted.replace(replaceable.toRegex(), "")
        val parsed = BigDecimal(clean).setScale(fraction, BigDecimal.ROUND_FLOOR).divide(BigDecimal(scale), BigDecimal.ROUND_FLOOR)

        return parsed.toString()
    }

    fun setText(value: Double?) {
        val formatted = getCurrencyFormat().format(value ?: 0)
        super.setText(formatted)
    }

    private fun getCurrencyFormat(): NumberFormat {
        val format = NumberFormat.getCurrencyInstance()
        format.minimumFractionDigits = fraction
        format.maximumFractionDigits = fraction
        return format;
    }

    private fun getCurrencySymbol(): String = getCurrencyFormat().currency.symbol

    private fun getCursorPosition(formatted: String): Int =
            if (formatted.indexOf(getCurrencySymbol()) == 0) formatted.length
            else formatted.length - getCurrencySymbol().length - 1

    private inner class MoneyFormatterWatcherHelper(editText: EditText): TextWatcher {
        private var editTextWeakReference: WeakReference<EditText> = WeakReference(editText)

        override fun afterTextChanged(edit: Editable) {
            try {
                val editText = editTextWeakReference.get() ?: return
                editText.removeTextChangedListener(this)

                val cleanString = edit.toString().replace(replaceable.toRegex(), "")
                val toParse = if (cleanString.isEmpty()) "0" else cleanString
                val parsed = BigDecimal(toParse).setScale(fraction, BigDecimal.ROUND_FLOOR).divide(BigDecimal(scale), BigDecimal.ROUND_FLOOR)
                val currencyFormat = getCurrencyFormat()
                val formatted = currencyFormat.format(parsed)
                editText.setText(formatted)


                editText.setSelection(getCursorPosition(formatted))
                editText.addTextChangedListener(this)
            }
            catch (e: Exception) {
                kotlin.error(e)
            }
        }

        override fun beforeTextChanged(p0: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, start: Int, before: Int, count: Int) {

        }

    }

}