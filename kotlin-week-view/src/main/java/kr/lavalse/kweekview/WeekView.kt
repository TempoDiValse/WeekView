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
import kr.lavalse.kweekview.exception.*
import kr.lavalse.kweekview.extension.EContext.toDP
import kr.lavalse.kweekview.extension.EContext.toSP
import kr.lavalse.kweekview.extension.ELocalDateTime.isBeforeDay
import kr.lavalse.kweekview.extension.ELocalDateTime.isBeforeWeek
import kr.lavalse.kweekview.extension.ELocalDateTime.isSameDay
import kr.lavalse.kweekview.extension.ELocalDateTime.toLocalDateTime
import kr.lavalse.kweekview.extension.ELocalDateTime.toMinuteOfHours
import kr.lavalse.kweekview.extension.ELocalDateTime.toText
import kr.lavalse.kweekview.extension.ELocalDateTime.toTimeMillis
import kr.lavalse.kweekview.extension.ELocalDateTime.weekOfYear
import kr.lavalse.kweekview.extension.ELocalDateTime.withDayOfWeek
import kr.lavalse.kweekview.extension.ELocalDateTime.withTime
import kr.lavalse.kweekview.model.DummyWeekEvent
import kr.lavalse.kweekview.model.WeekEvent
import kr.lavalse.kweekview.model.WeekRect
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil

