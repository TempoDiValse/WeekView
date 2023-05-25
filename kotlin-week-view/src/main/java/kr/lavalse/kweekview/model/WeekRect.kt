package kr.lavalse.kweekview.model

import android.graphics.Rect
import android.graphics.RectF

class WeekRect(left: Float, top: Float, right: Float, bottom: Float, event: WeekEvent)
    : RectF(left, top, right, bottom) {

    private var _event: WeekEvent = event

    private val absRect : RectF = RectF()
    val absoluteRect get() = absRect

    val event: WeekEvent get() = _event
    val originalEvent: WeekEvent get() = _event.originalEvent

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