package kr.lavalse.kweekview.model

class DummyWeekEvent : WeekEvent() {
    companion object {
        private const val DUMMY_EVENT_COLOR = "#FA614F"
    }

    init {
        id = "-1"
        title = "대면보고"

        setBackgroundColor(DUMMY_EVENT_COLOR)
    }
}