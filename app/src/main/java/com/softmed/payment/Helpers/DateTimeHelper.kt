package com.softmed.payment.helpers

import java.text.SimpleDateFormat
import java.util.*

class DateTimeHelper {
    companion object {
        const val PATTERN_SHORT = "yyyy.MM.dd"
        const val PATTERN_LONG = "dd MMMM yyyy"
        const val PATTERN_FILE = "yyyy_MM_dd"
        const val PATTERN_REQUEST_RESPONSE = "yyyy-MM-dd'T'HH:mm:ss"
        const val PATTERN_BIRTHDAY_MONTH_DAY = "MM.dd"
        const val PATTERN_BIRTHDAY_DAY_MONTH = "dd/MM"
        const val PATTERN_BIRTHDAY_DAY_MONTH_YEAR = "dd/MM/yyyy"

        fun dateToString(date: Date, pattern: String = PATTERN_SHORT): String = SimpleDateFormat(pattern, Locale.getDefault()).format(date)

        fun dateToString(date: Long, pattern: String = PATTERN_SHORT): String {
            return dateToString(Date(date), pattern)
        }

        fun parseStringToDate(string: String, pattern: String = PATTERN_SHORT): Date = SimpleDateFormat(pattern, Locale.getDefault()).parse(string)

        fun getStarOfDay(calendar: Calendar): Calendar {
            val start = calendar.clone() as Calendar
            start[Calendar.HOUR] = 0
            start[Calendar.MINUTE] = 0
            start[Calendar.SECOND] = 0
            start[Calendar.MILLISECOND] = 0

            return start
        }

        fun getEndDate(calendar: Calendar): Calendar {
            // La fecha del calendario tiene hora 00:00:00, se debe agregar un día a la fecha
            // final o poner la hora en 23:59:59. En este caso agregamos un día.
            val end: Calendar = getStarOfDay(calendar)
            end.add(Calendar.DAY_OF_MONTH, 1)

            return end
        }

        fun secondsUntilTomorrow(now: Calendar): Long {
            val tomorrow = getStarOfDay(now)
            tomorrow.add(Calendar.DAY_OF_MONTH, 1)

            val untilInMillis = tomorrow.time.time - now.time.time

            return untilInMillis / 1000
        }

        fun getStartOfTheWeek(): Calendar {
            val calendar = Calendar.getInstance()
            calendar[Calendar.DAY_OF_WEEK] = calendar.firstDayOfWeek

            return getStarOfDay(calendar)
        }

        fun getEndOfTheWeek(): Calendar {
            val calendar = getStartOfTheWeek()
            calendar.add(Calendar.WEEK_OF_YEAR, 1)

            return calendar
        }
    }
}