package kr.lavalse.kweekview.model

import android.graphics.Rect
import android.graphics.RectF

class WeekRect : RectF {
    private var _event: WeekEvent
    private var _origin: WeekEvent

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