class WeekView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DEFAULT_FORMAT_DAY_OF_WEEK = "d"

        private const val DEFAULT_VISIBLE_DAYS = 7
        private const val DEFAULT_DAY_OF_WEEK_START = -1
        private const val DEFAULT_PRELOAD_WEEK_DATA_RANGE = 3
        private const val DEFAULT_FLING_MAX_LIMIT = 10000f

        private const val DEFAULT_TEXT_SIZE = 12f

        private const val DEFAULT_LINE_COLOR = 0x0f3C3C43
        private const val DEFAULT_LINE_STROKE_WIDTH = 1f

        private const val DEFAULT_HOUR_HEIGHT = 56f
        private const val DEFAULT_HOUR_HEIGHT_MIN = 30f
        private const val DEFAULT_HOUR_HEIGHT_MAX = 70f

        private const val DEFAULT_TIMELINE_LEFT_PADDING = 8f
        private const val DEFAULT_TIMELINE_RIGHT_PADDING = 4f
        private const val DEFAULT_TIMELINE_TEXT_SIZE = 12f
        private const val DEFAULT_TIMELINE_TEXT_COLOR = 0xFF8B8B8F

        private const val DEFAULT_TIME_NOW_COLOR = 0x355AEF
        private const val DEFAULT_TIME_NOW_STROKE_WIDTH = .5f

        private const val DEFAULT_DAY_OF_WEEK_TOP_PADDING = 7f
        private const val DEFAULT_DAY_OF_WEEK_BOTTOM_PADDING = 2f
        private const val DEFAULT_DAY_OF_WEEK_TEXT_SPACE = 3f

        private const val DEFAULT_DAY_OF_WEEK_TEXT_NUMBER_SIZE = 16f
        private const val DEFAULT_DAY_OF_WEEK_TEXT_NUMBER_COLOR = 0xFF111111
        private const val DEFAULT_DAY_OF_WEEK_TEXT_COLOR = 0xFF8B8B8F
        private const val DEFAULT_DAY_OF_WEEK_TODAY_TEXT_COLOR = 0xFFFFFFFF
        private const val DEFAULT_DAY_OF_WEEK_TODAY_BACKGROUND_COLOR = 0xFF4D7CFE
        private const val DEFAULT_DAY_OF_WEEK_TODAY_BACKGROUND_SIZE = 30f
        private const val DEFAULT_DAY_OF_WEEK_SUNDAY_TEXT_COLOR = 0xFFFC4C60
        private const val DEFAULT_DAY_OF_WEEK_SATURDAY_TEXT_COLOR = 0xFF4D7CFE

        private const val DEFAULT_ALLDAY_TEXT = "종일"

        private const val DEFAULT_DAY_BACKGROUND_COLOR = Color.TRANSPARENT
        private const val DEFAULT_TODAY_BACKGROUND_COLOR = Color.TRANSPARENT
        private const val DEFAULT_PAST_EVENT_COLOR = Color.LTGRAY

        private const val DEFAULT_EVENT_CORNER_RADIUS = 3f
        private const val DEFAULT_EVENT_BLOCK_ROUND_CEIL = 10f
        private const val DEFAULT_EVENT_BLOCK_PADDING = 1f
        private const val DEFAULT_EVENT_BLOCK_EMPTY_BACKGROUND = 0xFFF7F8FC
        private const val DEFAULT_EVENT_TEXT_HORIZONTAL_PADDING = 6f
        private const val DEFAULT_EVENT_TEXT_VERTICAL_PADDING = 8f
        private const val DEFAULT_EVENT_TEXT_COLOR = 0xFF111111
        private const val DEFAULT_EVENT_TEXT_MAX_LINES = 2f

        private const val DEFAULT_EDIT_EVENT_DIM_ALPHA = 0xBF
        private const val DEFAULT_EDIT_EVENT_STROKE_WIDTH = 1f
        private const val DEFAULT_EDIT_EVENT_STROKE_COLOR = 0xFFFFFFFF
        private const val DEFAULT_EDIT_EVENT_SHADOW_COLOR = 0x4D000000

        private const val DEFAULT_HIGHLIGHT_COLOR = 0xFF4D7CFE
        private const val DEFAULT_HIGHLIGHT_BACKGROUND_ALPHA = 25
        private const val DEFAULT_HIGHLIGHT_STROKE_WIDTH = 1f

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
    private var startDayOfWeek = -1

    private val rects: MutableList<WeekRect> = mutableListOf()

    // x, y 좌표
    private val origin: PointF = PointF(0f, 0f)
    private var prevX: Float = 0f

    private var eventTextHorizontalPadding = 0f
    private var eventTextVerticalPadding = 0f
    private var eventCornerRadius = 0f
    private var eventBlockPadding = 0f
    private var painter = Paint(Paint.ANTI_ALIAS_FLAG)
    private var eventEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_EVENT_BLOCK_EMPTY_BACKGROUND.toInt()
    }

    private var pastEventColor = Color.WHITE

    private var editEventText : String = ""
    private var editEventColor = Color.WHITE
    private var editEventStrokeColor = Color.WHITE
    private var editEventStrokeWidth = 0f
    private var editEventShadowColor = Color.TRANSPARENT
    private val editEventDimPaint : Paint = Paint()
    private val editEventStrokePaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private var lineColor = Color.WHITE
    private var lineAllDayColor = Color.WHITE
    private var lineStrokeWidth = 0f
    private val linePaint : Paint = Paint()

    private var widthPerDay: Float = 0f
    private var visibleDays: Int = DEFAULT_VISIBLE_DAYS

    private var textSize = 0
    private var eventTextColor = 0
    private var eventTextSize = 0
    private var eventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var timelineTextSize = 0f
    private var timelineTextColor = Color.BLACK
    private var timelineAllDayTextColor = Color.BLACK
    private val timelineWidth
        get() =  timelinePadding.left + timelineTextWidth + timelinePadding.right

    private var hourHeight = 0
    private var hourMinHeight = 0
    private var hourMaxHeight = 0
    private var timelineTextWidth = 0
    private var timelineTextHeight = 0
    private var timelinePadding = Rect()
    private val timelineTextPaint : TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.RIGHT
    }

    private var timeNowColor = 0
    private var timeNowStrokeWidth = .0f
    private val timeNowPaint : Paint = Paint()

    private var dayOfWeekTextSize = 0
    private var dayOfWeekPadding : Rect = Rect()
    private var dayOfWeekTextSpace = 0
    private val dayOfWeekHeight
        get() = dayOfWeekPadding.top + dayOfWeekTextHeight + dayOfWeekTextSpace + dayOfWeekTodayBackgroundSize + dayOfWeekPadding.bottom

    private var dayOfWeekTextHeight = 0
    private var dayOfWeekTextColor = 0
    private var dayOfWeekTextNumberSize = 0
    private var dayOfWeekTextNumberHeight = 0
    private var dayOfWeekTextNumberColor = Color.WHITE

    private var dayOfWeekTodayTextColor = 0
    private var dayOfWeekTodayBackgroundSize = 0
    private var dayOfWeekTodayBackgroundColor = 0

    private var dayOfWeekSundayTextColor = 0
    private var dayOfWeekSaturdayTextColor = 0
    private val dayOfWeekTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekTextNumberPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekTodayTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekTodayBackgroundPaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dayOfWeekSundayTextPaint : Paint = getDefaultDayOfWeekPaint()
    private val dayOfWeekSaturdayTextPaint : Paint = getDefaultDayOfWeekPaint()

    private var timelineAllDayEventText = ""
    private var timelineAllDayTextWidth = 0
    private var timelineAllDayEventTextSize = 0
    private var allDayAreaHeight = 0

    private var dayBackgroundColor = 0
    private var dayBackgroundPaint = Paint()
    private var todayBackgroundColor = 0
    private var todayBackgroundPaint = Paint()

    private val headerHeight
        get() = (dayOfWeekHeight + allDayAreaHeight + (lineStrokeWidth * 2)).toInt()

    private val highlightRect = RectF()
    private var highlightColor = Color.WHITE
    private var highlightPaint = Paint()
    private var highlightStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val highlightEmphasisDayOfWeekTextPaint = getDefaultDayOfWeekPaint()
    private val highlightEmphasisTimelineTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }
    private var highlightStrokeWidth = 0f

    private var scaledTouchSlop = 0
    private var minimumFlingVelocity = 0

    private var currentScrollDirection = Scroll.NONE
    private var currentFlingDirection = currentScrollDirection

    private var isEditMode = false
    private var isLongPressed = false
    private var isDrawn = false

    /** 현재일 이전 데이터로 이동이 필요한 경우, true 설정 하도록 한다. */
    private var canMovePastDate = false
    private var canEditLongClick = false
    private var needTimeNow = false

    private var listener : OnWeekViewListener? = null

    private var editEvent : DummyWeekEvent? = null

    private var scroller = OverScroller(context, DecelerateInterpolator())
    private var detector: GestureDetectorCompat
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)

            scrollToNearestOrigin()

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if(!canEditLongClick){
                if(isEditMode){
                    val (start, end) = getTimeFromPoint(e.x, e.y).run {
                        adjustScheduleStartOrEnd(this, true) to adjustScheduleStartOrEnd(this, false)
                    }

                    if(editEvent!!.run { startAt == start && endAt == end }){
                        dismissEditSchedule()

                        return true
                    }
                }

                changeWithEditMode(e)

                return super.onSingleTapConfirmed(e)
            }

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
            if(isZooming) return true

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
                    val velocity = vx.coerceIn(-DEFAULT_FLING_MAX_LIMIT, DEFAULT_FLING_MAX_LIMIT) * Scroll.X_SPEED

                    scroller.fling(
                        origin.x.toInt(), origin.y.toInt(),
                        velocity.toInt(), 0,
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
            if(e.y < headerHeight) {
                sendError(CantTouchAllDayException())

                return
            }

            if(!isLongPressed)
                isLongPressed = true

            if(canEditLongClick)
                changeWithEditMode(e)
        }

        private fun changeWithEditMode(e: MotionEvent){
            if(e.y < headerHeight) {
                sendError(CantTouchAllDayException())

                return
            }

            if(e.x < timelineWidth) return

            val (start, end) = getTimeFromPoint(e.x, e.y).run {
                adjustScheduleStartOrEnd(this, true) to adjustScheduleStartOrEnd(this, false)
            }

            prepareScheduleInternal(start, end)

            if(animator != null){
                invalidate()
            }else{
                animator = animatorHighlight()
                animator?.start()
            }
        }
    }

    private var isZooming = false
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val minHeightOnScreen = kotlin.math.max(ceil((height - headerHeight) / 24f).toInt(), hourMinHeight)
            val mH = (hourHeight * detector.scaleFactor).toInt().coerceIn(minHeightOnScreen, hourMaxHeight)

            hourHeight = mH

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
            // Common
            textSize = getDimensionPixelSize(R.styleable.WeekView_android_textSize,
                context.toSP(DEFAULT_TEXT_SIZE).toInt())
            canMovePastDate = getBoolean(R.styleable.WeekView_canMovePastDate, canMovePastDate)
            startDayOfWeek = getInt(R.styleable.WeekView_startDayOfWeek, DEFAULT_DAY_OF_WEEK_START)

            // dayOfWeek
            dayOfWeekPadding.top = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekTopPadding,
                context.toDP(DEFAULT_DAY_OF_WEEK_TOP_PADDING).toInt())
            dayOfWeekPadding.bottom = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekBottomPadding,
                context.toDP(DEFAULT_DAY_OF_WEEK_BOTTOM_PADDING).toInt())
            dayOfWeekTextSpace = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekTextSpace,
                context.toDP(DEFAULT_DAY_OF_WEEK_TEXT_SPACE).toInt())
            dayOfWeekTextNumberSize = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekTextNumberSize,
                context.toDP(DEFAULT_DAY_OF_WEEK_TEXT_NUMBER_SIZE).toInt())
            dayOfWeekTextNumberColor = getColor(R.styleable.WeekView_dayOfWeekTextNumberColor, DEFAULT_DAY_OF_WEEK_TEXT_NUMBER_COLOR.toInt())
            dayOfWeekTextSize = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekTextSize, textSize)
            dayOfWeekTextColor = getColor(R.styleable.WeekView_dayOfWeekTextColor, DEFAULT_DAY_OF_WEEK_TEXT_COLOR.toInt())
            dayOfWeekTodayTextColor = getColor(R.styleable.WeekView_dayOfWeekTodayTextColor, DEFAULT_DAY_OF_WEEK_TODAY_TEXT_COLOR.toInt())
            dayOfWeekTodayBackgroundSize = getDimensionPixelSize(R.styleable.WeekView_dayOfWeekTodayBackgroundSize,
                context.toDP(DEFAULT_DAY_OF_WEEK_TODAY_BACKGROUND_SIZE).toInt())
            dayOfWeekTodayBackgroundColor = getColor(R.styleable.WeekView_dayOfWeekTodayBackgroundColor, DEFAULT_DAY_OF_WEEK_TODAY_BACKGROUND_COLOR.toInt())
            dayOfWeekSundayTextColor = getColor(R.styleable.WeekView_dayOfWeekSundayTextColor, DEFAULT_DAY_OF_WEEK_SUNDAY_TEXT_COLOR.toInt())
            dayOfWeekSaturdayTextColor = getColor(R.styleable.WeekView_dayOfWeekSaturdayTextColor, DEFAULT_DAY_OF_WEEK_SATURDAY_TEXT_COLOR.toInt())

            // Timeline
            hourHeight = getDimensionPixelSize(R.styleable.WeekView_hourHeight,
                context.toDP(DEFAULT_HOUR_HEIGHT).toInt())

            hourMinHeight = getDimensionPixelSize(R.styleable.WeekView_hourMinHeight,
                context.toDP(DEFAULT_HOUR_HEIGHT_MIN).toInt())
            hourMaxHeight = getDimensionPixelSize(R.styleable.WeekView_hourMaxHeight,
                context.toDP(DEFAULT_HOUR_HEIGHT_MAX).toInt())

            timelineTextSize = getDimension(R.styleable.WeekView_timelineTextSize,
                context.toDP(DEFAULT_TIMELINE_TEXT_SIZE))
            timelineTextColor = getColor(R.styleable.WeekView_timelineTextColor, DEFAULT_TIMELINE_TEXT_COLOR.toInt())
            timelinePadding.left = getDimensionPixelSize(R.styleable.WeekView_timelineLeftPadding,
                context.toDP(DEFAULT_TIMELINE_LEFT_PADDING).toInt())
            timelinePadding.right = getDimensionPixelSize(R.styleable.WeekView_timelineRightPadding,
                context.toDP(DEFAULT_TIMELINE_RIGHT_PADDING).toInt())

            timeNowColor = getColor(R.styleable.WeekView_timeNowColor, DEFAULT_TIME_NOW_COLOR)
            timeNowStrokeWidth = getDimension(R.styleable.WeekView_timeNowStrokeWidth,
                context.toDP(DEFAULT_TIME_NOW_STROKE_WIDTH))

            timelineAllDayTextColor = getColor(R.styleable.WeekView_timelineAllDayTextColor, timelineTextColor)
            timelineAllDayEventText = getString(R.styleable.WeekView_timelineAllDayEventText) ?: DEFAULT_ALLDAY_TEXT
            timelineAllDayEventTextSize = getDimensionPixelSize(R.styleable.WeekView_timelineAllDayEventTextSize, textSize)

            // Event
            eventCornerRadius = getDimension(R.styleable.WeekView_eventCornerRadius,
                context.toDP(DEFAULT_EVENT_CORNER_RADIUS))
            eventBlockPadding = getDimension(R.styleable.WeekView_eventBlockPadding,
                context.toDP(DEFAULT_EVENT_BLOCK_PADDING))
            eventTextHorizontalPadding = getDimension(R.styleable.WeekView_eventTextHorizontalPadding,
                context.toDP(DEFAULT_EVENT_TEXT_HORIZONTAL_PADDING))
            eventTextVerticalPadding = getDimension(R.styleable.WeekView_eventTextHorizontalPadding,
                context.toDP(DEFAULT_EVENT_TEXT_VERTICAL_PADDING))
            eventTextColor = getColor(R.styleable.WeekView_eventTextColor, DEFAULT_EVENT_TEXT_COLOR.toInt())
            eventTextSize = getDimensionPixelSize(R.styleable.WeekView_eventTextSize, textSize)

            pastEventColor = getColor(R.styleable.WeekView_pastEventColor, DEFAULT_PAST_EVENT_COLOR)

            editEventColor = getColor(R.styleable.WeekView_editEventColor, DEFAULT_HIGHLIGHT_COLOR.toInt())
            editEventText = getString(R.styleable.WeekView_editEventText) ?: ""
            editEventStrokeColor = getColor(R.styleable.WeekView_editEventStrokeColor, DEFAULT_EDIT_EVENT_STROKE_COLOR.toInt())
            editEventStrokeWidth = getDimension(R.styleable.WeekView_editEventStrokeWidth,
                context.toDP(DEFAULT_EDIT_EVENT_STROKE_WIDTH))
            editEventShadowColor = getColor(R.styleable.WeekView_editEventShadowColor, DEFAULT_EDIT_EVENT_SHADOW_COLOR.toInt())

            // AllDay
            allDayAreaHeight = getDimensionPixelSize(R.styleable.WeekView_allDayAreaHeight, hourHeight)

            // Day
            dayBackgroundColor = getColor(R.styleable.WeekView_dayBackgroundColor, DEFAULT_DAY_BACKGROUND_COLOR.toInt())
            todayBackgroundColor = getColor(R.styleable.WeekView_todayBackgroundColor, DEFAULT_TODAY_BACKGROUND_COLOR.toInt())

            lineColor = getColor(R.styleable.WeekView_lineColor, DEFAULT_LINE_COLOR.toInt())
            lineAllDayColor = getColor(R.styleable.WeekView_lineAllDayColor, lineColor)
            lineStrokeWidth = getDimension(R.styleable.WeekView_lineStrokeWidth,
                context.toDP(DEFAULT_LINE_STROKE_WIDTH))

            // Highlight
            highlightColor = getColor(R.styleable.WeekView_highlightColor, DEFAULT_HIGHLIGHT_COLOR.toInt())
            highlightStrokeWidth = getDimension(R.styleable.WeekView_highlightStrokeWidth,
                context.toDP(DEFAULT_HIGHLIGHT_STROKE_WIDTH))

            needTimeNow = getBoolean(R.styleable.WeekView_needTimeNow, needTimeNow)

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
        linePaint.let { p ->
            p.color = lineColor
            p.strokeWidth = lineStrokeWidth
        }

        dayOfWeekTextPaint.let { p ->
            p.textSize = dayOfWeekTextSize.toFloat()
            p.color = dayOfWeekTextColor
        }
        dayOfWeekTextNumberPaint.let { p ->
            p.textSize = dayOfWeekTextNumberSize.toFloat()
            p.color = dayOfWeekTextNumberColor
        }
        dayOfWeekTodayTextPaint.let { p ->
            p.textSize = dayOfWeekTextNumberSize.toFloat()
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
        editEventStrokePaint.let { p ->
            p.color = editEventStrokeColor
            p.strokeWidth = editEventStrokeWidth

            p.setShadowLayer(3f, 0f, 1f, editEventShadowColor)
        }

        highlightStrokePaint.strokeWidth = highlightStrokeWidth

        highlightEmphasisDayOfWeekTextPaint.let { p ->
            p.textSize = dayOfWeekTextSize.toFloat()
            p.color = highlightColor
        }
        highlightEmphasisTimelineTextPaint.let { p ->
            p.textSize = timelineTextSize
            p.typeface = Typeface.DEFAULT_BOLD
            p.color = highlightColor
        }

        timelineTextPaint.let { p ->
            p.textSize = timelineTextSize
            p.color = timelineTextColor
        }

        dayOfWeekTodayBackgroundPaint.color = dayOfWeekTodayBackgroundColor
        dayBackgroundPaint.color = dayBackgroundColor
        todayBackgroundPaint.color = todayBackgroundColor

        measureTimeline()
        measureDayOfWeekHeight()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if(!didAttach){
            didAttach = true

            loadSchedules(current)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        animator?.cancel()
        animator = null
    }

    private fun loadSchedules(weekDate: LocalDateTime){
        clearAllRect()

        val schedules = mutableListOf<WeekEvent>()
        var date = weekDate.minusWeeks(1)

        for(dataRange in 0 until DEFAULT_PRELOAD_WEEK_DATA_RANGE){
            with(date){
                val pool = listener?.onWeekChanged(
                    year,
                    monthValue,
                    dayOfMonth,
                    weekOfYear(),
                ) ?: listOf()

                // 중복된 아이디가 있는 경우에는 삭제하도록 한다.
                val distinct = pool.distinctBy { it.id }

                // 데이터가 비어있는 경우에는 그대로 삽입하도록 한다.
                if(schedules.isEmpty()) {
                    schedules.addAll(sortAndSplitSchedules(distinct))
                } else {
                    // 데이터가 있는 경우에는 중복체크를 한 후 삽입하도록 한다.
                    val mDistinct = distinct.toMutableList()

                    for(s in schedules){
                        val index = mDistinct.indexOfLast { c-> s.id == c.id }
                        if(index == -1) continue

                        mDistinct.removeAt(index)
                    }

                    schedules.addAll(sortAndSplitSchedules(mDistinct))
                }
            }

            if(dataRange == 0)
                schedules.removeIf { it.endTimeInMillis < date.toTimeMillis() }

            if(dataRange == DEFAULT_PRELOAD_WEEK_DATA_RANGE - 1)
                schedules.removeIf { it.startTimeInMillis > date.toTimeMillis() }

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

        current.run {
            listener?.onCurrentWeekChanged(year, monthValue, dayOfMonth, weekOfYear())
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // 스크롤을 할 수 있는 최대/최소치를 정해준다
        val y = height - (hourHeight * 24f) - headerHeight
        origin.y = if(y < 0) origin.y.coerceAtLeast(y) else 0f

        // 하루 단위의 셀 넓이를 계산한다.
        widthPerDay = (width - timelineWidth) / visibleDays * 1f

        // 기존 소스가 index 를 통해서 일자를 표시 했음.
        // 스크롤을 통해서 이동하게 되면 index 숫자가, 다음 날은 1 / 이전 날은 -1 로 이동하면서 맨 왼쪽 시작일을 표시했다
        val firstVisibleIndex = -ceil(origin.x / widthPerDay).toInt()
        val startX = timelineWidth + origin.x + (widthPerDay * firstVisibleIndex)
        var offsetX = startX

        var standard = LocalDateTime.now()
        if(startDayOfWeek != DEFAULT_DAY_OF_WEEK_START)
            standard = standard.withDayOfWeek(DayOfWeek.of(startDayOfWeek + 1))

        // 좌측 시간표시(Timeline)와 스케쥴 테이블(Grid)은 일자마다 그릴 필요가 없고
        // 먼저 그려져야 되는 기능이다
        drawTimeline(canvas)
        drawGrid(canvas)

        for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1){
            val date = standard.plusDays((index - 1).toLong())
            val dt = getCurrentDayType(date)

            drawDayOfWeek(canvas, date, offsetX, dt)
            drawAllDayEvent(canvas, date, offsetX, dt)
            drawCommonEvent(canvas, date, offsetX, dt)
            drawTimeNow(canvas, offsetX, dt)

            if(index == firstVisibleIndex + 1)
                firstVisibleDate = date

            offsetX += widthPerDay
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
                var t = startAt!!.toMinuteOfHours() * 1f
                val r = 1f
                var b = endAt!!.toMinuteOfHours() * 1f

                // N분 단위로 일정 노출하기 위한 계산
                // 1분 단위로 노출하고자 할 때에는 아래 수식 삭제
                t -= (t % DEFAULT_EVENT_BLOCK_ROUND_CEIL)
                var ceilBottom = DEFAULT_EVENT_BLOCK_ROUND_CEIL - (b % DEFAULT_EVENT_BLOCK_ROUND_CEIL)
                if(ceilBottom == DEFAULT_EVENT_BLOCK_ROUND_CEIL) ceilBottom = 0f
                b += ceilBottom

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

                // 편집 대상 시간 종료 시간이 24:00 인 경우, 다음날 00시로 처리되기 때문에 View 에 표시가 되지 않아 따로 처리할 수 있도록 변경
                val et = if(endAt!!.hour != 0) endAt!! else endAt!!.minusNanos(1)
                val b = et.toMinuteOfHours() * 1f

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

            if(comparator == 0){
                val (c1, c2) = e1.title to e2.title
                comparator = if(c1 > c2) 1 else (if(c1 < c2) -1 else 0)
            }

            comparator
        }
    }

    private fun measureTimeline(){
        timelineTextWidth = timelineTextPaint.measureText(timelineAllDayEventText).toInt()
        timelineTextHeight = Rect().apply { timelineTextPaint.getTextBounds(timelineAllDayEventText, 0, timelineAllDayEventText.length, this) }.height()
    }

    private fun measureDayOfWeekHeight(){
        dayOfWeekTextHeight = today.toText("E").run {
            Rect().also { r -> dayOfWeekTextPaint.getTextBounds(this, 0, length, r) }.height()
        }

        dayOfWeekTextNumberHeight = today.toText(DEFAULT_FORMAT_DAY_OF_WEEK).run {
            Rect().also { r -> dayOfWeekTextNumberPaint.getTextBounds(this, 0, length, r) }.height()
        }
    }

    private fun drawTimeline(canvas: Canvas?){
        canvas?.run {
            timelineTextPaint.color = timelineAllDayTextColor

            save()
            drawText(timelineAllDayEventText,
                timelineWidth - timelinePadding.right - timelineAllDayTextWidth * 1f,
                dayOfWeekHeight + (allDayAreaHeight / 2) + (timelineTextHeight / 2f),
                timelineTextPaint)

            restore()

            timelineTextPaint.color = timelineTextColor
            save()

            clipRect(0f, headerHeight * 1f, timelineWidth * 1f, height * 1f)

            val date = today.withTime(0)
            (0 until 24).forEach { drawTimelineText(this, date.withHour(it)) }

            restore()
        }
    }

    private fun drawTimelineText(canvas: Canvas?, time: LocalDateTime, doEdit: Boolean = false){
        val str = time.toText("HH")
        val top = origin.y + headerHeight + (hourHeight * time.hour) + timelineTextHeight

        if(top < height){
            val p = if(doEdit) highlightEmphasisTimelineTextPaint else { timelineTextPaint.apply { color = timelineTextColor } }

            canvas?.drawText(str, timelineWidth - timelinePadding.right * 1f, top + 20, p)
        }
    }

    private fun drawGrid(canvas: Canvas?){
        canvas?.run {
            // 종일 스케쥴의 선도 함께 그려준다.
            // 스케쥴의 선을 그렸으면 Header 의 높이를 잴 때 함께 선의 Stroke Width 도 포함시켜야 한다.
            save()

            // 한번에 두줄을 그리기 위해서 FloatArray 를 사용한다
            val lines = FloatArray(2 * 4)

            for(i in 0 until 2){
                var y1 = dayOfWeekHeight + (i * allDayAreaHeight) * 1f
                if(i > 0) y1 += (lineStrokeWidth * 2)

                lines[i * 4] = 0f
                lines[i * 4 + 1] = y1
                lines[i * 4 + 2] = width * 1f
                lines[i * 4 + 3] = y1
            }

            linePaint.color = lineAllDayColor
            drawLines(lines, linePaint)

            restore()

            // 일반 스케쥴에 그어준다.
            save()
            clipRect(timelineWidth, headerHeight, width, height)

            linePaint.color = lineColor
            val offsetY = headerHeight + origin.y
            (1 until 24).forEach{ drawLineWithHour(this, it, offsetY) }

            restore()
        }
    }

    private fun drawLineWithHour(canvas: Canvas?, hour: Int, offsetY: Float, doEdit: Boolean = false){
        val (x, y) = timelineWidth * 1f to offsetY + (hourHeight * hour) + lineStrokeWidth
        val p = if(doEdit) highlightStrokePaint else linePaint

        if(y < height)
            canvas?.drawLine(x, y, width * 1f, y, p)
    }

    private fun drawDayOfWeek(canvas: Canvas?, date: LocalDateTime, offsetX: Float, dayType: Int){
        canvas?.run {
            save()

            clipRect(timelineWidth, 0, width, dayOfWeekHeight)

            val (x1, y1) = offsetX + (widthPerDay / 2) to (dayOfWeekPadding.top + dayOfWeekTextHeight) * 1f
            val (x2, y2) = offsetX + (widthPerDay / 2) to dayOfWeekHeight - dayOfWeekPadding.bottom - ((dayOfWeekTodayBackgroundSize / 2f) - (dayOfWeekTextNumberHeight / 2f))

            val isFirstDayOfMonth = YearMonth.from(date).atDay(1).isSameDay(date.toLocalDate())
            val dayOfWeekText = date.toText("E")
            val dayOfWeekTextNumber = date.toText(if(!isFirstDayOfMonth) DEFAULT_FORMAT_DAY_OF_WEEK else "M.d")

            val textPaint = when(date.dayOfWeek){
                DayOfWeek.SUNDAY -> dayOfWeekSundayTextPaint
                DayOfWeek.SATURDAY -> dayOfWeekSaturdayTextPaint
                else -> dayOfWeekTextPaint
            }
            val textNumberPaint = if(dayType == DayType.TODAY) dayOfWeekTodayTextPaint else dayOfWeekTextNumberPaint

            if(dayType == DayType.TODAY){
                val radius = dayOfWeekTodayBackgroundSize / 2f
                val y = y2 - (dayOfWeekTextNumberHeight / 2f)

                drawCircle(x2, y, radius, dayOfWeekTodayBackgroundPaint)
            }

            drawText(dayOfWeekText, x1, y1, textPaint)
            drawText(dayOfWeekTextNumber, x2, y2, textNumberPaint)

            restore()
        }
    }

    private fun drawCommonEvent(canvas: Canvas?, date: LocalDateTime, offsetX: Float, type: Int){
        canvas?.run {
            save()
            clipRect(timelineWidth * 1f, headerHeight * 1f + lineStrokeWidth, width * 1f, height * 1f)

            drawEmptyRect(this, offsetX)
            drawCommonRect(this, date, offsetX, type)

            restore()
        }
    }

    private fun drawEmptyRect(canvas: Canvas?, startX: Float){
        canvas?.run {
            save()

            val offsetY = origin.y + headerHeight + lineStrokeWidth

            val l = startX
            val r = l + widthPerDay

            for(i in 0 until 24){
                var t = offsetY + (hourHeight * i)
                if(t > 0) t += lineStrokeWidth

                val b = t + hourHeight - (lineStrokeWidth * 2)

                if(t < height)
                    drawRoundRect(
                        l + eventBlockPadding,
                        t + eventBlockPadding,
                        r - eventBlockPadding,
                        b - eventBlockPadding,
                        eventCornerRadius, eventCornerRadius, eventEmptyPaint)
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

            for (rect in cRects){
                val offsetY = origin.y + headerHeight

                val l = startX + (rect.left * widthPerDay)
                val t = offsetY + ((hourHeight * 24 * rect.top) / 1440) + (lineStrokeWidth * 2)
                val r = l + (rect.right * widthPerDay)
                val b = offsetY + ((hourHeight * 24 * rect.bottom) / 1440)

                rect.setAbsoluteRect(l, t, r, b, eventBlockPadding)

                drawEventArea(this, rect, type)
            }

            restore()
        }
    }

    private fun drawEventText(canvas: Canvas?, rect: WeekRect, gravity: Int = Gravity.TOP, typeface: Int = Typeface.NORMAL){
        val (e, bound) = rect.run { originalEvent to absoluteRect }

        eventTextPaint.color = if(isBrightColor(e.backgroundColor)) eventTextColor else Color.WHITE

        val sb = SpannableStringBuilder()
        if(e.title.isNotBlank()){
            with(sb){
                append(e.title)
                setSpan(StyleSpan(typeface), 0, length, 0)
            }
        }

        val (availWidth, availHeight) = bound.width() - eventTextHorizontalPadding to bound.height() - eventTextVerticalPadding
        val alignment = when(gravity){
            Gravity.CENTER -> Layout.Alignment.ALIGN_CENTER
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        var sl = StaticLayout(sb, eventTextPaint, availWidth.toInt(), alignment, 1f, 0f, false)
        val lineHeight = sl.height / sl.lineCount

        if(availHeight < lineHeight) return

        var availLineCount = (availHeight / lineHeight).coerceAtMost(DEFAULT_EVENT_TEXT_MAX_LINES)

        do {
            sl = StaticLayout(
                TextUtils.ellipsize(sb, eventTextPaint, availLineCount * availWidth, TextUtils.TruncateAt.END),
                eventTextPaint,
                availWidth.toInt(),
                alignment,
                1f, 0f, false
            )

            availLineCount--
        } while (sl.height > availHeight)

        canvas?.run {
            save()

            var (x, y) = bound.left to bound.top
            when(gravity){
                Gravity.TOP -> {
                    x += (eventTextHorizontalPadding / 2)
                    y += (eventTextVerticalPadding / 2)
                }

                Gravity.CENTER -> {
                    x += (bound.width() / 2) - (sl.width / 2)
                    y += (bound.height() / 2) - (sl.height / 2)
                }
            }

            translate(x, y)
            sl.draw(this)

            restore()
        }
    }

    private fun drawAllDayEvent(canvas: Canvas?, date: LocalDateTime, offsetX: Float, type: Int){
        if(rects.isEmpty()) return

        canvas?.run {
            save()

            clipRect(timelineWidth, dayOfWeekHeight, width, headerHeight)

            for(rect in rects){
                if(!date.isSameDay(rect.startAt) || !rect.event.isAllDay()) continue

                val (l, t) = offsetX to dayOfWeekHeight + lineStrokeWidth * 1f
                val (r, b) = l + widthPerDay to t + allDayAreaHeight

                rect.setAbsoluteRect(l, t, r, b, eventBlockPadding)

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
            drawRoundRect(absRect, eventCornerRadius, eventCornerRadius, painter)
            drawEventText(canvas, rect)
        }
    }

    private fun drawEdit(canvas: Canvas?, firstVisibleIndex: Int, firstDayOfWeek: LocalDateTime, startX: Float){
        val dRects = rects.filter { it.originalEvent is DummyWeekEvent }
        if(dRects.isEmpty()) return

        var offsetX = startX

        canvas?.run {
            save()

            // Dim 을 담당한다.
            drawRect(timelineWidth * 1f, (dayOfWeekHeight + lineStrokeWidth) * 1f, width * 1f, height * 1f, editEventDimPaint)

            restore()

            val offsetY = origin.y + headerHeight

            for(rect in dRects){
                val temp = firstDayOfWeek.run { LocalDateTime.of(toLocalDate(), toLocalTime()) }

                for(index in firstVisibleIndex + 1 .. firstVisibleIndex + visibleDays + 1) {
                    val date = temp.plusDays((index - 1).toLong())

                    if(!date.isSameDay(rect.startAt)) {
                        offsetX += widthPerDay

                        continue
                    }

                    val l = offsetX + (rect.left * widthPerDay) + highlightStrokeWidth
                    val t = offsetY + ((hourHeight * 24 * rect.top) / 1440) + lineStrokeWidth
                    val r = l + (rect.right * widthPerDay) - highlightStrokeWidth
                    val b = offsetY + ((hourHeight * 24 * rect.bottom) / 1440) - lineStrokeWidth

                    rect.setAbsoluteRect(l, t, r, b, eventBlockPadding)
                    highlightRect.left = l
                    highlightRect.right = r

                    save()

                    clipRect(timelineWidth * 1f, 0f, width * 1f, height * 1f)
                    // 하이라이트 & 딤을 해주면 일자와 시간이 전부 알파처리되기 떄문에
                    drawRoundRect(highlightRect, eventCornerRadius, eventCornerRadius, highlightPaint)
                    drawRoundRect(highlightRect, eventCornerRadius, eventCornerRadius, highlightStrokePaint)

                    restore()

                    save()

                    // 그 위로 새로 덧그려준다
                    val startAt = rect.originalEvent.startAt!!
                    drawTimelineText(this, startAt, true)
                    drawLineWithHour(this, startAt.hour, offsetY, true)

                    val dt = getCurrentDayType(date)
                    drawDayOfWeek(this, date, offsetX, dt)

                    restore()

                    save()
                    clipRect(timelineWidth * 1f, 0f, width * 1f, height * 1f)

                    if(l < r
                        && l < width && t < height
                        && r > timelineWidth && b > headerHeight){
                        painter.color = rect.backgroundColor

                        drawRoundRect(rect.absoluteRect, eventCornerRadius, eventCornerRadius, painter)
                        drawRoundRect(rect.absoluteRect, eventCornerRadius, eventCornerRadius, editEventStrokePaint)

                        drawEventText(this, rect, Gravity.CENTER, Typeface.BOLD)
                    }

                    restore()
                }

                offsetX = startX
            }
        }
    }

    private fun drawTimeNow(canvas: Canvas?, offsetX: Float, dayType: Int){
        if(!(needTimeNow && dayType == DayType.TODAY)) return

        val left = offsetX.coerceAtLeast(timelineWidth.toFloat())
        val top = headerHeight

        // 현재 날의 시간 위치를 수평선으로 보여 준다.
        val barOffset = top + origin.y
        val beforeNow = today.run { (hour + (minute / 60f)) * hourHeight }

        canvas?.run {
            save()

            clipRect(timelineWidth, top, width, height)
            drawLine(left, barOffset + beforeNow, offsetX + widthPerDay, barOffset + beforeNow, timeNowPaint)

            restore()
        }
    }

    /**
     * #DCDCDC 보다 밝은 경우에는 다른 색을 표시할 수 있도록 한다.
     * @param color 비교할 컬러 값
     */
    private fun isBrightColor(color: Int) : Boolean = (color and 0x00FFFFFF) > 0xDCDCDC

    private fun getCurrentDayType(date: LocalDateTime) = when {
        today.isBeforeDay(date) -> DayType.PAST
        date.isSameDay(today) -> DayType.TODAY
        else -> DayType.COMMON
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
                        // 롱 클릭 후 드래그 하기 위해서는 ACTION_MOVE 할 때,
                        // Cancel 이벤트를 MotionEvent 에 전달해주는 방식으로 트릭을 사용한다고 한다.
                        if(isLongPressed){
                            isLongPressed = false

                            val cancelEvent = MotionEvent.obtain(this).apply {
                                action = MotionEvent.ACTION_CANCEL
                            }

                            detector.onTouchEvent(cancelEvent)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
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
            // 스크롤 이벤트가 일어났는데 페이지가 이동되는 경우라면,
            if(PageMove.doPageMove(statePageMove)){
                // 이벤트에 맞게 데이터를 다시 호출할 수 있도록 한다.
                when(statePageMove){
                    PageMove.TODAY -> loadSchedules(today)
                    PageMove.PREV_MONTH, PageMove.NEXT_MONTH, PageMove.WHERE -> loadSchedules(current)
                }

                statePageMove = PageMove.NONE
            }

            // 스크롤이 VERTICAL 인 경우에는 NONE 처리를 따로 해주어야 한다.
            if(arrayOf(currentScrollDirection, currentFlingDirection).all { it == Scroll.VERTICAL }){
                currentFlingDirection = Scroll.NONE
                currentScrollDirection = Scroll.NONE
            }

            // 모든 스크롤/플링 이벤트가 종료 되면,
            if(arrayOf(currentScrollDirection, currentFlingDirection).all { it == Scroll.NONE }){
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
                    currentScrollDirection == Scroll.LEFT -> kotlin.math.floor(this)
                    currentScrollDirection == Scroll.RIGHT -> ceil(this)
                    currentFlingDirection != Scroll.NONE -> kotlin.math.round(this)
                    else -> kotlin.math.round(this)
                }
            }.toLong()

            when (currentScrollDirection) {
                Scroll.LEFT -> {
                    statePageMove = PageMove.NEXT_MONTH

                    val date = LocalDate.now()
                        .minusDays(days)
                        .plusWeeks(1)
                        .withDayOfWeek(today.dayOfWeek)

                    scrollToDate(date)
                }

                Scroll.RIGHT -> {
                    statePageMove = PageMove.PREV_MONTH

                    val date = LocalDate.now()
                        .minusDays(days)
                        .withDayOfWeek(today.dayOfWeek)

                    scrollToDate(date)
                }
            }
        }else{
            // 허용 범위를 넘어서지 못하게 되면 원래 좌표로 되돌아가게 된다.
            val dx = origin.x - prevX

            scrollOriginTo(-dx.toInt(), 0, 100)
        }

        currentFlingDirection = Scroll.NONE
        currentScrollDirection = Scroll.NONE
    }

    private fun scrollToDate(date: LocalDate){
        val diff = today.toLocalDate().toEpochDay() - date.toEpochDay()
        val offset = origin.x - (diff * widthPerDay)

        current = date.toLocalDateTime()

        scrollOriginTo(-offset.toInt(), 0)
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

        animator = null
        isEditMode = false
        editEvent = null
    }

    private fun removeOnlyDummyEvent(){ rects.removeIf { it.originalEvent is DummyWeekEvent } }

    private fun clearAllRect(){
        // 편집 이벤트에 관련된 블록은 삭제에서 제외하도록 한다.
        // 스크롤 및 페이지 이동 시 화면이 갱신됨에 따라 사라지는 문제 때문.
        val r = rects.filterNot { it.originalEvent is DummyWeekEvent }
        if(r.isEmpty()) return

        rects.removeIf { it.originalEvent !is DummyWeekEvent }
    }

    var onWeekChangeListener: OnWeekViewListener?
        set(value) { this.listener = value }
        get() = this.listener

    /**
     * 현재 시각으로 스크롤을 이동하도록 한다.
     */
    fun scrollToCurrentTime() { scrollToTime(LocalDateTime.now().hour) }

    /**
     * 해당 시각으로 스크롤을 이동하도록 한다. 해당 메소드를 사용하지 않으면 00시부터 데이터가 디스플레이 된다.
     *
     * @param hour 0~23 까지의 시간
     * @param smoothScroll 스크롤 애니메이션 여부
     */
    fun scrollToTime(hour: Int, smoothScroll: Boolean = true){
        val y = (hourHeight * hour) + (lineStrokeWidth * 2)

        if(!isDrawn){
            val offset = y - height

            origin.y = -offset
        }else{
            if(!smoothScroll){
                origin.y = -y

                invalidate()
            }else{
                val dy = origin.y + y

                scrollOriginTo(0, -dy.toInt())
            }
        }
    }

    /**
     * 스크롤 애니메이션이 없이 시간을 설정하도록 한다.
     *
     * @param hour 0~23 까지의 시간
     */
    fun scrollToTime(hour: Int){ scrollToTime(hour, false) }

    /**
     * 다음 주로 이동하도록 한다.
     */
    fun moveToNext(){
        if(doScrolling() || statePageMove != PageMove.NONE) return

        statePageMove = PageMove.NEXT_MONTH
        val date = current.plusWeeks(1)

        moveTo(date.toLocalDate())
    }

    /**
     * 이전 주로 이동하도록 한다.
     */
    fun moveToPrev(){
        if(doScrolling() || statePageMove != PageMove.NONE) return
        if(!canMovePrevious()) return

        statePageMove = PageMove.PREV_MONTH
        val date = current.minusWeeks(1)

        moveTo(date.toLocalDate())
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
        if(statePageMove == PageMove.NONE){
            statePageMove = PageMove.WHERE
        }

        scrollToDate(date)
    }

    /**
     * 이전 페이지로 넘어갈 수 있는 지 확인한다.
     */
    fun canMovePrevious() : Boolean
        = !canMovePastDate && !today.isBeforeWeek(current.minusWeeks(1))

    /**
     * 편집모드 하이라이트 사각형의 범위를 지정해준다
     */
    private fun setEditBounds(){
        resetEditBounds()

        val t = highlightStrokeWidth
        val b = height - t

        highlightRect.set(0f, t * 1f, 0f, b * 1f)
    }

    private fun resetEditBounds(){
        highlightRect.set(0f, 0f, 0f, 0f)
    }

    /**
     * 특정 위치(날짜시간)에 생성할 스케쥴의 위치를 표시하도록 한다.
     * 시간은 1시간 단위로 UI 상에 표시가 된다.
     *
     * @param date 표시하고자 하는 날짜와 시작시간
     */
    fun prepareEditSchedule(date: LocalDateTime){
        // 오늘 기준 이전일 이면 편집을 하지 못하도록 한다.
        if(today.isBeforeDay(date)) {
            sendError(IsPastFromTodayException(today, date))
            return
        }

        val c = date.withDayOfWeek(today.dayOfWeek)
        moveTo(c.toLocalDate())

        postDelayed(250) {
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
     * 스케쥴 등록을 위해 준비한 UI 를 제거 한다.
     */
    fun dismissEditSchedule(){
        clearEditMode()

        invalidate()

        listener?.onEmptyEventDismissed()
    }

    /**
     * ID 값을 통하여 이벤트를 지우도록 한다. View 상에서 UI 만 삭제되기 때문에
     * 모델 상의 데이터는 따로 삭제 해주어야 한다.
     *
     * @param id 이벤트의 ID 값
     */
    fun removeScheduleById(id: String){
        if(!hasScheduleById(id)) {
            sendError(NotExistException())

            return
        }

        rects.removeIf { it.originalEvent.id == id }

        invalidate()
    }

    /**
     * 스케쥴을 등록한다
     *
     * @param event UI 로 사용할 이벤트를 넣는다.
     */
    fun addSchedule(event: WeekEvent){
        if(event.startTimeInMillis > event.endTimeInMillis) {
            sendError(TimeIsReversedException())
            return
        }

        if(hasSchedule(event)) {
            sendError(AlreadyExistException(event))
            return
        }

        val list = sortAndSplitSchedules(listOf(event))
        dispatchCommonEvents(list.toMutableList())

        invalidate()
    }

    /**
     * 현재 스케쥴 리스트에 스케쥴이 있는지 확인한다.
     *
     * @param event 찾고자 하는 이벤트
     */
    fun hasSchedule(event: WeekEvent) : Boolean = hasScheduleById(event.id)

    /**
     * ID 값을 통하여 현재 스케쥴 리스트에 스케쥴이 있는지 확인한다.
     *
     * @param id 찾고자 하는 이벤트 ID 값
     */
    fun hasScheduleById(id: String) : Boolean = rects.any { it.originalEvent.id == id }

    /**
     * 현재 UI에 보여지는 연/월/년 기준 주 수 를 전달 해 준다.
     */
    fun getCurrentDate() = current.run { listOf(year, monthValue, weekOfYear()) }

    /**
     * 편집 모드일 때의 임시 이벤트명을 설정한다
     *
     * @param str 임시 이벤트 명
     */
    fun setEditEventText(str: String){
        editEventText = str

        if(isDrawn) invalidate()
    }

    /**
     * 편집 모드일 때의 임시 이벤트명을 설정한다
     *
     * @param res 임시 이벤트 명 리소스
     */
    fun setEditEventText(res: Int){ setEditEventText(resources.getString(res)) }

    /**
     * 타임라인의 왼쪽 패딩을 넣어준다. 단 패딩은 타임라인의 시간 글자 영역을 밀어내며, 수평으로 그어진 선은 밀어내지 않는다.
     * @param padding 패딩 값
     */
    fun setTimelineLeftPadding(padding: Int){ setTimelinePadding(padding, timelinePadding.right) }

    /**
     * 타임라인의 오른쪽 패딩을 넣어준다
     * @param padding 패딩 값
     */
    fun setTimelineRightPadding(padding: Int){ setTimelinePadding(timelinePadding.right, padding) }

    /**
     * 타임라인의 양 옆 패딩을 넣어준다. 단, 왼쪽 패딩은 타임라인의 시간 글자 영역을 밀어내며, 수평으로 그어진 선은 밀어내지 않는다.
     *
     * @param left 왼쪽 패딩
     * @param right 오른쪽 패딩
     */
    fun setTimelinePadding(leftPx: Int, rightPx: Int){
        timelinePadding.left = context.toDP(leftPx).toInt()
        timelinePadding.right = context.toDP(rightPx).toInt()

        if(isDrawn) invalidate()
    }

    private fun prepareScheduleInternal(start: LocalDateTime, end: LocalDateTime){
        isEditMode = true

        editEvent = DummyWeekEvent().apply {
            title = editEventText
            setBackgroundColor(editEventColor)

            setStartAndEndDate(start, end)
        }

        setEditBounds()
        positioningTempEventRectOffset(listOf(editEvent!!))

        listener?.onEmptyEventWillBeAdded(editEvent!!.startTimeInMillis, editEvent!!.endTimeInMillis)
    }

    private fun adjustScheduleStartOrEnd(times: Long, isStartOrEnd: Boolean)
            = adjustScheduleStartOrEnd(times.toLocalDateTime(), isStartOrEnd)

    private fun adjustScheduleStartOrEnd(ldt: LocalDateTime, isStartOrEnd: Boolean)
            = ldt.run { withTime(if(isStartOrEnd) hour else hour + 1, 0, 0) }

    private fun changeAlphaColor(alpha: Int, color: Int) = (alpha shl 24) or (color and 0x00FFFFFF)

    private fun animatorHighlight() : Animator {
        val anim1 = ValueAnimator.ofInt(0, 0xFF).apply {
            addUpdateListener {
                val color = changeAlphaColor(it.animatedValue as Int, editEventColor)

                editEvent!!.setBackgroundColor(color)
            }
        }

        val anim2 = ValueAnimator.ofInt(0, DEFAULT_HIGHLIGHT_BACKGROUND_ALPHA).apply {
            addUpdateListener { highlightPaint.color = changeAlphaColor(it.animatedValue as Int, highlightColor) }
        }

        val anim3 = ValueAnimator.ofInt(0, 0xFF).apply {
            addUpdateListener { highlightStrokePaint.color = changeAlphaColor(it.animatedValue as Int, highlightColor) }
        }

        val anim4 = ValueAnimator.ofInt(0, DEFAULT_EDIT_EVENT_DIM_ALPHA).apply {
            addUpdateListener {
                editEventDimPaint.color = changeAlphaColor(it.animatedValue as Int, 0xFFFFFFFF.toInt())

                invalidate()
            }
        }

        val animSet = AnimatorSet().apply {
            duration = 250
            interpolator = DecelerateInterpolator()

            playTogether(anim1, anim2, anim3, anim4)
        }

        return animSet
    }

    private fun sendError(e: Throwable?){ listener?.onErrorEventListener(e) }

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
         * 선택된 이벤트에 대한 데이터를 넘겨준다. Edit 모드가 단일 클릭에서 이뤄지는 경우 호출이 되지 않는다.
         *
         * @param event UI 에서 사용하는 이벤트 데이터
         */
        fun onWeekEventSelected(event: WeekEvent)

        /**
         * 비어있는 이벤트에 대해서 추가하고자 할 때 사용된다. 해당 메소드는 기본적으로 클릭으로 실행된다.
         * 넘겨주는 데이터는 클릭이 일어난 부분의 start 시간과 end 시간을 milliseconds 값으로 알려주게 된다.
         *
         * @param start 시작시간
         * @param end 종료시간
         */
        fun onEmptyEventWillBeAdded(start: Long, end: Long)

        /**
         * 이벤트 편집모드가 바뀔 때 호출된다.
         */
        fun onEmptyEventDismissed()

        /**
         * 주가 변경될 때마다 불리는 메소드로, @see[onWeekChanged] 와는 다르게 현재의 주만 뿌려준다.
         *
         * @param year 해당 주 첫번째 일의 연도
         * @param month 해당 주 첫번째 일의 월 (+1 하지 않아도 됌)
         * @param date 해당 주 첫번째 일의 일자
         * @param week 해당 주의 연 기준 주일 수
         */
        fun onCurrentWeekChanged(year: Int, month: Int, date: Int, week: Int)

        /**
         * 이벤트 처리 중 에러가 있는 경우에 발생함
         *
         * @see[WeekViewException]
         */
        fun onErrorEventListener(e: Throwable?)
    }
}