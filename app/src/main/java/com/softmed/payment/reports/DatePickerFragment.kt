package com.softmed.payment.reports

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.os.Bundle
import android.widget.DatePicker
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by nelso on 9/28/2017.
 */
class DatePickerFragment: DialogFragment(), DatePickerDialog.OnDateSetListener, AnkoLogger {
    interface DatePickerListener {
        fun onDateSet(year: Int, month: Int, day: Int)
    }

    private var listener: DatePickerListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        val year = arguments.getInt(ARG_YEAR, calendar.get(Calendar.YEAR))
        val month = arguments.getInt(ARG_MONTH, calendar.get(Calendar.MONTH))
        val day = arguments.getInt(ARG_DAY, calendar.get(Calendar.DAY_OF_MONTH))

        return DatePickerDialog(activity, this, year, month, day)
    }

    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, day: Int) {
        listener?.onDateSet(year, month, day)
    }

    fun setOnDataSetListener(listener: DatePickerListener) {
        this.listener = listener
    }

    companion object {
        private val ARG_YEAR = "year"
        private val ARG_MONTH = "month"
        private val ARG_DAY = "day"

        fun getInstance(year: Int?, month: Int?, day: Int?): DatePickerFragment {
            val frag = DatePickerFragment()
            val calendar = Calendar.getInstance()
            val args = Bundle()
            args.putInt(ARG_YEAR, year ?: calendar.get(Calendar.YEAR))
            args.putInt(ARG_MONTH, month ?: calendar.get(Calendar.MONTH))
            args.putInt(ARG_DAY, day ?: calendar.get(Calendar.DAY_OF_MONTH))
            frag.arguments = args

            return frag
        }
    }
}