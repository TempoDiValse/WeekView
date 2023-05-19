package kr.lavalse.kweekview.model

import android.graphics.Rect
import android.graphics.RectF

class WeekRect : RectF {
    companion object {
        const val TYPE_SINGLE = -1
        const val TYPE_FRONT = 0
        const val TYPE_CENTER = 1
        const val TYPE_REAR = 2
    }

    private var _event: WeekEvent
    private var _origin: WeekEvent
    private var _type: Int = TYPE_SINGLE

    private val absRect : RectF = RectF()
    val absoluteRect get() = absRect

    constructor(left: Float, top: Float, right: Float, bottom: Float, event: WeekEvent, origin: WeekEvent): super(left, top, right, bottom){
        this._event = event
        this._origin = origin
    }

    constructor(left: Float, top: Float, right: Float, bottom: Float, event: WeekEvent)
        : this(left, top, right, bottom, event, event)

    val event: WeekEvent get() = _event
    val originalEvent: WeekEvent get() = _origin
    var rectType
        get() = _type
        set(value) {
            _type = if(!arrayOf(TYPE_SINGLE, TYPE_FRONT, TYPE_CENTER, TYPE_REAR).contains(value)) TYPE_SINGLE
                    else value
        }

    val startAt get() = event.startAt!!
    val endAt get() = event.endAt!!

    val startTimeInMillis get () = event.startTimeInMillis
    val endTimeInMillis get () = event.endTimeInMillis
    val difference get () = event.difference

    val backgroundColor get() = event.backgroundColor

    fun setAbsoluteRect(l: Float, t: Float, r: Float, b: Float, padding: Float){
        absRect.set(l + padding, t + padding, r - padding, b - padding)
    }

    fun containsTouchPoint(x: Float, y: Float) = absoluteRect.contains(x, y)

    override fun toString(): String {
        return "$_event L: $left T: $top R: $right B: $bottom"
    }
}