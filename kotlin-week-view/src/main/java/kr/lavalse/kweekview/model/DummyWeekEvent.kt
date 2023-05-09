package kr.lavalse.kweekview.model

import kr.lavalse.kweekview.extension.ELocalDateTime.toText

class DummyWeekEvent : WeekEvent("-1") {
    override fun toString(): String
        = "Dummy ${startAt!!.toText("yyyy/MM/dd HH:mm")} ~ ${endAt!!.toText("yyyy/MM/dd HH:mm")}"

    override fun equals(other: Any?): Boolean
        = other?.let { it as DummyWeekEvent; it.startAt == startAt && it.endAt == endAt } ?: false

    override fun clone(): DummyWeekEvent
        = (super.clone() as DummyWeekEvent).also { c ->
            c.startAt = startAt
            c.endAt = endAt
            c.title = title

            c.setBackgroundColor(backgroundColor)
        }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}