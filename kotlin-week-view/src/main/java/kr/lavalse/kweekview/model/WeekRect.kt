package kr.lavalse.kweekview.model

import android.graphics.RectF

class WeekRect : RectF {
    private var _event: WeekEvent

    constructor(left: Float, top: Float, right: Float, bottom: Float, event: WeekEvent) : super(left, top, right, bottom){
        this._event = event
    }

    val event: WeekEvent get() = _event

    val startAt get() = event.startAt!!
    val endAt get() = event.endAt!!

    val startTimeInMillis get () = event.startTimeInMillis
    val endTimeInMillis get () = event.endTimeInMillis
    val difference get () = event.difference

    val backgroundColor get() = event.backgroundColor
}