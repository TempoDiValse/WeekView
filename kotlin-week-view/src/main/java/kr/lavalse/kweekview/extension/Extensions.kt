package kr.lavalse.kweekview.extension

import android.content.Context
import android.util.TypedValue
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

object ELocalDateTime {
    fun LocalDateTime.isSameYear(with: LocalDateTime) = year == with.year
    fun LocalDateTime.isSameWeek(with: LocalDateTime)
            = isSameYear(with) && weekOfYear() == with.weekOfYear()
    fun LocalDateTime.isSameDay(with: LocalDateTime) = isSameYear(with) && dayOfYear == with.dayOfYear
    fun LocalDateTime.isBeforeDay(with: LocalDateTime) = isSameYear(with) && dayOfYear > with.dayOfYear
    fun LocalDateTime.toText(pattern: String = "yyyy/MM/dd"): String = format(DateTimeFormatter.ofPattern(pattern))
    fun LocalDateTime.weekOfYear() = get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
    fun LocalDateTime.toTimeMillis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun LocalDateTime.toMinuteOfHours() = (hour * 60) + minute

    fun LocalDateTime.withTime(hour: Int, minute: Int = 0, second: Int = 0): LocalDateTime =
        with(LocalTime.of(hour, minute, second))

    fun Long.toLocalDateTime(): LocalDateTime
        = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

object EContext {
    fun Context.applyDimension(unit: Int, value: Float)
            = TypedValue.applyDimension(unit, value, resources.displayMetrics)

    fun Context.toSP(value: Float) = applyDimension(TypedValue.COMPLEX_UNIT_SP, value)
    fun Context.toSP(value: Int) = toSP(value.toFloat())

    fun Context.toDP(value: Float) = applyDimension(TypedValue.COMPLEX_UNIT_DIP, value)
    fun Context.toDP(value: Int) = toDP(value.toFloat())
}