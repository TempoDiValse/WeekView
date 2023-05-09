package kr.lavalse.kweekview

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.*
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.postDelayed
import kr.lavalse.kweekview.extension.EContext.toDP
import kr.lavalse.kweekview.extension.EContext.toSP
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
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class WeekView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DEFAULT_FORMAT_DAY_OF_WEEK = "E d"

        private const val DEFAULT_VISIBLE_DAYS = 7
        private const val DEFAULT_PRELOAD_WEEK_DATA_RANGE = 3

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
        private const val DEFAULT_DAY_OF_WEEK_TODAY_TEXT_COLOR = 0xFFFFFFFF
        private const val DEFAULT_DAY_OF_WEEK_TODAY_BACKGROUND_COLOR = 0xFF415AEF
        private const val DEFAULT_DAY_OF_WEEK_SUNDAY_TEXT_COLOR = 0xFFFF0011
        private const val DEFAULT_DAY_OF_WEEK_SATURDAY_TEXT_COLOR = 0xFF4682F1

        private const val DEFAULT_ALLDAY_TEXT = "종일"
        private const val DEFAULT_ALLDAY_AREA_HEIGHT = 50f

        private const val DEFAULT_DAY_BACKGROUND_COLOR = Color.WHITE
        private const val DEFAULT_TODAY_BACKGROUND_COLOR = 0xFFC7F1FF
        private const val DEFAULT_PAST_EVENT_COLOR = Color.LTGRAY

        private const val DEFAULT_EVENT_CORNER_RADIUS = 0f
        private const val DEFAULT_EVENT_TITLE_PADDING = 10f
        private const val DEFAULT_EVENT_TEXT_COLOR = 0xFF333333
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

        /** 다음 페이지로 이동하는 플래그 */
        const val LEFT = 1
        /** 이전 페이지로 이동하는 플래그 */
        const val RIGHT = 2
        const val VERTICAL = 3

        const val X_SPEED = .18f
        const val X_AUTO_SCROLL_DURATION = 250
    }

    private object PageMove {
        const val PREV_MONTH = -1
        const val NONE = 0
        const val NEXT_MONTH = 1
        const val TODAY = 2
        const val WHERE = 3

        const val DISTANCE_TO_MOVE = 400

        fun doPageMove(state: Int) = arrayOf(PREV_MONTH, NEXT_MONTH, WHERE, TODAY).contains(state)
    }

    private val today = LocalDateTime.now()
    private var current : LocalDateTime = today
    private var firstVisibleDate: LocalDateTime = today

    private val rects: MutableList<WeekRect> = mutableListOf()

    // x, y 좌표
    private val origin: PointF = PointF(0f, 0f)
    private var prevX: Float = 0f

    private var eventCornerRadius = 0f
    private var eventTitlePadding = 0f
    private var painter = Paint(Paint.ANTI_ALIAS_FLAG)
    private var strokePainter = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private var pastEventColor = Color.WHITE

    private var editEventText = ""
    private var editEventColor = Color.WHITE
    private var editEventDimPaint : Paint = Paint()

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
    private var dayOfWeekTodayBackgroundColor = 0
    private var dayOfWeekSundayTextColor = 0
    private var dayOfWeekSaturdayTextColor = 0
    private val dayOfWeekTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekTodayTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekTodayBackgroundPaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
    private var canMovePastDate = false

    private var listener : OnWeekViewListener? = null

    private var editEvent : DummyWeekEvent? = null

    private var scroller = OverScroller(context, DecelerateInterpolator())
    private var detector: GestureDetectorCompat
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            if(isEditMode){
                clearEditMode()

                invalidate()

                return true
            }

            scroller.forceFinished(true)

            scrollToNearestOrigin()

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if(rects.isNotEmpty()){
                //for(r in rects.reversed()){
                for(r in rects){
                    if(r.containsTouchPoint(e.x, e.y)){
                        val event = r.originalEvent
                        listener?.onWeekEventSelected(event)

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
                    origin.x -= dx * Scroll.X_SPEED

                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
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
                Scroll.LEFT, Scroll.RIGHT -> {
                    scroller.fling(
                        origin.x.toInt(), origin.y.toInt(),
                        (vx * Scroll.X_SPEED).toInt(), 0,
                        Int.MIN_VALUE, Int.MAX_VALUE,
                        minY, 0
                    )
                }

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

            val start = adjustScheduleStartOrEnd(getTimeFromPoint(e.x, e.y), true)
            val end = adjustScheduleStartOrEnd(getTimeFromPoint(e.x, e.y), false)

            prepareScheduleInternal(start, end)

            animator = animatorHighlight()
            animator?.start()
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
    private var statePageMove = PageMove.NONE

    private var animator : Animator? = null

    init {
        with(context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, defStyleAttr, 0)){
            textSize = getDimensionPixelSize(R.styleable.WeekView_android_textSize,
                context.toSP(DEFAULT_TEXT_SIZE).toInt())
            canMovePastDate = getBoolean(R.styleable.WeekView_canMovePastDate, canMovePastDate)

            // Event
            //eventCornerRadius = getDimension(R.styleable.WeekView_eventCornerRadius,
            //    context.toDP(DEFAULT_EVENT_CORNER_RADIUS))
            eventTitlePadding = getDimension(R.styleable.WeekView_eventTitlePadding, context.toDP(
                DEFAULT_EVENT_TITLE_PADDING))
            eventTextColor = getColor(R.styleable.WeekView_eventTextColor, DEFAULT_EVENT_TEXT_COLOR.toInt())
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
            dayOfWeekTodayBackgroundColor = getColor(R.styleable.WeekView_dayOfWeekTodayBackgroundColor, DEFAULT_DAY_OF_WEEK_TODAY_BACKGROUND_COLOR.toInt())
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

        dayOfWeekTodayBackgroundPaint.color = dayOfWeekTodayBackgroundColor
        dayBackgroundPaint.color = dayBackgroundColor
        todayBackgroundPaint.color = todayBackgroundColor

        measureTimeline()
        measureDayOfWeekHeight()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if(!didAttach){
            current = today

            loadSchedules(current, true)

            didAttach = true
        }
    }

    private fun loadSchedules(weekDate: LocalDateTime, force: Boolean = false){
        val isSameWeek = current.isSameWeek(weekDate)
        if(isSameWeek && !force) return

        clearAllRect()

        val schedules = mutableListOf<WeekEvent>()
        var date = weekDate.minusWeeks(1)

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

        current.run { listener?.onCurrentWeekChanged(year, monthValue, dayOfMonth, weekOfYear()) }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // 스크롤을 할 수 있는 최대 최소치를 정해준다
        origin.y = max(height - (hourHeight * 24) - headerHeight.toFloat() ,min(origin.y, 0f))

        // 셀 하나에 하루
        widthPerDay = (width - timelineWidth - visibleDays - 1) / visibleDays

        val firstVisibleIndex = -ceil(origin.x / widthPerDay).toInt()
        val startX = timelineWidth + origin.x + (widthPerDay * firstVisibleIndex)
        var offsetX = startX

        val standard = LocalDateTime.now()

        drawTimeline(canvas)
        drawGrid(canvas, firstVisibleIndex, standard, startX)

        canvas?.drawRect(timelineWidth * 1f, 0f, width * 1f, dayOfWeekHeight * 1f, dayBackgroundPaint)

        for(drawWhat in 0 until 2){
            for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
                val date = standard.plusDays((index - 1).toLong())

                val dt = when {
                    today.isBeforeDay(date) -> DayType.PAST
                    date.isSameDay(today) -> DayType.TODAY
                    else -> DayType.COMMON
                }

                when(drawWhat){
                    0 -> {
                        drawDayOfWeek(canvas, date, offsetX, dt)
                        drawCommonEvent(canvas, date, offsetX, dt)
                    }

                    1 -> drawAllDayEvent(canvas, date, offsetX, dt)
                }

                if(index == firstVisibleIndex + 1){
                    firstVisibleDate = date
                }

                offsetX += widthPerDay
            }

            offsetX = startX
        }

        if(isEditMode)
            drawEdit(canvas, firstVisibleIndex, standard, startX)

        isDrawn = true
    }

    private fun sortAndSplitSchedules(events: List<WeekEvent>) : List<WeekEvent> {
        sortEvent(events)

        return events
            .filter { it.startTimeInMillis < it.endTimeInMillis }
            .map(::splitEventPerDay)
            .flatten()
    }

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

        var j = 0f
        for(span in sortedSpans){
            with(span){
                setBackgroundColor(backgroundColor)

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
        dayOfWeekTextHeight = today.toText(DEFAULT_FORMAT_DAY_OF_WEEK).run {
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

    private fun drawDayOfWeek(canvas: Canvas?, date: LocalDateTime, offsetX: Float, dayType: Int){
        canvas?.run {
            save()
            clipRect(timelineWidth, 0, width, dayOfWeekHeight)

            val (x, y) = offsetX + (widthPerDay / 2) to dayOfWeekHeight - (dayOfWeekVerticalPadding / 2f)

            val isFirstDayOfMonth = YearMonth.from(date).atDay(1).isSameDay(date.toLocalDate())
            val str = date.toText(if(!isFirstDayOfMonth) DEFAULT_FORMAT_DAY_OF_WEEK else "E M/d")

            when(date.dayOfWeek){
                DayOfWeek.SUNDAY -> drawText(str, x, y, dayOfWeekSundayTextPaint)
                DayOfWeek.SATURDAY -> drawText(str, x, y, dayOfWeekSaturdayTextPaint)
                else -> {
                    when(dayType){
                        DayType.TODAY -> {
                            val top = (dayOfWeekVerticalPadding / 4f)

                            drawRoundRect(offsetX, top, offsetX + widthPerDay,  ((dayOfWeekHeight * 1f) - (top / 2f)), 10f, 10f, dayOfWeekTodayBackgroundPaint)
                            drawText(str, x, y, dayOfWeekTodayTextPaint)
                        }
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
            save()

            clipRect(timelineWidth, headerHeight, width, height)

            for (rect in cRects){
                val offsetY = origin.y + headerHeight

                val l = startX + (rect.left * widthPerDay)
                val t = offsetY + ((hourHeight * 24 * rect.top) / 1440)
                val r = l + (rect.right * widthPerDay)
                val b = offsetY + ((hourHeight * 24 * rect.bottom) / 1440)

                rect.setAbsoluteRect(l, t, r, b)

                drawEventArea(this, rect, type)
            }

            restore()
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

                rect.setAbsoluteRect(l, t, r, b)

                drawEventArea(this, rect, type)
            }

            restore()
        }
    }

    private fun drawEventArea(canvas: Canvas, rect: WeekRect, type: Int){
        val absRect = rect.absoluteRect

        painter.color = when(type){
            DayType.PAST -> pastEventColor
            else -> rect.backgroundColor
        }

        with(canvas){
            drawRect(absRect, painter)

            if(rect.isBrightColor){
                strokePainter.color = rect.strokeColor
                drawRect(absRect, strokePainter)

                eventTextPaint.color = eventTextColor
            }else{
                eventTextPaint.color = Color.WHITE
            }

            drawEventText(canvas, rect.originalEvent, absRect.left, absRect.top, absRect.right, absRect.bottom)
        }
    }

    private fun drawEdit(canvas: Canvas?, firstVisibleIndex: Int, firstDayOfWeek: LocalDateTime, startX: Float){
        val dRects = rects.filter { it.originalEvent is DummyWeekEvent }
        if(dRects.isEmpty()) return

        var offsetX = startX

        canvas?.run {
            save()

            // Dim 을 담당한다.
            drawRect(0f, 0f, width.toFloat(), height.toFloat(), editEventDimPaint)

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

            if(detector.onTouchEvent(this)) true
            else{
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
        var date = today

        val firstVisibleIndex = -ceil(origin.x / widthPerDay).toInt()
        var startX = timelineWidth + origin.x + (widthPerDay * firstVisibleIndex)

        for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
            val start = startX.coerceAtLeast(timelineWidth * 1f)

            if(startX + widthPerDay - start > 0
                && x > start && x < startX + widthPerDay){
                val offsetY = y - origin.y - headerHeight
                val hour = (offsetY / hourHeight).toInt()

                date = date
                    .plusDays((index - 1).toLong())
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
            if(PageMove.doPageMove(statePageMove)){
                when(statePageMove){
                    PageMove.TODAY -> loadSchedules(today)
                    PageMove.PREV_MONTH, PageMove.NEXT_MONTH, PageMove.WHERE -> loadSchedules(current, true)
                }

                statePageMove = PageMove.NONE
            }

            // 모든 스크롤/플링 이벤트가 종료 되면,
            if(currentScrollDirection == Scroll.NONE
                && currentFlingDirection == Scroll.NONE){
                // x 좌표를 저장한다.
                prevX = origin.x
            }
        }else{
            if(currentFlingDirection != Scroll.NONE
                && scroller.currVelocity <= minimumFlingVelocity){

                scrollToNearestOrigin()
            }else if(scroller.computeScrollOffset()){
                if(!canMovePastDate && scroller.currX > 0){
                    origin.set(0f, scroller.currY.toFloat())

                    scrollToNearestOrigin()
                }else{
                    origin.set(scroller.currX.toFloat(), scroller.currY.toFloat())
                }


                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    /**
     * 가장 가까운 일자에 자동으로 스크롤이 붙도록 한다.
     * 해당 메소드는 @see[onTouchEvent] (Scroll 시)  와 @see[computeScroll] (Fling 시) 에서 확인 가능하다
     */
    private fun scrollToNearestOrigin(){
        val diffX = abs(origin.x - prevX)

        // 허용 범위를 넘어서게되면 페이지를 이동한다.
        if(diffX > PageMove.DISTANCE_TO_MOVE){
            val days = (origin.x / widthPerDay).run {
                when {
                    currentScrollDirection == Scroll.LEFT -> floor(this)
                    currentScrollDirection == Scroll.RIGHT -> ceil(this)
                    currentFlingDirection != Scroll.NONE -> kotlin.math.round(this)
                    else -> kotlin.math.round(this)
                }
            }

            when {
                currentScrollDirection == Scroll.LEFT -> {
                    statePageMove = PageMove.NEXT_MONTH

                    val date = LocalDate.now()
                        .minusDays(days.toLong())
                        .plusWeeks(1)
                        .withDayOfWeek(today.dayOfWeek)

                    val diff = today.toLocalDate().toEpochDay() - date.toEpochDay()
                    val offset = origin.x - (diff * widthPerDay)

                    current = date.toLocalDateTime()

                    scrollOriginTo(-offset.toInt(), 0)
                }

                currentScrollDirection == Scroll.RIGHT -> {
                    statePageMove = PageMove.PREV_MONTH

                    val date = LocalDate.now()
                        .minusDays(days.toLong())
                        .withDayOfWeek(today.dayOfWeek)

                    val diff = today.toLocalDate().toEpochDay() - date.toEpochDay()
                    val offset = origin.x - (diff * widthPerDay)

                    current = date.toLocalDateTime()

                    scrollOriginTo(-offset.toInt(), 0)
                }
            }
        }else{
            // 허용 범위를 넘어서지 못하게 되면 원래 좌표로 되돌아가게 된다.
            val dx = origin.x - prevX

            scrollOriginTo(-dx.toInt(), 0, 100)
        }

        with(Scroll.NONE){
            currentScrollDirection = this
            currentFlingDirection = this
        }
    }

    private fun doScrolling()
        = !(currentScrollDirection == Scroll.NONE
            && currentFlingDirection == Scroll.NONE
            && scroller.isFinished)

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

    /**
     * 다음 주로 이동하도록 한다.
     */
    fun moveToNext(){
        if(doScrolling() || statePageMove != PageMove.NONE) return

        statePageMove = PageMove.NEXT_MONTH
        current = current.plusWeeks(1)

        moveTo(current.toLocalDate())
    }

    /**
     * 이전 주로 이동하도록 한다.
     */
    fun moveToPrev(){
        if(doScrolling() || statePageMove != PageMove.NONE) return
        if(!canMovePastDate && firstVisibleDate.isSameDay(today)) return

        statePageMove = PageMove.PREV_MONTH
        current = current.minusWeeks(1)

        moveTo(current.toLocalDate())
    }

    /**
     * 오늘로 이동하도록 한다.
     */
    fun moveToToday(){
        if(doScrolling() || statePageMove != PageMove.NONE) return

        statePageMove = PageMove.TODAY

        moveTo(today.toLocalDate())
    }

    /**
     * 지정일로 이동하도록 한다.
     *
     * @param date 지정일
     */
    fun moveTo(date: LocalDate){
        if(statePageMove == PageMove.NONE)
            statePageMove = PageMove.WHERE

        if(isEditMode)
            clearEditMode()

        val fdow = firstVisibleDate.toLocalDate()
        val diff = date.toEpochDay() - fdow.toEpochDay()

        val offset = diff * widthPerDay

        scrollOriginTo(-offset.toInt(), 0)
    }

    /**
     * 지정된 시간을 하이라이팅하여 보여주도록 한다.
     */
    private fun showScheduleInsertion(date: LocalDate){
        dismissScheduleInsertion()

        val firstVisibleIndex = -ceil(origin.x / widthPerDay).toInt()
        val startX = timelineWidth + origin.x + (widthPerDay * firstVisibleIndex)

        val firstVisibleDate = today
                .plusDays(firstVisibleIndex * 1L)
                .toLocalDate()

        val days = date.toEpochDay() - firstVisibleDate.toEpochDay()

        val (l, t) = startX + (days * widthPerDay) to highlightStrokeWidth
        val (r, b) = l + widthPerDay to height - t

        highlightRect.set(l.toInt(), t, r.toInt(), b)
    }

    private fun dismissScheduleInsertion(){
        highlightRect.set(0, 0, 0, 0)
    }

    /**
     * 특정 위치(날짜시간)에 생성할 스케쥴의 위치를 표시하도록 한다.
     * 시간은 1시간 단위로 UI 상에 표시가 된다.
     *
     * @param date 표시하고자 하는 날짜와 시작시간
     */
    fun prepareSchedule(date: LocalDateTime){
        if(!current.isSameWeek(date)){
            current = date.withDayOfWeek(today.dayOfWeek)

            moveTo(current.toLocalDate())
            prepareSchedule(date)

            return
        }

        postDelayed(300) {
            val (start, end) = date.toTimeMillis().run {
                adjustScheduleStartOrEnd(this, true) to adjustScheduleStartOrEnd(this, false)
            }

            prepareScheduleInternal(start, end)
            scrollToTime(date.hour, false)

            if(animator != null){
                animator?.cancel()
                animator = null
            }

            animator = animatorHighlight()
            animator?.start()
        }
    }

    /**
     * ID 값을 통하여 이벤트를 지우도록 한다. View 상에서 UI 만 삭제되기 때문에
     * 모델 상의 데이터는 따로 삭제 해주어야 한다.
     *
     * @param id 이벤트의 ID 값
     */
    fun removeScheduleById(id: String){
        rects.removeIf { it.originalEvent.id == id }

        invalidate()
    }

    /**
     * 스케쥴을 등록한다
     *
     * @param event UI 로 사용할 이벤트를 넣는다.
     */
    fun addSchedule(event: WeekEvent){
        if(event.startTimeInMillis > event.endTimeInMillis)
            throw IllegalArgumentException("시작시간이 끝 시간보다 큼")

        val list = sortAndSplitSchedules(listOf(event))

        dispatchCommonEvents(list.toMutableList())

        invalidate()
    }

    /**
     * 현재 UI에 보여지는 연/월/년 기준 주 수 를 전달 해 준다.
     */
    fun getCurrentDate() = current.run { listOf(year, monthValue, weekOfYear()) }

    private fun prepareScheduleInternal(start: LocalDateTime, end: LocalDateTime){
        if(isEditMode){
            clearEditMode()
        }

        isEditMode = true

        // 오늘 기준 이전일 이면 롱클릭 편집을 하지 못하도록 한다.
        if(today.isBeforeDay(start)){
            isEditMode = false

            return
        }

        editEvent = DummyWeekEvent().apply {
            title = editEventText

            setBackgroundColor(editEventColor)
            setStartAndEndDate(start, end)
        }

        showScheduleInsertion(start.toLocalDate())
        positioningTempEventRectOffset(listOf(editEvent!!))
    }

    private fun adjustScheduleStartOrEnd(times: Long, isStartOrEnd: Boolean)
            = adjustScheduleStartOrEnd(times.toLocalDateTime(), isStartOrEnd)

    private fun adjustScheduleStartOrEnd(ldt: LocalDateTime, isStartOrEnd: Boolean)
            = ldt.run { withTime(if(isStartOrEnd) hour else hour + 1, 0, 0) }

    private fun animatorHighlight() : Animator {
        val anim1 = ValueAnimator.ofArgb(Color.TRANSPARENT, editEventColor).apply {
            addUpdateListener { editEvent!!.setBackgroundColor(it.animatedValue as Int) }
        }

        val anim2 = ValueAnimator.ofArgb(Color.TRANSPARENT, highlightPaint.color).apply {
            addUpdateListener { highlightPaint.color = it.animatedValue as Int }
        }

        val anim3 = ValueAnimator.ofArgb(Color.TRANSPARENT, highlightStrokePaint.color).apply {
            addUpdateListener { highlightStrokePaint.color = it.animatedValue as Int }
        }

        val anim4 = ValueAnimator.ofArgb(Color.TRANSPARENT, 0x80FFFFFF.toInt()).apply {
            addUpdateListener { editEventDimPaint.color = it.animatedValue as Int; invalidate() }
        }

        val animSet = AnimatorSet().apply {
            duration = 250
            interpolator = DecelerateInterpolator()

            playTogether(anim1, anim2, anim3, anim4)
        }

        return animSet
    }

    interface OnWeekViewListener {
        /**
         * View 에 데이터를 전달하는 부분으로, 기본적으로 전/이번/다음 주의 데이터를 불러오게 되어있다.
         * 값은 항상 해당 주 첫번째 일의 연,월,일,주수를 전달해 준다.
         *
         * @param year 해당 주 첫번째 일의 연도
         * @param month 해당 주 첫번째 일의 월 (+1 하지 않아도 됌)
         * @param date 해당 주 첫번째 일의 일자
         * @param week 해당 주의 연 기준 주일 수
         */
        fun onWeekChanged(year: Int, month: Int, date: Int, week: Int) : List<WeekEvent>?

        /**
         * 선택된 이벤트에 대한 데이터를 넘겨준다
         *
         * @param event UI 에서 사용하는 이벤트 데이터
         */
        fun onWeekEventSelected(event: WeekEvent)

        /**
         * 비어있는 이벤트에 대해서 추가하고자 할 때 사용된다. 해당 메소드는 기본적으로 롱클릭으로 실행된다.
         * 넘겨주는 데이터는 롱클릭이 일어난 부분의 start 시간과 end 시간을 milliseconds 값으로 알려주게 된다.
         *
         * @param start 시작시간
         * @param end 종료시간
         */
        fun onEmptyEventWillBeAdded(start: Long, end: Long)

        /**
         * 주가 변경될 때마다 불리는 메소드로, @see[onWeekChanged] 와는 다르게 현재의 주만 뿌려준다.
         *
         * @param year 해당 주 첫번째 일의 연도
         * @param month 해당 주 첫번째 일의 월 (+1 하지 않아도 됌)
         * @param date 해당 주 첫번째 일의 일자
         * @param week 해당 주의 연 기준 주일 수
         */
        fun onCurrentWeekChanged(year: Int, month: Int, date: Int, week: Int)
    }
}