package kr.lavalse.kweekview.extension

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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

    fun LocalDateTime.visibleDateFrom(days: Int)
        = with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            .minusDays(days * 1L)
    fun LocalDateTime.withDayOfWeek(dayOfWeek: DayOfWeek) = with(TemporalAdjusters.previousOrSame(dayOfWeek))
    fun LocalDateTime.toMinuteOfHours() = (hour * 60) + minute

    fun LocalDateTime.withTime(hour: Int, minute: Int = 0, second: Int = 0): LocalDateTime =
        if(hour > 23){
            val _hour = hour - 23

            with(LocalTime.of(23, minute, second)).plusHours(_hour * 1L)
        }else{
            with(LocalTime.of(hour, minute, second))
        }


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

object ECanvas {
    fun Canvas.drawHalfRoundRect(l: Float, t: Float, r: Float, b: Float, roundness: Float, paint: Paint, isLeftTop: Boolean = true){
        val corner =
            if(isLeftTop) floatArrayOf(roundness, roundness, 0f, 0f, 0f, 0f, roundness, roundness)
            else floatArrayOf(0f, 0f, roundness, roundness, roundness, roundness, 0f, 0f)

        val path = Path().apply {
            addRoundRect(l, t, r, b, corner, Path.Direction.CW)
        }

        drawPath(path, paint)
    }
}