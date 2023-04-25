package kr.lavalse.kweekview.extension

import java.text.SimpleDateFormat
import java.util.*

object ECalendar {
    fun Calendar.formattedText(format: String = "HH:mm") : String
        = SimpleDateFormat(format, Locale.getDefault()).format(time)

    fun Calendar.isSameDay(with: Calendar)
        = isSameYear(with) && get(Calendar.DAY_OF_YEAR) == with.get(Calendar.DAY_OF_YEAR)

    fun Calendar.isPastDay(date: Calendar)
        = isSameYear(date) && date.get(Calendar.DAY_OF_YEAR) > get(Calendar.DAY_OF_YEAR)

    fun Calendar.isSameWeek(with: Calendar)
        = isSameYear(with) && get(Calendar.WEEK_OF_YEAR) == with.get(Calendar.WEEK_OF_YEAR)

    fun Calendar.isSameYear(with: Calendar)
        = get(Calendar.YEAR) == with.get(Calendar.YEAR)

    fun Calendar.toMinute() = get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
}