package kr.lavalse.kweekview

import android.content.Context
import android.graphics.*
import android.text.*
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.*
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import kr.lavalse.kweekview.extension.ECanvas.drawHalfRoundRect
import kr.lavalse.kweekview.extension.EContext.toDP
import kr.lavalse.kweekview.extension.EContext.toSP
import kr.lavalse.kweekview.extension.ELocalDateTime.visibleDateFrom
import kr.lavalse.kweekview.extension.ELocalDateTime.isBeforeDay
import kr.lavalse.kweekview.extension.ELocalDateTime.isSameDay
import kr.lavalse.kweekview.extension.ELocalDateTime.isSameWeek
import kr.lavalse.kweekview.extension.ELocalDateTime.toLocalDateTime
import kr.lavalse.kweekview.extension.ELocalDateTime.withTime
import kr.lavalse.kweekview.extension.ELocalDateTime.toMinuteOfHours
import kr.lavalse.kweekview.extension.ELocalDateTime.toText
import kr.lavalse.kweekview.extension.ELocalDateTime.toTimeMillis
import kr.lavalse.kweekview.extension.ELocalDateTime.weekOfYear
import kr.lavalse.kweekview.extension.ELocalDateTime.withDayOfWeek
import kr.lavalse.kweekview.model.DummyWeekEvent
import kr.lavalse.kweekview.model.WeekEvent
import kr.lavalse.kweekview.model.WeekRect
import java.lang.Float.max
import java.lang.Float.min
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class WeekView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DEFAULT_VISIBLE_DAYS = 7
        private const val DEFAULT_PRELOAD_WEEK_DATA_RANGE = 1

        private const val DEFAULT_TEXT_SIZE = 12f

        private const val DEFAULT_GRID_SEPARATOR_COLOR = Color.LTGRAY
        private const val DEFAULT_GRID_SEPARATOR_STROKE_WIDTH = .3f

        private const val DEFAULT_HOUR_HEIGHT = 50f
        private const val DEFAULT_HOUR_HEIGHT_MIN = 30f
        private const val DEFAULT_HOUR_HEIGHT_MAX = 70f

        private const val DEFAULT_TIMELINE_HORIZONTAL_PADDING = 10f

        private const val DEFAULT_TIME_NOW_COLOR = 0x355AEF
        private const val DEFAULT_TIME_NOW_STROKE_WIDTH = .5f

        private const val DEFAULT_DAY_OF_WEEK_VERTICAL_PADDING = 20f
        private const val DEFAULT_DAY_OF_WEEK_TEXT_COLOR = 0xFF333333
        private const val DEFAULT_DAY_OF_WEEK_TODAY_TEXT_COLOR = 0xFF55AAFF
        private const val DEFAULT_DAY_OF_WEEK_SUNDAY_TEXT_COLOR = 0xFFFF0011
        private const val DEFAULT_DAY_OF_WEEK_SATURDAY_TEXT_COLOR = 0xFF4682F1

        private const val DEFAULT_ALLDAY_TEXT = "종일"
        private const val DEFAULT_ALLDAY_AREA_HEIGHT = 50f

        private const val DEFAULT_DAY_BACKGROUND_COLOR = Color.WHITE
        private const val DEFAULT_TODAY_BACKGROUND_COLOR = 0xFFC7F1FF
        private const val DEFAULT_PAST_EVENT_COLOR = Color.LTGRAY

        private const val DEFAULT_EVENT_CORNER_RADIUS = 0f
        private const val DEFAULT_EVENT_TITLE_PADDING = 10f
        private const val DEFAULT_EVENT_TEXT_COLOR = Color.BLACK
        private const val DEFAULT_EVENT_TEXT_MAX_LINES = 2f
        private const val DEFAULT_TEMPORARY_EVENT_COLOR = 0xFFFA614F

        private const val DEFAULT_HIGHLIGHT_COLOR = 0xFF415AEF
        private const val DEFAULT_HIGHLIGHT_BACKGROUND_ALPHA = 150
        private const val DEFAULT_HIGHLIGHT_STROKE_WIDTH = 2f

        fun getDefaultDayOfWeekPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private object DayType {
        const val PAST = -1
        const val COMMON = 0
        const val TODAY = 1
    }

    private object Scroll {
        const val NONE = 0
        const val LEFT = 1
        const val RIGHT = 2
        const val VERTICAL = 3

        const val X_SPEED = .5f
        const val X_AUTO_SCROLL_DURATION = 250
    }

    private val today = LocalDateTime.now()
    private var current : LocalDateTime = today

    private val rects: MutableList<WeekRect> = mutableListOf()

    // x, y 좌표
    private val origin: PointF = PointF(0f, 0f)

    private var eventCornerRadius = 0f
    private var eventTitlePadding = 0f
    private var painter = Paint(Paint.ANTI_ALIAS_FLAG)

    private var pastEventColor = Color.WHITE

    private var editEventText = ""
    private var editEventColor = Color.WHITE

    private var gridSeparatorColor = Color.WHITE
    private var gridSeparatorStrokeWidth = .0f
    private val gridSeparatorPaint : Paint = Paint()

    private var widthPerDay: Int = 0
    private var visibleDays: Int = DEFAULT_VISIBLE_DAYS

    private var textSize = 0
    private var eventTextColor = 0
    private var eventTextSize = 0
    private var eventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var timelineTextSize = 0
    private var timelineWidth = 0
    private var hourHeight = 0
    private var hourMinHeight = 0
    private var hourMaxHeight = 0
    private var timelineTextWidth = 0
    private var timelineTextHeight = 0
    private var timelineHorizontalPadding = 0
    private val timelineTextPaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }

    private var timeNowColor = 0
    private var timeNowStrokeWidth = .0f
    private val timeNowPaint : Paint = Paint()

    private var dayOfWeekTextSize = 0
    private var dayOfWeekVerticalPadding = 0
    private var dayOfWeekTextHeight = 0
    private val dayOfWeekHeight get() = dayOfWeekTextHeight + dayOfWeekVerticalPadding
    private var dayOfWeekTextColor = 0
    private var dayOfWeekTodayTextColor = 0
    private var dayOfWeekSundayTextColor = 0
    private var dayOfWeekSaturdayTextColor = 0
    private val dayOfWeekTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekTodayTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekSundayTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekSaturdayTextPaint : Paint = getDefaultDayOfWeekPaint()

    private var allDayEventText = ""
    private var allDayTextWidth = 0
    private var allDayEventTextSize = 0
    private var allDayAreaHeight = 0

    private var dayBackgroundColor = 0
    private var dayBackgroundPaint = Paint()
    private var todayBackgroundColor = 0
    private var todayBackgroundPaint = Paint()

    private val headerHeight get() = dayOfWeekHeight + allDayAreaHeight

    private val highlightRect = Rect()
    private var highlightColor = Color.WHITE
    private var highlightPaint = Paint()
    private var highlightStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private var highlightStrokeWidth = 0

    private var scaledTouchSlop = 0
    private var minimumFlingVelocity = 0

    private var currentScrollDirection = Scroll.NONE
    private var currentFlingDirection = currentScrollDirection

    private var isEditMode = false
    private var isLongPressed = false
    private var isDrawn = false

    private var listener : OnWeekViewListener? = null

    private var editEvent : DummyWeekEvent? = null
    private var pEditEvent: DummyWeekEvent? = null

    private var scroller = OverScroller(context, FastOutLinearInInterpolator())
    private var detector: GestureDetectorCompat
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)

            scrollToNearestOrigin()

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if(isEditMode){
                clearEditMode()

                invalidate()
                return super.onSingleTapConfirmed(e)
            }

            if(rects.isNotEmpty()){
                val rRects = rects
                rRects.reverse()

                for(r in rRects){
                    if(r.containsTouchPoint(e.x, e.y)){
                        val event = r.originalEvent

                        with(event.startAt!!){
                            listener?.onWeekEventSelected(
                                year,
                                monthValue,
                                dayOfMonth,
                                weekOfYear(),
                                event)
                        }

                        playSoundEffect(SoundEffectConstants.CLICK)

                        return super.onSingleTapConfirmed(e)
                    }
                }
            }

            return super.onSingleTapConfirmed(e)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if(isZooming || isEditMode) return true

            val didScrollHorizontal = abs(dx) > abs(dy)

            /**
             공간 선택할 때 Drag 가 필요한 경우 주석을 해제하도록 한다.
             그렇지 않은 경우에는 1시간 단위로만 선택이 가능하다 (@see [onLongPress])

            if(isEditMode) {
                val times = mutableListOf<Long>().apply {
                    add(e1.run { getTimeFromPoint(x, y) })
                    add(e2.run { getTimeFromPoint(x, y) })
                }

                times.sort()

                val (start, end) = times.mapIndexed{ i, v -> adjustScheduleStartOrEnd(v, i == 0) }
                // 종료 시점이 시작 시점보다 작은경우 진행하지 않음
                if(start > end) return true

                editEvent!!.setStartAndEndDate(start, end)
                if(pEditEvent == editEvent)
                    return true

                val editEvents = splitEventPerDay(editEvent!!).toMutableList()
                removeOnlyDummyEvent()

                positioningEventRectOffset(editEvents)

                invalidate()

                pEditEvent = editEvent?.clone()

                return true
            }
            */

            when(currentScrollDirection){
                Scroll.NONE ->
                    currentScrollDirection = when(didScrollHorizontal){
                        true -> if(dx > 0) Scroll.LEFT else Scroll.RIGHT
                        else -> Scroll.VERTICAL
                    }

                Scroll.LEFT -> {
                    if(didScrollHorizontal && dx < -scaledTouchSlop)
                        currentScrollDirection = Scroll.RIGHT
                }

                Scroll.RIGHT -> {
                    if(didScrollHorizontal && dx > scaledTouchSlop)
                        currentScrollDirection = Scroll.LEFT
                }
            }

            when(currentScrollDirection){
                Scroll.LEFT, Scroll.RIGHT -> {
                    /*
                    origin.x -= dx * Scroll.X_SPEED

                    val day = ceil(origin.x / widthPerDay).toInt()
                    val firstVisibleDate = LocalDateTime.now().visibleDateFrom(day)

                    loadSchedules(firstVisibleDate.withDayOfWeek(DayOfWeek.SUNDAY))

                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                    */
                }

                Scroll.VERTICAL -> {
                    origin.y -= dy

                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
            }

            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if(isZooming) return true

            if(arrayOf(Scroll.LEFT, Scroll.RIGHT, Scroll.VERTICAL).contains(currentFlingDirection))
                return true

            scroller.forceFinished(true)
            currentFlingDirection = currentScrollDirection

            val minY = -((hourHeight * 24) + headerHeight - height)
            when(currentFlingDirection){
                /*
                Scroll.LEFT, Scroll.RIGHT ->
                    scroller.fling(origin.x.toInt(), origin.y.toInt(),
                        (vx * Scroll.X_SPEED).toInt(), 0,
                        Int.MIN_VALUE, Int.MAX_VALUE,
                        minY, 0)
                */

                Scroll.VERTICAL ->
                    scroller.fling(origin.x.toInt(), origin.y.toInt(),
                        0, vy.toInt(),
                        Int.MIN_VALUE, Int.MAX_VALUE,
                        minY, 0)
            }

            ViewCompat.postInvalidateOnAnimation(this@WeekView)

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if(e.y < headerHeight) return

            if(!isLongPressed)
                isLongPressed = true

            if(isEditMode){
                clearEditMode()

                invalidate()
            }

            isEditMode = true

            val start = adjustScheduleStartOrEnd(getTimeFromPoint(e.x, e.y), true)
            // 오늘 기준 이전일 이면 롱클릭 편집을 하지 못하도록 한다.
            if(today.isBeforeDay(start)){
                isEditMode = false

                return
            }

            val end = adjustScheduleStartOrEnd(getTimeFromPoint(e.x, e.y), false)

            showScheduleInsertion(start.toLocalDate())

            editEvent = DummyWeekEvent().apply {
                title = editEventText

                setBackgroundColor(editEventColor)
                setStartAndEndDate(start, end)
            }

            positioningTempEventRectOffset(listOf(editEvent!!))

            invalidate()
        }
    }

    private var isZooming = false
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            hourHeight = (hourHeight * detector.scaleFactor).roundToInt()
            hourHeight = hourHeight.coerceIn(hourMinHeight, hourMaxHeight)

            invalidate()

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isZooming = true
            scrollToNearestOrigin()

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) { isZooming = false }
    })

    private var didAttach = false

    init {
        with(context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, defStyleAttr, 0)){
            textSize = getDimensionPixelSize(R.styleable.WeekView_android_textSize,
                context.toSP(DEFAULT_TEXT_SIZE).toInt())

            // Event
            eventCornerRadius = getDimension(R.styleable.WeekView_cornerRadius,
                context.toDP(DEFAULT_EVENT_CORNER_RADIUS))
            eventTitlePadding = getDimension(R.styleable.WeekView_eventTitlePadding, context.toDP(
                DEFAULT_EVENT_TITLE_PADDING))
            eventTextColor = getColor(R.styleable.WeekView_eventTextColor, DEFAULT_EVENT_TEXT_COLOR)
            eventTextSize = getDimensionPixelSize(R.styleable.WeekView_eventTextSize, textSize)

            editEventColor = getColor(R.styleable.WeekView_editEventColor, DEFAULT_TEMPORARY_EVENT_COLOR.toInt())
            editEventText = getString(R.styleable.WeekView_editEventText) ?: ""

            pastEventColor = getColor(R.styleable.WeekView_pastEventColor, DEFAULT_PAST_EVENT_COLOR)

            // Timeline
            hourHeight = getDimensionPixelSize(R.styleable.WeekView_hourHeight,
                context.toDP(DEFAULT_HOUR_HEIGHT).toInt())
            hourMinHeight = getDimensionPixelSize(R.styleable.WeekView_hourMinHeight,
                context.toDP(DEFAULT_HOUR_HEIGHT_MIN).toInt())
            hourMaxHeight = getDimensionPixelSize(R.styleable.WeekView_hourMaxHeight,
                context.toDP(DEFAULT_HOUR_HEIGHT_MAX).toInt())
            timelineTextSize = getDimensionPixelSize(R.styleable.WeekView_timelineTextSize, textSize)
            timelineHorizontalPadding = getDimensionPixelSize(R.styleable.WeekView_timelineHorizontalPadding,
                context.toDP(DEFAULT_TIMELINE_HORIZONTAL_PADDING).toInt())

            timeNowColor = getColor(R.styleable.WeekView_timeNowColor, DEFAULT_TIME_NOW_COLOR)
            timeNowStrokeWidth = getDimension(R.styleable.WeekView_timeNowStrokeWidth,
                context.toDP(DEFAULT_TIME_NOW_STROKE_WIDTH))

            // dayOfWeek
            dayOfWeekVerticalPadding = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekVerticalPadding,
                context.toDP(DEFAULT_DAY_OF_WEEK_VERTICAL_PADDING).toInt())
            dayOfWeekTextSize = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekTextSize, textSize)
            dayOfWeekTextColor = getColor(R.styleable.WeekView_dayOfWeekTextColor, DEFAULT_DAY_OF_WEEK_TEXT_COLOR.toInt())
            dayOfWeekTodayTextColor = getColor(R.styleable.WeekView_dayOfWeekTodayTextColor, DEFAULT_DAY_OF_WEEK_TODAY_TEXT_COLOR.toInt())
            dayOfWeekSundayTextColor = getColor(R.styleable.WeekView_dayOfWeekSundayTextColor, DEFAULT_DAY_OF_WEEK_SUNDAY_TEXT_COLOR.toInt())
            dayOfWeekSaturdayTextColor = getColor(R.styleable.WeekView_dayOfWeekSaturdayTextColor, DEFAULT_DAY_OF_WEEK_SATURDAY_TEXT_COLOR.toInt())

            // AllDay
            allDayEventText = getString(R.styleable.WeekView_allDayEventText) ?: DEFAULT_ALLDAY_TEXT
            allDayEventTextSize = getDimensionPixelSize(R.styleable.WeekView_allDayEventTextSize, textSize)
            allDayAreaHeight = getDimensionPixelSize(R.styleable.WeekView_allDayAreaHeight,
                context.toDP(DEFAULT_ALLDAY_AREA_HEIGHT).toInt())

            // Day
            dayBackgroundColor = getColor(R.styleable.WeekView_dayBackgroundColor, DEFAULT_DAY_BACKGROUND_COLOR.toInt())
            todayBackgroundColor = getColor(R.styleable.WeekView_todayBackgroundColor, DEFAULT_TODAY_BACKGROUND_COLOR.toInt())

            gridSeparatorColor = getColor(R.styleable.WeekView_gridSeparatorColor, DEFAULT_GRID_SEPARATOR_COLOR)
            gridSeparatorStrokeWidth = getDimension(R.styleable.WeekView_gridSeparatorStrokeWidth,
                context.toDP(DEFAULT_GRID_SEPARATOR_STROKE_WIDTH))

            // Highlight
            highlightColor = getColor(R.styleable.WeekView_highlightColor, DEFAULT_HIGHLIGHT_COLOR.toInt())
            highlightStrokeWidth = getDimensionPixelSize(R.styleable.WeekView_highlightStrokeWidth, context.toDP(
                DEFAULT_HIGHLIGHT_STROKE_WIDTH).toInt())

            recycle()
        }

        detector = GestureDetectorCompat(context, gestureListener)

        ViewConfiguration.get(context).let { c ->
            scaledTouchSlop = c.scaledTouchSlop
            minimumFlingVelocity = c.scaledMinimumFlingVelocity
        }

        initialize()
    }

    private fun initialize(){
        gridSeparatorPaint.let { p ->
            p.color = gridSeparatorColor
            p.strokeWidth = gridSeparatorStrokeWidth
        }

        dayOfWeekTextPaint.let { p ->
            p.textSize = dayOfWeekTextSize.toFloat()
            p.color = dayOfWeekTextColor
        }

        dayOfWeekTodayTextPaint.let { p ->
            p.textSize = dayOfWeekTextSize.toFloat()
            p.color = dayOfWeekTodayTextColor
        }

        dayOfWeekSundayTextPaint.let { p ->
            p.textSize = dayOfWeekTextSize.toFloat()
            p.color = dayOfWeekSundayTextColor
        }

        dayOfWeekSaturdayTextPaint.let { p ->
            p.textSize = dayOfWeekTextSize.toFloat()
            p.color = dayOfWeekSaturdayTextColor
        }

        timeNowPaint.let { p ->
            p.strokeWidth = timeNowStrokeWidth
            p.color = Color.BLACK
        }

        eventTextPaint.let { p ->
            p.typeface = Typeface.DEFAULT
            p.textSize = eventTextSize.toFloat()
            p.color = eventTextColor
        }

        highlightPaint.color = (DEFAULT_HIGHLIGHT_BACKGROUND_ALPHA shl 24) or (highlightColor and 0x00ffffff)
        highlightStrokePaint.let { p ->
            p.color = highlightColor
            p.strokeWidth = highlightStrokeWidth.toFloat()
        }

        timelineTextPaint.textSize = timelineTextSize.toFloat()

        dayBackgroundPaint.color = dayBackgroundColor
        todayBackgroundPaint.color = todayBackgroundColor

        measureTimeline()
        measureDayOfWeekHeight()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if(!didAttach){
            current = today
            loadSchedules(current.withDayOfWeek(DayOfWeek.SUNDAY), true)

            didAttach = true
        }
    }

    private fun loadSchedules(weekDate: LocalDateTime, force: Boolean = false){
        val isSameWeek = current.isSameWeek(weekDate)
        if(isSameWeek && !force) return

        clearAllRect()

        val schedules = mutableListOf<WeekEvent>()
        //var date = weekDate.minusWeeks(1)
        var date = weekDate

        for(day in 0 until DEFAULT_PRELOAD_WEEK_DATA_RANGE){
            with(date){
                val pool = listener?.onWeekChanged(
                    year,
                    monthValue,
                    dayOfMonth,
                    weekOfYear(),
                ) ?: listOf()

                schedules.addAll(sortAndSplitSchedules(pool))
            }

            date = date.plusWeeks(1)
        }

        schedules.groupBy { if(it.isAllDay()) "A" else "C" }
            .forEach { (k, l) ->
                when(k){
                    "A" -> dispatchAllDayEvents(l.toMutableList())
                    "C" -> dispatchCommonEvents(l.toMutableList())
                }
            }

        current = weekDate
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        isDrawn = true
        println("[onDraw]")

        // 스크롤을 할 수 있는 최대 최소치를 정해준다
        origin.y = max(height - (hourHeight * 24) - headerHeight.toFloat() ,min(origin.y, 0f))

        // 셀 하나에 하루
        widthPerDay = (width - timelineWidth - visibleDays - 1) / visibleDays

        val firstVisibleIndex = -ceil(origin.x / widthPerDay).toInt()
        val startX = timelineWidth + origin.x + (widthPerDay * firstVisibleIndex)
        var offsetX = startX

        val firstDayOfWeek = current.withDayOfWeek(DayOfWeek.SUNDAY)

        drawTimeline(canvas)
        drawGrid(canvas, firstVisibleIndex, firstDayOfWeek, startX)

        canvas?.drawRect(timelineWidth * 1f, 0f, width * 1f, dayOfWeekHeight * 1f, dayBackgroundPaint)

        for(drawWhat in 0 until 2){
            val temp = firstDayOfWeek.run { LocalDateTime.of(toLocalDate(), toLocalTime()) }

            for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
                val date = temp.plusDays((index - 1).toLong())

                val dt = when {
                    today.isBeforeDay(date) -> DayType.PAST
                    date.isSameDay(today) -> DayType.TODAY
                    else -> DayType.COMMON
                }

                when(drawWhat){
                    0 -> {
                        drawWeekOfDay(canvas, date, offsetX, dt)
                        drawCommonEvent(canvas, date, offsetX, dt)
                    }

                    1 -> drawAllDayEvent(canvas, date, offsetX, dt)
                }

                offsetX += widthPerDay
            }

            offsetX = startX
        }

        if(isEditMode)
            drawEdit(canvas, firstVisibleIndex, firstDayOfWeek, startX)
    }

    private fun sortAndSplitSchedules(events: List<WeekEvent>) : List<WeekEvent> {
        sortEvent(events)

        return events
            .filter { it.startTimeInMillis < it.endTimeInMillis }
            .map(::splitEventPerDay)
            .flatten()
    }

    private fun adjustScheduleStartOrEnd(times: Long, isStartOrEnd: Boolean)
        = times.toLocalDateTime().run { withTime(if(isStartOrEnd) hour else hour + 1, 0, 0) }

    private fun <T: WeekEvent> splitEventPerDay(event: T) : List<WeekEvent> {
        val events = mutableListOf<WeekEvent>()

        // 스케쥴이 하루에 끝나지 않는 경우에는 스케쥴을 일 단위로 분리를 시켜줘야 한다
        if(!event.run { startAt!!.isSameDay(endAt!!) }){
            with(event){
                val ends = startAt!!.withTime(23, 59, 59)

                // 첫날은 시작일 ~ 해당일 23:59:59 까지 이벤트를 진행하도록 설정한다
                events.add(copyEventWith(this).also {
                    it.setStartAndEndDate(startAt!!, ends, isAllDay())
                })

                // 시작일 다음으로 달력의 커서를 변경하고,
                var other = startAt!!.plusDays(1)

                // 그 다음날 부터는 마지막 스케쥴 날짜와 같아지기 전까지 다음의 행위를 반복한다.
                // 시작일 00:00:00 시작일의 종료일 23:59:59 까지 설정하고 반복
                while(!other.isSameDay(endAt!!)){
                    val nextDay = other.withTime(0)
                    val endOfNextDay = nextDay.withTime(23, 59, 59)

                    events.add(copyEventWith(this).also {
                        it.setStartAndEndDate(nextDay, endOfNextDay, isAllDay())
                    })

                    other = other.plusDays(1)
                }

                // 마지막 날은 시작 일을 마지막 날의 00:00:00 에 설정 하고서
                // 마지막 날과 함께 이벤트 객체를 만들어 삽입 하도록 한다.
                val lastDay = endAt!!.withTime(0)

                events.add(copyEventWith(this).also {
                    it.setStartAndEndDate(lastDay, endAt!!, isAllDay())
                })
            }
        }else{
            // 스케쥴 분리가 필요 없는 것은 그대로 넣어 주도록 한다.
            events.add(event)
        }

        return events
    }

    private fun <T: WeekEvent> copyEventWith(event: T) : WeekEvent
        = when (event) {
            is DummyWeekEvent -> (event as DummyWeekEvent).clone()
            else -> (event as WeekEvent).clone()
        }

    private fun dispatchCommonEvents(events: MutableList<WeekEvent>){
        while(events.size > 0){
            val groups = mutableListOf<WeekEvent>()

            val e1 = events.removeFirst()
            groups.add(e1)

            var i = 0
            while(i < events.size){
                val e2 = events[i]

                // 겹치는 일정 들을 묶어 보내도록 한다.
                if(e1.startAt!!.isSameDay(e2.startAt!!)){
                    events.removeAt(i)
                    groups.add(e2)
                }else{
                    i++
                }
            }

            overlapCommonEvents(groups)
        }
    }

    private fun dispatchAllDayEvents(events: MutableList<WeekEvent>){
        for(event in events){
            with(event){
                val (l, r) = 0f to 1f
                val t = 0f
                val b = 1f

                val rect = WeekRect(l, t, r, b, this)

                rects.add(rect)
            }
        }
    }

    private fun overlapCommonEvents(events: List<WeekEvent>){
        val collisions = mutableListOf<MutableList<WeekEvent>>()

        for(e in events){
            var isPlaced = false

            for(collides in collisions){
                for(ge in collides){
                    if(isEventCollide(ge, e)){
                        collides.add(e)
                        isPlaced = true

                        break
                    }
                }
            }

            if(!isPlaced){
                collisions.add(mutableListOf<WeekEvent>().apply {
                    add(e)
                })
            }
        }

        collisions.forEach(::positioningEventRectOffset)
    }

    private fun positioningEventRectOffset(group: List<WeekEvent>){
        val spans = mutableListOf<MutableList<WeekEvent>>()
        spans.add(mutableListOf())

        for(e in group){
            var isPlaced = false

            for(span in spans){
                if(span.isEmpty()){
                    span.add(e)

                    isPlaced = true
                }else if(!isEventCollide(e, span.last())){
                    span.add(e)

                    isPlaced = true
                    break
                }
            }

            if(!isPlaced){
                spans.add(mutableListOf<WeekEvent>().apply {
                    add(e)
                })
            }
        }

        val sortedSpans = spans.flatten().sortedByDescending { it.difference }
        val spanCount = sortedSpans.size

        var j = 0f
        for(span in sortedSpans){
            with(span){
                val alpha = (255 * ((j + 1) / spanCount)).roundToInt()
                setBackgroundColor(alpha shl 24 or (backgroundColor and 0x00ffffff))

                val l = 0f
                val t = startAt!!.toMinuteOfHours() * 1f
                val r = 1f
                val b = endAt!!.toMinuteOfHours() * 1f

                val rect = WeekRect(l, t, r, b, this)

                rects.add(rect)
            }

            j++
        }
    }

    private fun positioningTempEventRectOffset(events: List<DummyWeekEvent>){
        if(events.isEmpty()) return

        removeOnlyDummyEvent()
        events.forEach {
            with(it){
                val t = startAt!!.toMinuteOfHours() * 1f
                val b = endAt!!.toMinuteOfHours() * 1f

                rects.add(WeekRect(0f, t, 1f, b, this))
            }
        }
    }

    private fun sortEvent(events: List<WeekEvent>){
        Collections.sort(events) { e1, e2 ->
            val (a1, a2) = e1.startTimeInMillis to e2.startTimeInMillis
            var comparator = if(a1 > a2) 1 else (if(a1 < a2) -1 else 0)

            if(comparator == 0){
                val (b1, b2) = e1.startTimeInMillis to e2.startTimeInMillis
                comparator = if(b1 > b2) 1 else (if(b1 < b2) -1 else 0)
            }

            comparator
        }
    }

    private fun measureTimeline(){
        val temp = today

        for(i in 0 until 24){
            val str = temp.withTime(i, 0).toText("HH:mm")

            timelineTextWidth = timelineTextWidth.coerceAtLeast(timelineTextPaint.measureText(str).toInt())

            if(i == 23)
                timelineTextHeight = Rect().apply { timelineTextPaint.getTextBounds(str, 0, str.length, this) }.height()
        }

        timelineWidth = timelineTextWidth + (timelineHorizontalPadding * 2)
        allDayTextWidth = timelineTextPaint.measureText(allDayEventText).toInt()
    }

    private fun measureDayOfWeekHeight(){
        dayOfWeekTextHeight = today.toText("dd E").run {
            Rect().also { r -> dayOfWeekTextPaint.getTextBounds(this, 0, length, r) }.height()
        }
    }

    private fun drawTimeline(canvas: Canvas?){
        canvas?.run {
            save()

            val date = today.withTime(0)

            clipRect(0f, headerHeight - (timelineTextHeight / 2f), timelineWidth * 1f, height * 1f)

            for(i in 0 until 24){
                val str = date.withHour(i).toText("HH:mm")

                val top = origin.y + headerHeight + (hourHeight * i) + (timelineTextHeight / 2)

                if(top < height)
                    drawText(str, (timelineTextWidth + timelineHorizontalPadding).toFloat(), top, timelineTextPaint)
            }

            restore()
        }
    }

    private fun drawGrid(canvas: Canvas?, firstVisibleIndex: Int, firstDayOfWeek: LocalDateTime, startX: Float){
        canvas?.run {
            save()

            clipRect(0, dayOfWeekHeight, width, height)

            val lines = ((height - headerHeight) / hourHeight + 1) * (visibleDays + 1)
            val (h, v) = FloatArray(lines * 4) to FloatArray(lines * 4)

            var (offsetX, offsetY) = startX to headerHeight
            var (x1, y1) = .0f to .0f
            var (x2, y2) = .0f to .0f

            for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
                val date = firstDayOfWeek.plusDays((index - 1) * 1L)
                val left = offsetX.coerceAtLeast(timelineWidth.toFloat())

                // 배경색 설정 당일인 경우에 다른색으로도 표시 가능
                if(widthPerDay + offsetX - left > 0){
                    drawRect(left, top.toFloat(), offsetX + widthPerDay, height.toFloat(),
                        if(today.isSameDay(date)) todayBackgroundPaint
                        else dayBackgroundPaint)
                }

                var i = 0
                for(hour in 1 .. 25) {
                    val top = offsetY + origin.y + (hourHeight * hour)

                    if(top > offsetY - gridSeparatorStrokeWidth
                        && top < height
                        && offsetX + widthPerDay - left > 0){

                        if(i < 24){
                            h[i * 4] = left
                            h[i * 4 + 1] = top
                            h[i * 4 + 2] = offsetX + widthPerDay
                            h[i * 4 + 3] = top
                        }

                        x1 = left
                        y1 = top + 10
                        x2 = left
                        y2 = top - (hourHeight * 2)

                        v[i * 4] = x1
                        v[i * 4 + 1] = y1
                        v[i * 4 + 2] = x2
                        v[i * 4 + 3] = y2

                        i++
                    }
                }

                // Grid 를 그려준다
                drawLines(h, gridSeparatorPaint)
                drawLines(v, gridSeparatorPaint)

                drawLine(x1, y1 + hourHeight, x2, y2, gridSeparatorPaint)

                offsetX += widthPerDay
            }

            restore()
        }
    }

    private fun drawWeekOfDay(canvas: Canvas?, date: LocalDateTime, offsetX: Float, dayType: Int){
        canvas?.run {
            save()
            clipRect(timelineWidth, 0, width, dayOfWeekHeight)

            val (x, y) = offsetX + (widthPerDay / 2) to dayOfWeekHeight - (dayOfWeekVerticalPadding / 2f)
            val str = date.toText("dd E")

            when(date.dayOfWeek){
                DayOfWeek.SUNDAY -> drawText(str, x, y, dayOfWeekSundayTextPaint)
                DayOfWeek.SATURDAY -> drawText(str, x, y, dayOfWeekSaturdayTextPaint)
                else -> {
                    when(dayType){
                        DayType.TODAY -> drawText(str, x, y, dayOfWeekTodayTextPaint)
                        else -> drawText(str, x, y, dayOfWeekTextPaint)
                    }
                }
            }

            restore()
        }
    }

    private fun drawCommonEvent(canvas: Canvas?, date: LocalDateTime, offsetX: Float, type: Int){
        canvas?.run {
            save()

            val left = offsetX.coerceAtLeast(timelineWidth.toFloat())
            val top = headerHeight

            drawCommonRect(this, date, offsetX, type)

            // 현재 날의 시간 위치를 수평선으로 보여 준다.
            if(type == DayType.TODAY){
                val barOffset = top + origin.y
                val beforeNow = today.run { (hour + (minute / 60f)) * hourHeight }

                drawLine(left, barOffset + beforeNow, offsetX + widthPerDay, barOffset + beforeNow, timeNowPaint)
            }

            restore()
        }
    }

    private fun drawCommonRect(canvas: Canvas?, date: LocalDateTime, startX: Float, type: Int){
        if(rects.isEmpty()) return

        val cRects = rects
            .filterNot { (!date.isSameDay(it.startAt) || it.event.isAllDay()) || it.event is DummyWeekEvent }
            .reversed()

        canvas?.run {
            clipRect(timelineWidth, headerHeight, width, height)

            for (rect in cRects){
                val offsetY = origin.y + headerHeight

                val l = startX + (rect.left * widthPerDay)
                val t = offsetY + ((hourHeight * 24 * rect.top) / 1440)
                val r = l + (rect.right * widthPerDay)
                val b = offsetY + ((hourHeight * 24 * rect.bottom) / 1440)

                rect.setAbsoluteRect(l, t, r, b)

                painter.color = when(type){
                    DayType.PAST -> pastEventColor
                    else -> rect.backgroundColor
                }

                drawRoundRect(rect.absoluteRect, eventCornerRadius, eventCornerRadius, painter)
                drawEventText(this, rect.originalEvent, l, t, r, b)
            }
        }
    }

    private fun drawEventText(canvas: Canvas?, event: WeekEvent, l: Float, t: Float, r: Float, b: Float){
        canvas?.run {
            val sb = SpannableStringBuilder()
            if(event.title.isNotBlank()){
                with(sb){
                    append(event.title)
                    setSpan(StyleSpan(Typeface.NORMAL), 0, length, 0)
                }
            }

            val (availWidth, availHeight) = r - l - eventTitlePadding to b - t - eventTitlePadding

            var sl = StaticLayout(sb, eventTextPaint, availWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
            val lineHeight = sl.height / sl.lineCount

            if(availHeight >= lineHeight){
                var availLineCount = (availHeight / lineHeight)
                    .coerceAtMost(DEFAULT_EVENT_TEXT_MAX_LINES)

                do {
                    sl = StaticLayout(
                        TextUtils.ellipsize(sb, eventTextPaint, availLineCount * availWidth, TextUtils.TruncateAt.END),
                        eventTextPaint,
                        availWidth.toInt(),
                        Layout.Alignment.ALIGN_NORMAL,
                        1f, 0f, false
                    )

                    availLineCount--
                } while (sl.height > availHeight)

                save()

                translate(l + (eventTitlePadding / 2), t + (eventTitlePadding / 2))
                sl.draw(this)

                restore()
            }
        }
    }

    private fun drawAllDayEvent(canvas: Canvas?, date: LocalDateTime, offsetX: Float, type: Int){
        canvas?.run {
            save()

            drawText(allDayEventText, timelineHorizontalPadding + allDayTextWidth * 1f, (headerHeight / 2) + (timelineTextHeight * 1f), timelineTextPaint)

            val left = offsetX.coerceAtLeast(timelineWidth.toFloat())

            drawAllDayRect(this, date, offsetX, type)

            drawLine(left, headerHeight * 1f, width * 1f, headerHeight * 1f, gridSeparatorPaint)

            restore()
        }
    }

    private fun drawAllDayRect(canvas: Canvas?, date: LocalDateTime, offsetX: Float, type: Int){
        if(rects.isEmpty()) return

        canvas?.run {
            save()

            clipRect(timelineWidth, dayOfWeekHeight, width, headerHeight)

            for(rect in rects){
                if(!date.isSameDay(rect.startAt) || !rect.event.isAllDay()) continue

                val (l, t) = offsetX to dayOfWeekHeight + (allDayAreaHeight * rect.top)
                val r = l + (rect.right * widthPerDay)
                val b = t + (allDayAreaHeight * rect.bottom)

                painter.color = when(type){
                    DayType.PAST -> pastEventColor
                    else -> rect.backgroundColor
                }

                rect.setAbsoluteRect(l, t, r, b)

                drawRoundRect(rect.absoluteRect, eventCornerRadius, eventCornerRadius, painter)
                drawEventText(canvas, rect.event, l, t, r, b)
            }

            restore()
        }
    }

    private fun drawEdit(canvas: Canvas?, firstVisibleIndex: Int, firstDayOfWeek: LocalDateTime, startX: Float){
        val dRects = rects.filter { it.originalEvent is DummyWeekEvent }
        var offsetX = startX

        canvas?.run {
            save()

            drawRect(highlightRect, highlightPaint)
            drawRect(highlightRect, highlightStrokePaint)

            val offsetY = origin.y + headerHeight

            for(rect in dRects){
                val temp = firstDayOfWeek.run { LocalDateTime.of(toLocalDate(), toLocalTime()) }

                for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1) {
                    val date = temp.plusDays((index - 1).toLong())

                    if(!date.isSameDay(rect.startAt)) {
                        offsetX += widthPerDay

                        continue
                    }

                    val l = offsetX + (rect.left * widthPerDay)
                    val t = offsetY + ((hourHeight * 24 * rect.top) / 1440)
                    val r = l + (rect.right * widthPerDay)
                    val b = offsetY + ((hourHeight * 24 * rect.bottom) / 1440)

                    rect.setAbsoluteRect(l, t, r, b)

                    if(l < r
                        && l < width && t < height
                        && r > timelineWidth && b > headerHeight){
                        painter.color = rect.backgroundColor

                        drawRoundRect(rect.absoluteRect, eventCornerRadius, eventCornerRadius, painter)
                        drawEventText(this, rect.originalEvent, l, t, r, b)
                    }
                }

                offsetX = startX
            }

            restore()
        }
    }

    private fun isEventCollide(e1: WeekEvent, e2: WeekEvent)
        = !((e1.startTimeInMillis >= e2.endTimeInMillis)
            || (e1.endTimeInMillis <= e2.startTimeInMillis))

    override fun onTouchEvent(event: MotionEvent?): Boolean
        = event?.run {
            scaleDetector.onTouchEvent(event)

            if(detector.onTouchEvent(this)){
                true
            }else{
                when(actionMasked){
                    MotionEvent.ACTION_MOVE -> {
                        if(isLongPressed){
                            isLongPressed = false

                            val cancelEvent = MotionEvent.obtain(this).apply {
                                action = MotionEvent.ACTION_CANCEL
                            }

                            detector.onTouchEvent(cancelEvent)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if(isEditMode){
                            val rect = rects.find { it.originalEvent is DummyWeekEvent } ?: return@run true
                            val event = rect.originalEvent

                            listener?.onEmptyEventWillBeAdded(event.startTimeInMillis, event.endTimeInMillis)
                        }

                        if(!isZooming && currentFlingDirection == Scroll.NONE){
                            if(currentScrollDirection.let { it == Scroll.LEFT || it == Scroll.RIGHT}){
                                scrollToNearestOrigin()
                            }

                            currentScrollDirection = Scroll.NONE
                        }
                    }
                }

                super.onTouchEvent(event)
            }
        } ?: super.onTouchEvent(event)

    private fun getTimeFromPoint(x: Float, y: Float): Long {
        var date = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        val firstVisibleIndex = -ceil(origin.x / widthPerDay).toInt()
        var startX = timelineWidth + origin.x + (widthPerDay * firstVisibleIndex)

        for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
            val start = startX.coerceAtLeast(timelineWidth * 1f)

            if(startX + widthPerDay - start > 0 && x > start && x < startX + widthPerDay){
                val offsetY = y - origin.y - headerHeight
                val hour = (offsetY / hourHeight).toInt()

                date = date.plusDays((index - 1).toLong())
                    .withTime(hour, 0, 0)

                return date.toTimeMillis()
            }

            startX += widthPerDay
        }

        return -1L
    }

    override fun computeScroll() {
        super.computeScroll()

        if(scroller.isFinished){
            if(currentFlingDirection != Scroll.NONE){
                scrollToNearestOrigin()
            }
        }else{
            if(currentFlingDirection != Scroll.NONE
                && scroller.currVelocity <= minimumFlingVelocity){
                scrollToNearestOrigin()
            }else if(scroller.computeScrollOffset()){
                origin.set(scroller.currX.toFloat(), scroller.currY.toFloat())

                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    private fun scrollToNearestOrigin(animated: Boolean = true){
        val days = (origin.x / widthPerDay).run {
            when {
                currentScrollDirection == Scroll.LEFT -> floor(this)
                currentScrollDirection == Scroll.RIGHT -> ceil(this)
                currentFlingDirection != Scroll.NONE -> kotlin.math.round(this)
                else -> kotlin.math.round(this)
            }
        }

        val nearestOrigin = origin.x - (days * widthPerDay)
        if(nearestOrigin != 0f){
            scrollOriginTo(-nearestOrigin.toInt(), 0, if(animated) 250 else 0)
        }

        with(Scroll.NONE){
            currentScrollDirection = this
            currentFlingDirection = this
        }
    }

    private fun scrollOriginTo(dx: Int, dy: Int, duration: Int = 250){
        with(scroller){
            forceFinished(true)
            startScroll(origin.x.toInt(), origin.y.toInt(), dx, dy, duration)
        }

        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun clearEditMode(){
        if(!isEditMode) return

        removeOnlyDummyEvent()

        isEditMode = false
        editEvent = null
        pEditEvent = null
    }

    private fun removeOnlyDummyEvent(){
        rects.removeIf { it.originalEvent is DummyWeekEvent }
    }

    private fun clearAllRect(){
        if(rects.isEmpty()) return

        rects.clear()
    }

    var onWeekChangeListener: OnWeekViewListener?
        set(value) { this.listener = value }
        get() = this.listener

    /**
     * 현재 시각으로 스크롤을 이동하도록 한다.
     */
    fun scrollToCurrentTime() {
        scrollToTime(LocalDateTime.now().hour)
    }

    /**
     * 해당 시각으로 스크롤을 이동하도록 한다. 해당 메소드를 사용하지 않으면 00시부터 데이터가 디스플레이 된다.
     *
     * @param hour 0~23 까지의 시간
     * @param smoothScroll 스크롤 애니메이션 여부
     */
    fun scrollToTime(hour: Int, smoothScroll: Boolean = true){
        if(!isDrawn){
            val offset = ((hourHeight * hour) - height)

            origin.y = -offset.toFloat()
        }else{
            if(!smoothScroll){
                val offset = hourHeight * hour

                origin.y = -offset.toFloat()

                invalidate()
            }else{
                val dy = origin.y + (hourHeight * hour)

                scrollOriginTo(0, -dy.toInt())
            }
        }
    }

    /**
     * 스크롤 애니메이션이 없이 시간을 설정하도록 한다.
     *
     * @param hour 0~23 까지의 시간
     */
    fun scrollToTime(hour: Int){
        scrollToTime(hour, false)
    }

    fun moveToNext(){
        val date = current.plusWeeks(1)

        loadSchedules(date)

        invalidate()
    }

    fun moveToPrev(){
        val date = current.minusWeeks(1)

        loadSchedules(date)

        invalidate()
    }

    /**
     * 지정된 시간을 하이라이팅하여 보여주도록 한다.
     */
    private fun showScheduleInsertion(date: LocalDate){
        dismissScheduleInsertion()

        val firstVisibleIndex = -ceil(origin.x / widthPerDay).toInt()
        val startX = timelineWidth + origin.x + (widthPerDay * firstVisibleIndex)

        val firstVisibleDate = current
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            .plusDays(firstVisibleIndex * 1L).toLocalDate()

        val days = date.toEpochDay() - firstVisibleDate.toEpochDay()

        val (l, t) = startX + (days * widthPerDay) to highlightStrokeWidth
        val (r, b) = l + widthPerDay to height - t

        highlightRect.set(l.toInt(), t, r.toInt(), b)
    }
    private fun dismissScheduleInsertion(){
        highlightRect.set(0, 0, 0, 0)
    }

    interface OnWeekViewListener {
        fun onWeekChanged(year: Int, month: Int, date: Int, week: Int) : List<WeekEvent>?
        fun onWeekEventSelected(year: Int, month: Int, date: Int, week: Int, event: WeekEvent?)
        fun onEmptyEventWillBeAdded(start: Long, end: Long)
    }
}