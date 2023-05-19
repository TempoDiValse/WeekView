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

    constructor(id: String, start: LocalDateTime, end: LocalDateTime, isAllDay: Boolean){
        this.id = id

        setStartAndEndDate(start, end, isAllDay)
    }
    constructor(id: String, start: LocalDateTime, end: LocalDateTime) : this(id, start, end, false)

    /**
     * ID 값은 모델 이벤트와 매칭 될 수 있도록 꼭 입력을 해준다.
     *
     * @param id 모델 이벤트의 ID
     */
    constructor(id: String) : this(id, LocalDateTime.now(), LocalDateTime.now()){ this.id = id }

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

    override fun equals(other: Any?): Boolean = other?.let { it as WeekEvent; it.id == id } ?: false

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