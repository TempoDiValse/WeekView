package kr.lavalse.kweekview.model

import android.graphics.Color
import kr.lavalse.kweekview.extension.ELocalDateTime.withTime
import kr.lavalse.kweekview.extension.ELocalDateTime.toLocalDateTime
import kr.lavalse.kweekview.extension.ELocalDateTime.toText
import kr.lavalse.kweekview.extension.ELocalDateTime.toTimeMillis
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

open class WeekEvent: Cloneable {
    var id: String = ""

    var startAt : LocalDateTime? = null
    var endAt : LocalDateTime? = null

    val startTimeInMillis get() = startAt?.toTimeMillis() ?: 0L
    val endTimeInMillis get() = endAt?.toTimeMillis() ?: 0L

    val difference get() = abs(endTimeInMillis - startTimeInMillis)
    val days get() = (difference / (24 * 60 * 60 * 1000f)).roundToInt()

    open var title: String = ""

    private var isAllDay: Boolean = false

    private var _color: Int = Color.BLACK

    val backgroundColor get() = _color

    /**
     * #DCDCDC 보다 밝은 경우에는 STROKE 값을 전달 해준다. 어두운 경우에는 -1을 전달 한다
     */
    val strokeColor get() = if(backgroundColor > 0xFFDCDCDC.toInt()) 0xFF999999.toInt() else -1
    val isBrightColor get() = strokeColor != -1

    fun isAllDay() = isAllDay
    fun setBackgroundColor(code: String){ this._color = Color.parseColor(code) }
    fun setBackgroundColor(value: Int){ this._color = value }

    fun setStartAndEndDate(start: Long, end: Long){
        setStartAndEndDate(start.toLocalDateTime(), end.toLocalDateTime())
    }

    fun setStartAndEndDate(startAt: LocalDateTime, endAt: LocalDateTime, isAllDay: Boolean = false){
        this.isAllDay = isAllDay

        var (_start, _end) = startAt to endAt

        if(isAllDay){
            _start = _start.withTime(0)
            _end = _end.withTime(23, 59, 59)
        }else{
            _start = _start.withSecond(0)
            _end = _end.withSecond(0)
        }

        this.startAt = _start
        this.endAt = _end
    }

    override fun equals(other: Any?): Boolean = other?.let { it as WeekEvent
        it.id == id
            && it.startTimeInMillis == startTimeInMillis
            && it.endTimeInMillis == endTimeInMillis
    } ?: false

    override fun toString(): String {
        val start = startAt?.toText("yyyy/MM/dd HH:mm") ?: "NULL"
        val end = endAt?.toText("yyyy/MM/dd HH:mm") ?: "NULL"

        return if(!isAllDay()) "($id) $start ~ $end" else "($id-A) $start ~ $end"
    }

    public override fun clone(): WeekEvent
        = (super.clone() as WeekEvent).also { c ->
            c.id = id
            c.title = title

            c.isAllDay = isAllDay
            c._color = _color
        }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (startAt?.hashCode() ?: 0)
        result = 31 * result + (endAt?.hashCode() ?: 0)
        result = 31 * result + title.hashCode()
        result = 31 * result + isAllDay.hashCode()
        result = 31 * result + _color
        return result
    }
}