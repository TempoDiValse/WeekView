package kr.lavalse.kweekview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewConfigurationCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import kr.lavalse.kweekview.extension.ECalendar.formattedText
import kr.lavalse.kweekview.extension.ECalendar.isPastDay
import kr.lavalse.kweekview.extension.ECalendar.isSameDay
import kr.lavalse.kweekview.extension.ECalendar.isSameWeek
import kr.lavalse.kweekview.extension.ECalendar.toMinute
import kr.lavalse.kweekview.extension.EContext.toDP
import kr.lavalse.kweekview.extension.EContext.toSP
import kr.lavalse.kweekview.model.WeekEvent
import kr.lavalse.kweekview.model.WeekRect
import java.lang.Float.max
import java.lang.Float.min
import java.lang.Math.round
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

class WeekView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DEFAULT_VISIBLE_DAYS = 7
        private const val DEFAULT_PRELOAD_WEEK_DATA_RANGE = 3

        private const val DEFAULT_TEXT_SIZE = 12f
        private const val DEFAULT_GRID_SEPARATOR_COLOR = Color.LTGRAY
        private const val DEFAULT_GRID_SEPARATOR_STROKE_WIDTH = .3f

        private const val DEFAULT_TIMELINE_ROW_HEIGHT = 100f
        private const val DEFAULT_TIMELINE_HORIZONTAL_PADDING = 10f

        private const val DEFAULT_TIME_NOW_COLOR = 0x355AEF
        private const val DEFAULT_TIME_NOW_STROKE_WIDTH = .5f

        private const val DEFAULT_DAY_OF_WEEK_VERTICAL_PADDING = 20f
        private const val DEFAULT_DAY_OF_WEEK_TEXT_COLOR = 0xFF333333
        private const val DEFAULT_DAY_OF_WEEK_TODAY_TEXT_COLOR = 0xFF55AAFF
        private const val DEFAULT_DAY_OF_WEEK_SUNDAY_TEXT_COLOR = 0xFFFF0011
        private const val DEFAULT_DAY_OF_WEEK_SATURDAY_TEXT_COLOR = 0xFF4682F1

        private const val DEFAULT_ALLDAY_AREA_HEIGHT = 100f

        private const val DEFAULT_DAY_BACKGROUND_COLOR = Color.WHITE
        private const val DEFAULT_TODAY_BACKGROUND_COLOR = 0xFFC7F1FF

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

        const val X_SPEED = .8f
        const val X_AUTO_SCROLL_DURATION = 250
    }

    private val today = Calendar.getInstance()

    private var current : Calendar = (today.clone() as Calendar).apply {
        minimalDaysInFirstWeek = 7
    }
    private var before : Calendar? = null

    private val rects: MutableList<WeekRect> = mutableListOf()

    // x, y 좌표
    private val origin: PointF = PointF(0f, 0f)

    private var painter = Paint(Paint.ANTI_ALIAS_FLAG)

    private var gridSeparatorColor = Color.WHITE
    private var gridSeparatorStrokeWidth = .0f
    private val gridSeparatorPaint : Paint = Paint()

    private var widthPerDay: Int = 0
    private var visibleDays: Int = DEFAULT_VISIBLE_DAYS

    private var textSize = 0

    private var timelineTextSize = 0
    private var timelineWidth = 0
    private var hourHeight = 0
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

    private var allDayEventTextSize = 0
    private var allDayAreaHeight = 0

    private var dayBackgroundColor = 0
    private var dayBackgroundPaint = Paint()
    private var todayBackgroundColor = 0
    private var todayBackgroundPaint = Paint()

    private val headerHeight get() = dayOfWeekHeight + allDayAreaHeight

    private var scaledTouchSlop = 0
    private var minimumFlingVelocity = 0

    private var currentScrollDirection = Scroll.NONE
    private var currentFlingDirection = currentScrollDirection

    private var listener : OnWeekChangeListener? = null
    private var scroller = OverScroller(context, FastOutLinearInInterpolator())
    private var gestureDetector: GestureDetectorCompat
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)

            scrollToNearestOrigin()

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            println("[onSingleTapConfirmed]")

            return super.onSingleTapConfirmed(e)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            val didScrollHorizontal = abs(dx) > abs(dy)

            when(currentScrollDirection){
                Scroll.NONE ->
                    currentScrollDirection = when(didScrollHorizontal){
                        true -> if(dx > 0) Scroll.LEFT else Scroll.RIGHT
                        else -> Scroll.VERTICAL
                    }

                Scroll.LEFT -> {
                    if(didScrollHorizontal && dx < -scaledTouchSlop)
                        currentScrollDirection = Scroll.RIGHT

                    loadSchedules()
                }

                Scroll.RIGHT -> {
                    if(didScrollHorizontal && dx > scaledTouchSlop)
                        currentScrollDirection = Scroll.LEFT

                    loadSchedules()
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
            if(arrayOf(Scroll.LEFT, Scroll.RIGHT, Scroll.VERTICAL).contains(currentFlingDirection))
                return true

            scroller.forceFinished(true)

            currentFlingDirection = currentScrollDirection

            val minY = -((hourHeight * 24) + headerHeight - height)
            when(currentFlingDirection){
                Scroll.LEFT, Scroll.RIGHT ->
                    scroller.fling(origin.x.toInt(), origin.y.toInt(),
                        (vx * Scroll.X_SPEED).toInt(), 0,
                        Int.MIN_VALUE, Int.MAX_VALUE,
                        minY, 0)

                Scroll.VERTICAL ->
                    scroller.fling(origin.x.toInt(), origin.y.toInt(),
                        0, vy.toInt(),
                        Int.MIN_VALUE, Int.MAX_VALUE,
                        minY, 0)
            }

            ViewCompat.postInvalidateOnAnimation(this@WeekView)

            return true
        }
    }

    private var didAttach = false

    init {
        with(context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, defStyleAttr, 0)){
            textSize = getDimensionPixelSize(R.styleable.WeekView_android_textSize,
                context.toSP(DEFAULT_TEXT_SIZE).toInt())

            // Timeline
            hourHeight = getDimensionPixelSize(R.styleable.WeekView_timelineRowHeight,
                context.toDP(DEFAULT_TIMELINE_ROW_HEIGHT).toInt())
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
            allDayEventTextSize = getDimensionPixelSize(R.styleable.WeekView_allDayEventTextSize, textSize)
            allDayAreaHeight = getDimensionPixelSize(R.styleable.WeekView_allDayAreaHeight,
                context.toDP(DEFAULT_ALLDAY_AREA_HEIGHT).toInt())

            // Day
            dayBackgroundColor = getColor(R.styleable.WeekView_dayBackgroundColor, DEFAULT_DAY_BACKGROUND_COLOR.toInt())
            todayBackgroundColor = getColor(R.styleable.WeekView_todayBackgroundColor, DEFAULT_TODAY_BACKGROUND_COLOR.toInt())

            gridSeparatorColor = getColor(R.styleable.WeekView_gridSeparatorColor, DEFAULT_GRID_SEPARATOR_COLOR)
            gridSeparatorStrokeWidth = getDimension(R.styleable.WeekView_gridSeparatorStrokeWidth,
                context.toDP(DEFAULT_GRID_SEPARATOR_STROKE_WIDTH))

            recycle()
        }

        gestureDetector = GestureDetectorCompat(context, gestureListener)
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

        timelineTextPaint.textSize = timelineTextSize.toFloat()

        dayBackgroundPaint.color = dayBackgroundColor
        todayBackgroundPaint.color = todayBackgroundColor

        measureTimeline()
        measureDayOfWeekHeight()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if(!didAttach){
            loadSchedules()

            didAttach = true
        }
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

        val firstDayOfWeek = (today.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        drawTimeline(canvas)
        drawGrid(canvas, firstVisibleIndex, firstDayOfWeek, startX)

        for(drawWhat in 0 until 2){
            for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
                val date = (firstDayOfWeek.clone() as Calendar).apply {
                    add(Calendar.DATE, index - 1)
                }

                val dt = when {
                    date.isPastDay(today) -> DayType.PAST
                    date.isSameDay(today) -> DayType.TODAY
                    else -> DayType.COMMON
                }

                when(drawWhat){
                    0 -> {
                        drawWeekOfDay(canvas, date, offsetX, dt)
                        drawCommonEvent(canvas, date, offsetX, dt)
                    }

                    1 -> {
                        drawAllDayEvent(canvas, date, offsetX, dt)
                    }
                }


                offsetX += widthPerDay

                if(index == firstVisibleIndex + 1){
                    current = date.clone() as Calendar
                }
            }

            offsetX = startX
        }

        //canvas?.clipRect(0, headerHeight, width, height)
    }

    private fun loadSchedules(){
        val isNotSame = before?.isSameWeek(current) ?: false

        if(!isNotSame){
            val schedules = mutableListOf<WeekEvent>()

            with(current.clone() as Calendar){
                add(Calendar.WEEK_OF_YEAR, -1)

                for(day in 0 until DEFAULT_PRELOAD_WEEK_DATA_RANGE){
                    val pool = listener?.onWeekChanged(
                        get(Calendar.YEAR),
                        get(Calendar.MONTH) + 1,
                        get(Calendar.DATE),
                        get(Calendar.WEEK_OF_YEAR),
                    ) ?: listOf()

                    sortAndCachedSchedules(pool, schedules)

                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            dispatchEvents(schedules)

            before = current.clone() as Calendar
        }

        // Rect 가 계속 쌓이는 것을 방지한다
        if(didAttach)
            invalidate()
    }

    private fun sortAndCachedSchedules(events: List<WeekEvent>, cached: MutableList<WeekEvent>) {
        sortEvent(events)

        events
            .filter { it.startTimeInMillis < it.endTimeInMillis }
            .forEach {
                cached.addAll(splitEventPerDay(it))
            }
    }

    private fun splitEventPerDay(event: WeekEvent) : List<WeekEvent> {
        val events = mutableListOf<WeekEvent>()

        // 스케쥴이 하루에 끝나지 않는 경우에는 스케쥴을 일 단위로 분리를 시켜줘야 한다
        if(!event.run { startAt!!.isSameDay(endAt!!) }){
            with(event){
                val ends = (startAt!!.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }

                // 첫날은 시작일 ~ 해당일 23:59:59 까지 이벤트를 진행하도록 설정한다
                events.add(WeekEvent.copyDescription(this).also {
                    it.setStartAndEndDate(startAt!!, ends, isAllDay())
                })

                // 시작일 다음으로 달력의 커서를 변경하고,
                val other = (startAt!!.clone() as Calendar).apply {
                    add(Calendar.DATE, 1)
                }

                // 그 다음날 부터는 마지막 스케쥴 날짜와 같아지기 전까지 다음의 행위를 반복한다.
                // 시작일 00:00:00 시작일의 종료일 23:59:59 까지 설정하고 반복
                while(!other.isSameDay(endAt!!)){
                    val nextDay = (other.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }

                    val endOfNextDay = (nextDay.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }

                    events.add(WeekEvent.copyDescription(this).also {
                        it.setStartAndEndDate(nextDay, endOfNextDay, isAllDay())
                    })

                    other.add(Calendar.DATE, 1)
                }

                // 마지막 날은 시작 일을 마지막 날의 00:00:00 에 설정 하고서
                // 마지막 날과 함께 이벤트 객체를 만들어 삽입 하도록 한다.
                val lastDay = (endAt!!.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)

                    add(Calendar.MILLISECOND, -1)
                }

                events.add(WeekEvent.copyDescription(this).also {
                    it.setStartAndEndDate(lastDay, endAt!!, isAllDay())
                })
            }
        }else{
            // 스케쥴 분리가 필요 없는 것은 그대로 넣어 주도록 한다.
            events.add(event.apply {
                endAt!!.add(Calendar.MILLISECOND, -1)
            })
        }

        return events
    }

    private fun dispatchEvents(events: MutableList<WeekEvent>){
        if(rects.isNotEmpty())
            rects.clear()

        while(events.size > 0){
            val groups = mutableListOf<WeekEvent>()

            val e1 = events.removeFirst()
            groups.add(e1)

            var i = 0
            while(i < events.size){
                val e2 = events[i]

                // 겹치는 일정 들을 묶어 보내도록 한다.
                // 조건 1. 이벤트 들이 서로 종일 이면서 일정이 겹치는 경우 (종일 이벤트가 타고 들어감)
                // 조건 2. 같은 날에 이벤트가 이뤄지는 경우 (일반 이벤트가 타고 들어감)
                if(e1.isAllDay() == e2.isAllDay() && isEventCollide(e1, e2)
                    || e1.startAt!!.isSameDay(e2.startAt!!)){
                    events.removeAt(i)
                    groups.add(e2)
                }else{
                    i++
                }
            }

            overlapEvents(groups)
        }
    }

    private fun overlapEvents(events: List<WeekEvent>){
        val collisions = mutableListOf<MutableList<WeekEvent>>()

        for(e in events){
            var isPlaced = false

            for(collides in collisions){
                for(ge in collides){
                    if(isEventCollide(ge, e) && ge.isAllDay() == e.isAllDay()){
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

        //println(collisions)

        collisions.forEach(::positioningEventRectOffset)
    }

    private fun positioningEventRectOffset(group: MutableList<WeekEvent>){
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

        // 최대 Span 의 갯수를 알아낸다
        val spanCount = spans.maxOf { it.size }
        for(i in 0 until spanCount){
            var j = 0f

            for(span in spans){
                if(span.size - 1 >= i){
                    with(span[i]){
                        if(!isAllDay()){
                            val l = j / spans.size
                            val t = startAt!!.toMinute() * 1f
                            val r = 1f / spans.size
                            val b = endAt!!.toMinute() * 1f

                            val rect = WeekRect(l, t, r, b, this)

                            rects.add(rect)
                        }
                    }
                }

                j++
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
        val temp = today.clone() as Calendar

        for(i in 0 until 24){
            val str = temp.apply {
                set(Calendar.HOUR_OF_DAY, i)
                set(Calendar.MINUTE, 0)
            }.formattedText()

            timelineTextWidth = timelineTextWidth.coerceAtLeast(timelineTextPaint.measureText(str).toInt())

            if(i == 23)
                timelineTextHeight = Rect().apply { timelineTextPaint.getTextBounds(str, 0, str.length, this) }.height()
        }

        timelineWidth = timelineTextWidth + (timelineHorizontalPadding * 2)
    }

    private fun measureDayOfWeekHeight(){
        val temp = today.clone() as Calendar

        dayOfWeekTextHeight = temp.formattedText("dd E").run {
            Rect().also { r -> dayOfWeekTextPaint.getTextBounds(this, 0, length, r) }.height()
        }
    }

    private fun drawTimeline(canvas: Canvas?){
        canvas?.run {
            save()

            val date = today.clone() as Calendar

            clipRect(0f, headerHeight - (timelineTextHeight / 2f), timelineWidth * 1f, height * 1f)

            for(i in 0 until 24){
                val str = date.apply {
                    set(Calendar.HOUR_OF_DAY, i)
                    set(Calendar.MINUTE, 0)
                }.formattedText()

                val top = origin.y + headerHeight + (hourHeight * i) + (timelineTextHeight / 2)

                if(top < height)
                    drawText(str, (timelineTextWidth + timelineHorizontalPadding).toFloat(), top, timelineTextPaint)
            }

            restore()
        }
    }

    private fun drawGrid(canvas: Canvas?, firstVisibleIndex: Int, firstDayOfWeek: Calendar, startX: Float){
        canvas?.run {
            save()

            val lines = ((height - headerHeight) / hourHeight + 1) * (visibleDays + 1)
            val (h, v) = FloatArray(lines * 4) to FloatArray(lines * 4)

            var (offsetX, offsetY) = startX to headerHeight
            var x1 = .0f
            var x2 = .0f
            var y1 = .0f
            var y2 = .0f

            for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
                val date = (firstDayOfWeek.clone() as Calendar).apply {
                    add(Calendar.DATE, index - 1)
                }

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

    private fun drawWeekOfDay(canvas: Canvas?, date: Calendar, offsetX: Float, dayType: Int){
        canvas?.run {
            save()

            clipRect(timelineWidth, 0, width, dayOfWeekHeight)

            val (x, y) = offsetX + (widthPerDay / 2) to dayOfWeekHeight - (dayOfWeekVerticalPadding / 2f)

            val str = date.formattedText("dd E")

            when(date.get(Calendar.DAY_OF_WEEK)){
                Calendar.SUNDAY -> canvas.drawText(str, x, y, dayOfWeekSundayTextPaint)
                Calendar.SATURDAY -> canvas.drawText(str, x, y, dayOfWeekSaturdayTextPaint)
                else -> {
                    when(dayType){
                        DayType.TODAY -> canvas.drawText(str, x, y, dayOfWeekTodayTextPaint)
                        else -> canvas.drawText(str, x, y, dayOfWeekTextPaint)
                    }
                }
            }

            restore()
        }
    }

    private fun drawCommonEvent(canvas: Canvas?, date: Calendar, offsetX: Float, type: Int){
        canvas?.run {
            save()

            val left = offsetX.coerceAtLeast(timelineWidth.toFloat())
            val top = headerHeight

            drawCommonRect(this, date, offsetX, type)

            if(type == DayType.TODAY){
                val barOffset = top + origin.y
                val beforeNow = Calendar.getInstance().run { (get(Calendar.HOUR_OF_DAY) + (get(Calendar.MINUTE) / 60f)) * hourHeight }

                drawLine(left, barOffset + beforeNow, offsetX + widthPerDay, barOffset + beforeNow, timeNowPaint)
            }

            restore()
        }
    }

    private fun drawCommonRect(canvas: Canvas?, date: Calendar, startX: Float, type: Int){
        if(rects.isEmpty()) return

        canvas?.run {
            clipRect(timelineWidth, headerHeight, width, height)

            for (rect in rects){
                if(!date.isSameDay(rect.startAt)) continue

                val offsetY = origin.y + headerHeight

                val l = startX + (rect.left * widthPerDay)
                val t = offsetY + ((hourHeight * 24 * rect.top) / 1440)
                val r = l + (rect.right * widthPerDay)
                val b = offsetY + ((hourHeight * 24 * rect.bottom) / 1440)

                if(l < r
                    && l < width && t < height
                    && r > timelineWidth && b > headerHeight){
                    painter.color = rect.backgroundColor

                    drawRoundRect(l, t, r, b, 10f, 10f, painter)
                }
            }
        }
    }

    private fun drawAllDayEvent(canvas: Canvas?, date: Calendar, offsetX: Float, type: Int){
        canvas?.run {
            save()

            val left = offsetX.coerceAtLeast(timelineWidth.toFloat())
            val top = dayOfWeekHeight

            drawText("종일", timelineTextWidth * 1f, (headerHeight / 2) + (timelineTextHeight * 1f), timelineTextPaint)

            drawLine(left, headerHeight * 1f, width * 1f, headerHeight * 1f, gridSeparatorPaint)

            restore()
        }
    }

    private fun isEventCollide(e1: WeekEvent, e2: WeekEvent)
        = e1.startTimeInMillis <= e2.endTimeInMillis
            && e2.startTimeInMillis <= e1.endTimeInMillis

    override fun onTouchEvent(event: MotionEvent?): Boolean
        = event?.run {
            gestureDetector.onTouchEvent(this)

            if(action == MotionEvent.ACTION_UP && currentFlingDirection == Scroll.NONE){
                if(currentScrollDirection.let { it == Scroll.LEFT || it == Scroll.RIGHT}){
                    scrollToNearestOrigin()
                }

                currentScrollDirection = Scroll.NONE
            }

            true
        } ?: super.onTouchEvent(event)

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

    private fun scrollToNearestOrigin(){
        var days = origin.x / widthPerDay
        days = when {
            currentScrollDirection == Scroll.LEFT -> floor(days)
            currentScrollDirection == Scroll.RIGHT -> ceil(days)
            currentFlingDirection != Scroll.NONE -> kotlin.math.round(days)
            else -> kotlin.math.round(days)
        }

        val nearestOrigin = origin.x - (days * widthPerDay)
        if(nearestOrigin != 0f){
            scroller.forceFinished(true)
            scroller.startScroll(
                origin.x.toInt(), origin.y.toInt(),
                -nearestOrigin.toInt(), 0,
                ((abs(nearestOrigin) / widthPerDay) * Scroll.X_AUTO_SCROLL_DURATION).toInt()
            )

            ViewCompat.postInvalidateOnAnimation(this)
        }

        with(Scroll.NONE){
            currentScrollDirection = this
            currentFlingDirection = this
        }
    }

    var onWeekChangeListener: OnWeekChangeListener?
        set(value) { this.listener = value }
        get() = this.listener

    interface OnWeekChangeListener {
        fun onWeekChanged(year: Int, month: Int, date: Int, week: Int) : List<WeekEvent>?
    }
}