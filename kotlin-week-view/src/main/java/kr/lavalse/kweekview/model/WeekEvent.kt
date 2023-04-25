package kr.lavalse.kweekview.model

import android.graphics.Color
import kr.lavalse.kweekview.extension.ECalendar.formattedText
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

open class WeekEvent {
    companion object {
        protected const val DEFAULT_COLOR = "#B2E1FF"

        fun copyDescription(from: WeekEvent) = WeekEvent().apply {
            id = from.id
            title = from.title
            creator = from.creator
            duty = from.duty
            inviterType = from.inviterType

            isAllDay = from.isAllDay
            _color = from._color
        }
    }

    private val oid = UUID.randomUUID().toString()
    val objectId get() = oid

    var id: String = ""

    var startAt : Calendar? = null
    var endAt : Calendar? = null

    val startTimeInMillis get() = startAt?.timeInMillis ?: 0L
    val endTimeInMillis get() = endAt?.timeInMillis ?: 0L

    val difference get() = abs(endTimeInMillis - startTimeInMillis)

    open var title: String = ""

    private var isAllDay: Boolean = false

    private var _color: String = DEFAULT_COLOR
    val backgroundColor get() = Color.parseColor(_color)

    var creator: String = ""
    var duty: String = ""

    private var canDelete: Boolean = true

    private var canTransformResource: Boolean = false
    var statusResource: String = ""

    private var isInvited = false
    var inviterType: String = ""

    fun isAllDay() = isAllDay

    fun canDelete() = canDelete
    fun canTransformResource() = canTransformResource

    fun isInvited() = isInvited

    fun setBackgroundColor(code: String){ this._color = code }
    fun setStartAndEndDate(startAt: Calendar, endAt: Calendar, isAllDay: Boolean = false){
        this.isAllDay = isAllDay

        if(isAllDay){
            with(startAt){
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }

            with(endAt){
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
        }

        startAt.set(Calendar.SECOND, 0)
        endAt.set(Calendar.SECOND, 0)

        this.startAt = startAt
        this.endAt = endAt
    }

    override fun equals(other: Any?): Boolean = other?.let { it as WeekEvent; it.id == id } ?: false
    override fun toString(): String {
        val start = startAt?.formattedText("yyyy/MM/dd HH:mm") ?: "NULL"
        val end = endAt?.formattedText("yyyy/MM/dd HH:mm") ?: "NULL"

        return if(!isAllDay()) "($id) $start ~ $end" else "($id-A) $start ~ $end"
    }


}