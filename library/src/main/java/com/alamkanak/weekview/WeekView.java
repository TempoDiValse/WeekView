package com.alamkanak.weekview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.alamkanak.weekview.WeekViewUtil.*;

public class WeekView extends View {
    public enum DayType {
        PAST, TODAY, COMMON, SELECTED
    }

    private enum Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    private static final int DRAW_EACH_EVENT_AREA = 0;
    private static final int DRAW_ALLDAY_EVENT_AREA = 1;

    private Calendar mCurrentWeek = null;

    private final Context mContext;
    private Paint mTimeTextPaint;
    private float mTimeTextWidth;
    private float mTimeTextHeight;
    private Paint mHeaderTextPaint;
    private float mHeaderTextHeight;
    private float mHeaderHeight;
    private GestureDetectorCompat mGestureDetector;
    private OverScroller mScroller;
    private PointF mCurrentOrigin = new PointF(0f, 0f);
    private Direction mCurrentScrollDirection = Direction.NONE;
    private Paint mHeaderBackgroundPaint;
    private float mWidthPerDay;
    private Paint mDayBackgroundPaint;
    private Paint mHourSeparatorPaint;
    private float mHeaderMarginBottom;
    private Paint mTempBackgroundPaint;
    private Paint mTodayBackgroundPaint;
    private Paint mSundayTextPaint;
    private Paint mSaturdayTextPaint;
    private Paint mNowLinePaint;
    private Paint mTodayHeaderTextPaint;
    private Paint mEventBackgroundPaint;
    private float mHeaderColumnWidth;

    private List<EventRect> mEventRects;
    private EventRect mTempEventRect;
    private List<? extends WeekViewEvent> mPreviousPeriodEvents;
    private List<? extends WeekViewEvent> mCurrentPeriodEvents;
    private List<? extends WeekViewEvent> mNextPeriodEvents;

    private TextPaint mEventTextPaint;
    private Paint mHeaderColumnBackgroundPaint;
    private Direction mCurrentFlingDirection = Direction.NONE;
    private ScaleGestureDetector mScaleDetector;
    private boolean mIsZooming;
    private int mDefaultEventColor;
    private int mMinimumFlingVelocity = 0;
    private int mScaledTouchSlop = 0;
    // Attributes and their default values.
    private int mHourHeight = 50;
    private int mNewHourHeight = -1;
    private int mMinHourHeight = 0; //no minimum specified (will be dynamic, based on screen)
    private int mEffectiveMinHourHeight = mMinHourHeight; //compensates for the fact that you can't keep zooming out.
    private int mMaxHourHeight = 250;
    private int mFirstDayOfWeek = Calendar.SUNDAY;
    private int mTextSize = 12;
    private int mHeaderColumnPadding = 10;
    private int mHeaderColumnTextColor = Color.BLACK;
    private int mNumberOfVisibleDays = 7;
    private int mHeaderRowPadding = 10;
    private int mHeaderRowBackgroundColor = Color.WHITE;
    private int mDayBackgroundColor = Color.rgb(245, 245, 245);
    private int mNowLineColor = Color.rgb(102, 102, 102);
    private int mNowLineThickness = 5;
    private int mHourSeparatorColor = Color.rgb(230, 230, 230);
    private int mTodayBackgroundColor = Color.rgb(239, 247, 254);
    private int mPastEventBackgroundColor = Color.rgb(177, 177, 177);
    private int mPastDayBackgroundColor = Color.rgb(207, 207, 207);
    private int mHourSeparatorHeight = 2;
    private int mTodayHeaderTextColor = Color.WHITE;
    private int mEventTextSize = 12;
    private int mEventTextColor = Color.BLACK;
    private int mEventPadding = 8;
    private int mHeaderColumnBackgroundColor = Color.WHITE;
    private boolean mAreDimensionsInvalid = true;
    private float mXScrollingSpeed = 1f;
    private Calendar mScrollToDay = null;
    private double mScrollToHour = -1;
    private int mEventCornerRadius = 0;
    private boolean mShowNowLine = false;
    private boolean mHorizontalFlingEnabled = true;
    private boolean mVerticalFlingEnabled = true;
    private int mAllDayEventHeight = 150;
    private int mScrollDuration = 250;

    // Listeners.
    private EventClickListener mEventClickListener;
    private EventLongPressListener mEventLongPressListener;
    private WeekViewLoader mWeekViewLoader;
    private EmptyViewClickListener mEmptyViewClickListener;
    private EmptyViewLongPressListener mEmptyViewLongPressListener;
    private DateTimeInterpreter mDateTimeInterpreter;
    private ScrollListener mScrollListener;
    private PinchListener pinchListener;

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            goToNearestOrigin();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Check if view is zoomed.
            if (mIsZooming)
                return true;

            boolean didScrollHorizontal = Math.abs(distanceX) > Math.abs(distanceY);
            switch (mCurrentScrollDirection) {
                case NONE: {
                    // Allow scrolling only in one direction.
                    if (didScrollHorizontal) {
                        if (distanceX > 0) {
                            mCurrentScrollDirection = Direction.LEFT;
                        } else {
                            mCurrentScrollDirection = Direction.RIGHT;
                        }
                    } else {
                        mCurrentScrollDirection = Direction.VERTICAL;
                    }
                    break;
                }
                case LEFT: {
                    // Change direction if there was enough change.
                    if (didScrollHorizontal && (distanceX < -mScaledTouchSlop)) {
                        mCurrentScrollDirection = Direction.RIGHT;
                    }
                    break;
                }
                case RIGHT: {
                    // Change direction if there was enough change.
                    if (didScrollHorizontal && (distanceX > mScaledTouchSlop)) {
                        mCurrentScrollDirection = Direction.LEFT;
                    }
                    break;
                }
            }

            // Calculate the new origin after scroll.
            switch (mCurrentScrollDirection) {
                case LEFT:
                case RIGHT:
                    mCurrentOrigin.x -= distanceX * mXScrollingSpeed;
                    ViewCompat.postInvalidateOnAnimation(WeekView.this);
                    break;
                case VERTICAL:
                    mCurrentOrigin.y -= distanceY;
                    ViewCompat.postInvalidateOnAnimation(WeekView.this);
                    break;
            }

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mIsZooming)
                return true;

            if ((mCurrentFlingDirection == Direction.LEFT && !mHorizontalFlingEnabled) ||
                    (mCurrentFlingDirection == Direction.RIGHT && !mHorizontalFlingEnabled) ||
                    (mCurrentFlingDirection == Direction.VERTICAL && !mVerticalFlingEnabled)) {
                return true;
            }

            mScroller.forceFinished(true);

            mCurrentFlingDirection = mCurrentScrollDirection;
            switch (mCurrentFlingDirection) {
                case LEFT:
                case RIGHT:
                    mScroller.fling((int) mCurrentOrigin.x, (int) mCurrentOrigin.y, (int) (velocityX * mXScrollingSpeed), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, (int) -(mHourHeight * 24 + mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight / 2 - getHeight()), 0);
                    break;
                case VERTICAL:
                    mScroller.fling((int) mCurrentOrigin.x, (int) mCurrentOrigin.y, 0, (int) velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, (int) -(mHourHeight * 24 + mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight/2 - getHeight()), 0);
                    break;
            }

            ViewCompat.postInvalidateOnAnimation(WeekView.this);
            return true;
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // 스케쥴 잡기 대기 중인 이벤트가 있는 경우에는 해당 이벤트를 삭제한다
            if(mTempEventRect != null){
                mTempEventRect.rectF = null;
                mTempEventRect = null;

                invalidate();

                return super.onSingleTapConfirmed(e);
            }

            // If the tap was on an event then trigger the callback.
            if (mEventRects != null && mEventClickListener != null) {
                List<EventRect> reversedEventRects = mEventRects;
                Collections.reverse(reversedEventRects);

                for (EventRect event : reversedEventRects) {
                    if (event.rectF != null && e.getX() > event.rectF.left && e.getX() < event.rectF.right && e.getY() > event.rectF.top && e.getY() < event.rectF.bottom) {
                        mEventClickListener.onEventClick(event.originalEvent, event.rectF);
                        playSoundEffect(SoundEffectConstants.CLICK);

                        return super.onSingleTapConfirmed(e);
                    }
                }
            }

            // 비어있는 공간을 터치하면 임시 트리거가 작동되도록 함.
            if (mEmptyViewClickListener != null
                    && e.getX() > mHeaderColumnWidth
                    && e.getY() > (mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom)) {
                Calendar selectedTime = getTimeFromPoint(e.getX(), e.getY());

                if (selectedTime != null) {
                    playSoundEffect(SoundEffectConstants.CLICK);

                    mEmptyViewClickListener.onEmptyViewClicked(selectedTime);

                    doDrawTemporaryEvent(selectedTime);
                }
            }

            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);

            if (mEventLongPressListener != null && mEventRects != null) {
                List<EventRect> reversedEventRects = mEventRects;
                Collections.reverse(reversedEventRects);
                for (EventRect event : reversedEventRects) {
                    if (event.rectF != null && e.getX() > event.rectF.left && e.getX() < event.rectF.right && e.getY() > event.rectF.top && e.getY() < event.rectF.bottom) {
                        mEventLongPressListener.onEventLongPress(event.originalEvent, event.rectF);
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        return;
                    }
                }
            }

            // If the tap was on in an empty space, then trigger the callback.
            if (mEmptyViewLongPressListener != null
                    && e.getX() > mHeaderColumnWidth
                    && e.getY() > (mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom)) {
                Calendar selectedTime = getTimeFromPoint(e.getX(), e.getY());

                if (selectedTime != null) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    mEmptyViewLongPressListener.onEmptyViewLongPress(selectedTime);

                    doDrawTemporaryEvent(selectedTime);
                }
            }
        }
    };

    public WeekView(Context context) {
        this(context, null);
    }

    public WeekView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeekView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Hold references.
        mContext = context;

        // Get the attribute values (if any).
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0);
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.WeekView_firstDayOfWeek, mFirstDayOfWeek);
            mHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourHeight, mHourHeight);
            mMinHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_minHourHeight, mMinHourHeight);
            mEffectiveMinHourHeight = mMinHourHeight;
            mMaxHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_maxHourHeight, mMaxHourHeight);
            mTextSize = a.getDimensionPixelSize(R.styleable.WeekView_textSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize, context.getResources().getDisplayMetrics()));
            mHeaderColumnPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerColumnPadding, mHeaderColumnPadding);
            mHeaderColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColor, mHeaderColumnTextColor);
            mHeaderRowPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerRowPadding, mHeaderRowPadding);
            mHeaderRowBackgroundColor = a.getColor(R.styleable.WeekView_headerRowBackgroundColor, mHeaderRowBackgroundColor);
            mDayBackgroundColor = a.getColor(R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor);
            mNowLineColor = a.getColor(R.styleable.WeekView_nowLineColor, mNowLineColor);
            mNowLineThickness = a.getDimensionPixelSize(R.styleable.WeekView_nowLineThickness, mNowLineThickness);
            mHourSeparatorColor = a.getColor(R.styleable.WeekView_hourSeparatorColor, mHourSeparatorColor);
            mTodayBackgroundColor = a.getColor(R.styleable.WeekView_todayBackgroundColor, mTodayBackgroundColor);
            mHourSeparatorHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorHeight, mHourSeparatorHeight);
            mEventTextSize = a.getDimensionPixelSize(R.styleable.WeekView_eventTextSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mEventTextSize, context.getResources().getDisplayMetrics()));
            mEventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, mEventTextColor);
            mEventPadding = a.getDimensionPixelSize(R.styleable.WeekView_eventPadding, mEventPadding);
            mHeaderColumnBackgroundColor = a.getColor(R.styleable.WeekView_headerColumnBackground, mHeaderColumnBackgroundColor);
            mEventCornerRadius = a.getDimensionPixelSize(R.styleable.WeekView_eventCornerRadius, mEventCornerRadius);
            mShowNowLine = a.getBoolean(R.styleable.WeekView_showNowLine, mShowNowLine);
            mAllDayEventHeight = a.getDimensionPixelSize(R.styleable.WeekView_allDayEventHeight, mAllDayEventHeight);
            mScrollDuration = a.getInt(R.styleable.WeekView_scrollDuration, mScrollDuration);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        // Scrolling initialization.
        mGestureDetector = new GestureDetectorCompat(mContext, mGestureListener);
        mScroller = new OverScroller(mContext, new FastOutLinearInInterpolator());

        mMinimumFlingVelocity = ViewConfiguration.get(mContext).getScaledMinimumFlingVelocity();

        // Measure settings for time column.
        mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimeTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTimeTextPaint.setTextSize(mTextSize);
        mTimeTextPaint.setColor(mHeaderColumnTextColor);
        Rect rect = new Rect();
        mTimeTextPaint.getTextBounds("00 PM", 0, "00 PM".length(), rect);
        mTimeTextHeight = rect.height();
        mHeaderMarginBottom = mTimeTextHeight / 2;
        initTextTimeWidth();

        // Measure settings for header row.
        mHeaderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHeaderTextPaint.setColor(mHeaderColumnTextColor);
        mHeaderTextPaint.setTextAlign(Paint.Align.CENTER);
        mHeaderTextPaint.setTextSize(mTextSize);
        mHeaderTextPaint.getTextBounds("00 PM", 0, "00 PM".length(), rect);
        mHeaderTextHeight = rect.height();
        mHeaderTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Prepare header background paint.
        mHeaderBackgroundPaint = new Paint();
        mHeaderBackgroundPaint.setColor(mHeaderRowBackgroundColor);

        // Prepare day background color paint.
        mDayBackgroundPaint = new Paint();
        mDayBackgroundPaint.setColor(mDayBackgroundColor);

        // Prepare hour separator color paint.
        mHourSeparatorPaint = new Paint();
        mHourSeparatorPaint.setStyle(Paint.Style.STROKE);
        mHourSeparatorPaint.setStrokeWidth(mHourSeparatorHeight);
        mHourSeparatorPaint.setColor(mHourSeparatorColor);

        // Prepare the "now" line color paint
        mNowLinePaint = new Paint();
        mNowLinePaint.setStrokeWidth(mNowLineThickness);
        mNowLinePaint.setColor(mNowLineColor);

        mTempBackgroundPaint = new Paint();
        mTempBackgroundPaint.setColor(Color.MAGENTA);

        // Prepare today background color paint.
        mTodayBackgroundPaint = new Paint();
        mTodayBackgroundPaint.setColor(mTodayBackgroundColor);

        // Prepare today header text color paint.
        mTodayHeaderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTodayHeaderTextPaint.setTextAlign(Paint.Align.CENTER);
        mTodayHeaderTextPaint.setTextSize(mTextSize);
        mTodayHeaderTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTodayHeaderTextPaint.setColor(mTodayHeaderTextColor);

        mSundayTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSundayTextPaint.setTextAlign(Paint.Align.CENTER);
        mSundayTextPaint.setTextSize(mTextSize);
        mSundayTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mSundayTextPaint.setColor(Color.rgb(246, 77, 32));

        mSaturdayTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSaturdayTextPaint.setTextAlign(Paint.Align.CENTER);
        mSaturdayTextPaint.setTextSize(mTextSize);
        mSaturdayTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mSaturdayTextPaint.setColor(Color.rgb(26, 139, 245));

        // Prepare event background color.
        mEventBackgroundPaint = new Paint();
        mEventBackgroundPaint.setColor(Color.rgb(174, 208, 238));

        // Prepare header column background color.
        mHeaderColumnBackgroundPaint = new Paint();
        mHeaderColumnBackgroundPaint.setColor(mHeaderColumnBackgroundColor);

        // Prepare event text size and color.
        mEventTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        mEventTextPaint.setStyle(Paint.Style.FILL);
        mEventTextPaint.setColor(mEventTextColor);
        mEventTextPaint.setTextSize(mEventTextSize);

        // Set default event color.
        mDefaultEventColor = Color.parseColor("#9fc6e7");

        mScaleDetector = new ScaleGestureDetector(mContext, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mIsZooming = false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mIsZooming = true;
                goToNearestOrigin();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mNewHourHeight = Math.round(mHourHeight * detector.getScaleFactor());
                invalidate();

                if(mNewHourHeight > mMaxHourHeight && pinchListener != null){
                    pinchListener.onReachedMax(detector);
                }
                return true;
            }
        });
    }

    // fix rotation changes
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mAreDimensionsInvalid = true;
    }

    /**
     * Initialize time column width. Calculate value with all possible hours (supposed widest text).
     */
    private void initTextTimeWidth() {
        mTimeTextWidth = 0;
        for (int i = 0; i < 24; i++) {
            // Measure time string and get max width.
            String time = getDateTimeInterpreter().interpretTime(i);
            if (time == null)
                throw new IllegalStateException("A DateTimeInterpreter must not return null time");
            mTimeTextWidth = Math.max(mTimeTextWidth, mTimeTextPaint.measureText(time));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        System.out.println("[onDraw]");

        mHeaderColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 2;
        mHeaderHeight = mHeaderTextHeight + mAllDayEventHeight + mHeaderMarginBottom;

        mWidthPerDay = getWidth() - mHeaderColumnWidth - mNumberOfVisibleDays - 1;
        mWidthPerDay = mWidthPerDay / mNumberOfVisibleDays;

        if (mAreDimensionsInvalid) {
            mEffectiveMinHourHeight= Math.max(mMinHourHeight, (int) ((getHeight() - mHeaderHeight - mHeaderRowPadding * 2 - mHeaderMarginBottom) / 24));

            mAreDimensionsInvalid = false;
            if(mScrollToDay != null)
                goToDate(mScrollToDay);

            mAreDimensionsInvalid = false;
            if(mScrollToHour >= 0)
                goToHour(mScrollToHour);

            mScrollToDay = null;
            mScrollToHour = -1;
            mAreDimensionsInvalid = false;
        }

        // Calculate the new height due to the zooming.
        if (mNewHourHeight > 0){
            if (mNewHourHeight < mEffectiveMinHourHeight)
                mNewHourHeight = mEffectiveMinHourHeight;
            else if (mNewHourHeight > mMaxHourHeight)
                mNewHourHeight = mMaxHourHeight;

            mCurrentOrigin.y = (mCurrentOrigin.y / mHourHeight) * mNewHourHeight;
            mHourHeight = mNewHourHeight;
            mNewHourHeight = -1;
        }

        // 스크롤 상/하 오버스크롤 방지
        mCurrentOrigin.y = Math.max(getHeight() - mHourHeight * 24 - mHeaderHeight - mHeaderRowPadding * 2 - mTimeTextHeight / 2, Math.min(mCurrentOrigin.y, 0));

        int standard = (int) -(Math.ceil(mCurrentOrigin.x / mWidthPerDay));
        float startPixel = mCurrentOrigin.x + mWidthPerDay * standard + mHeaderColumnWidth;
        float holdX = startPixel;

        System.out.println("Day Standard: "+standard);
        /*
         for (int dayNumber = standard + 1;
             dayNumber <= standard + mNumberOfVisibleDays + 1;
             dayNumber++)
         */

        Calendar today = today();
        Calendar firstDayOfWeek = (Calendar) today.clone();
        firstDayOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        clearRectangleCache();

        loadAllWeekEvents(firstDayOfWeek);

        drawScheduleGrid(canvas, firstDayOfWeek, startPixel);
        for(int drawWhat = DRAW_EACH_EVENT_AREA; drawWhat < 2; drawWhat++){
            for (int dayNumber = standard + 1;
                 dayNumber <= standard + mNumberOfVisibleDays + 1;
                 dayNumber++){

                Calendar date = (Calendar) firstDayOfWeek.clone();
                date.add(Calendar.DATE, dayNumber - 1);

                DayType dt = isTodayPast(date)
                        ? DayType.PAST
                        : isToday(date) ? DayType.TODAY : DayType.COMMON;

                switch(drawWhat){
                    case DRAW_EACH_EVENT_AREA: {
                        // 일반 이벤트 입력
                        drawEachEvent(canvas, date, startPixel, dt);
                        // 예약 하려고 하는 영역 이벤트 입력
                        drawTemporaryEvent(canvas, date, startPixel);
                    }
                    break;

                    case DRAW_ALLDAY_EVENT_AREA: {
                        // 일자 입력
                        drawWeekDayTextHeader(canvas, date, startPixel, dt);
                        // 종일 이벤트 입력
                        drawAlldayEvent(canvas, date, startPixel, dt);
                    }
                    break;
                }

                startPixel += mWidthPerDay;
            }

            startPixel = holdX;
        }

        drawTimeColumnAndAxes(canvas);
        drawLeftTopEdgeToBlank(canvas);
    }

    private void clearRectangleCache(){
        if (mEventRects != null) {
            for (EventRect eventRect: mEventRects) {
                eventRect.rectF = null;
            }
        }
    }

    private void loadAllWeekEvents(Calendar firstDayOfWeek){
        if(mWeekViewLoader == null) throw new IllegalStateException("MonthChangeListener 에서 데이터를 주입해야 함");
        if(mEventRects == null) mEventRects = new ArrayList<>();
        else mEventRects.clear();

        List<? extends WeekViewEvent> events = mWeekViewLoader.onLoad(
                firstDayOfWeek.get(Calendar.YEAR),
                firstDayOfWeek.get(Calendar.MONTH) + 1,
                firstDayOfWeek.get(Calendar.WEEK_OF_MONTH),
                firstDayOfWeek.get(Calendar.DATE));

        if(events == null) return;

        List<EventRect> cached = new ArrayList<>();

        sortAndCacheEvents(events, cached);

        while (cached.size() > 0) {
            ArrayList<EventRect> er = new ArrayList<>(cached.size());

            // Get first event for a day.
            EventRect er1 = cached.remove(0);
            er.add(er1);

            int i = 0;
            while (i < cached.size()) {
                EventRect er2 = cached.get(i);

                // 겹치는 일정 들을 묶어 보내도록 한다.
                // 조건 1. 이벤트 들이 서로 종일 이면서 일정이 겹치는 경우 (종일 이벤트가 타고 들어감)
                // 조건 2. 같은 날에 이벤트가 이뤄지는 경우 (일반 이벤트가 타고 들어감)
                if((er1.event.isAllDay() == er2.event.isAllDay() && isEventsCollide(er1.event, er2.event))
                        || isSameDay(er1.event.getStartTime(), er2.event.getStartTime())){
                    cached.remove(i);
                    er.add(er2);
                }else{
                    i++;
                }
            }

            overlapEvents(er);
        }
    }


    /**
     * 스케쥴 표 측면의 시간 나열을 그려준다.
     * @param canvas
     */
    private void drawTimeColumnAndAxes(Canvas canvas) {
        canvas.save();

        // 타임라인 범위
        canvas.drawRect(0, mHeaderHeight + mHeaderRowPadding * 2, mHeaderColumnWidth, getHeight(), mHeaderColumnBackgroundPaint);

        // Clip to paint in left column only.
        canvas.clipRect(0, mHeaderHeight + mHeaderRowPadding * 2, mHeaderColumnWidth, getHeight());
        canvas.restore();

        for (int i = 0; i < 24; i++) {
            float top = mHeaderHeight + mHeaderRowPadding * 2 + mCurrentOrigin.y + mHourHeight * i + mHeaderMarginBottom;

            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            String time = getDateTimeInterpreter().interpretTime(i);
            if (time == null)
                throw new IllegalStateException("A DateTimeInterpreter must not return null time");
            if (top < getHeight()) canvas.drawText(time, mTimeTextWidth + mHeaderColumnPadding, top + mTimeTextHeight, mTimeTextPaint);
        }
    }

    /**
     * 한 주의 요일을 표시하는 부분
     *
     * @param canvas
     * @param date 당일
     * @param offsetX Draw 가 되고 있는 X 좌표
     * @param dayType 일자 별 타입
     */
    private void drawWeekDayTextHeader(Canvas canvas, Calendar date, float offsetX, DayType dayType){
        canvas.save();

        // Draw the day labels.
        String dayLabel = getDateTimeInterpreter().interpretDate(date);
        if (dayLabel == null)
            throw new IllegalStateException("A DateTimeInterpreter must not return null date");

        float x = offsetX + (mWidthPerDay / 2),
                y = mHeaderTextHeight + mHeaderRowPadding;

        canvas.drawRect(x, 0, getWidth(), mHeaderTextHeight + mHeaderRowPadding * 2, mDayBackgroundPaint);

        switch(date.get(Calendar.DAY_OF_WEEK)){
            case Calendar.SUNDAY:
                canvas.drawText(dayLabel, x, y, mSundayTextPaint);
                break;
            case Calendar.SATURDAY:
                canvas.drawText(dayLabel, x, y, mSaturdayTextPaint);
                break;
            default: {
                if(dayType == DayType.TODAY){
                    canvas.drawRoundRect(offsetX, mHeaderRowPadding / 2f, offsetX + mWidthPerDay, (mHeaderRowPadding / 2f) + y, mEventCornerRadius, mEventCornerRadius, mSaturdayTextPaint);
                    canvas.drawText(dayLabel, x, y, mTodayHeaderTextPaint);
                }else{
                    canvas.drawText(dayLabel, x, y, mHeaderTextPaint);
                }
            }

            break;
        }

        canvas.restore();
    }

    private void drawScheduleGrid(Canvas canvas, Calendar firstDayOfWeek, float offsetX){
        int lineCount = (int) ((getHeight() - mHeaderHeight - mHeaderRowPadding * 2 - mHeaderMarginBottom) / mHourHeight) + 1;
        lineCount = (lineCount) * (mNumberOfVisibleDays + 1);

        float[] hLine = new float[lineCount * 4];
        float[] vLine = new float[lineCount * 4];

        float x1 = 0, y1 = 0, x2 = 0, y2 = 0;

        float offsetY = mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + (mTimeTextHeight / 2);

        Calendar day = (Calendar) firstDayOfWeek.clone();
        for(int dayOfWeek = Calendar.SUNDAY; dayOfWeek <= Calendar.SATURDAY + 1; dayOfWeek++) {
            day.set(Calendar.DAY_OF_WEEK, dayOfWeek);

            float start =  Math.max(offsetX, mHeaderColumnWidth);

            // 각 일자의 배경색을 설정할 수 있음
            if (mWidthPerDay + offsetX - start > 0){
                canvas.drawRect(start, offsetY, offsetX + mWidthPerDay, getHeight(), mDayBackgroundPaint);
            }

            // Prepare the separator lines for hours.
            int i = 0;
            for (int hourNumber = 0; hourNumber < 25; hourNumber++) {
                float top = offsetY + mCurrentOrigin.y + (mHourHeight * hourNumber);
                if (top > offsetY - mHourSeparatorHeight && top < getHeight() && offsetX + mWidthPerDay - start > 0){

                    if(i < 24) {
                        hLine[i * 4] = start;
                        hLine[i * 4 + 1] = top;
                        hLine[i * 4 + 2] = offsetX + mWidthPerDay;
                        hLine[i * 4 + 3] = top;
                    }

                    x1 = start;
                    y1 = top + 10;
                    x2 = start;
                    y2 = top - (getHourHeight() * 2);

                    vLine[i * 4] = x1;
                    vLine[i * 4 + 1] = y1;
                    vLine[i * 4 + 2] = x2;
                    vLine[i * 4 + 3] = y2;

                    i++;
                }
            }

            // Draw the lines for hours.
            canvas.drawLines(hLine, mHourSeparatorPaint);
            canvas.drawLines(vLine, mHourSeparatorPaint);

            // draw the last line
            canvas.drawLine(x1 , y1 + getHourHeight(), x2 , y1, mHourSeparatorPaint);

            offsetX += mWidthPerDay;
        }
    }

    private void drawEachEvent(Canvas canvas, Calendar date, float offsetX, DayType dayType){
        canvas.save();

        // Draw background color for each day.
        float start =  Math.max(offsetX, mHeaderColumnWidth);
        float offsetY = mHeaderHeight + (mHeaderRowPadding * 2) + mHeaderMarginBottom + (mTimeTextHeight / 2);

        canvas.clipRect(mHeaderColumnWidth, offsetY, getWidth(), getHeight());

        // 이벤트를 그린다.
        drawEvents(canvas, date, offsetX, dayType);

        // 현재 시간 위치를 그려준다
        if (mShowNowLine && dayType == DayType.TODAY){
            float startY = offsetY + mCurrentOrigin.y;
            Calendar now = Calendar.getInstance();

            float beforeNow = (now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0f) * mHourHeight;
            canvas.drawLine(start, startY + beforeNow, offsetX + mWidthPerDay, startY + beforeNow, mNowLinePaint);
        }

        canvas.restore();
    }

    private void drawTemporaryEvent(Canvas canvas, Calendar date, float offsetX){
        if(mTempEventRect == null) return;

        canvas.save();

        WeekViewEvent e = mTempEventRect.event;
        EventRect er = mTempEventRect;

        if (isSameDay(e.getStartTime(), date)){
            float offsetY = mCurrentOrigin.y + mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight/2;

            float top = mHourHeight * 24 * er.top / 1440 + offsetY;
            float bottom = mHourHeight * 24 * er.bottom / 1440 + offsetY;
            float left = offsetX + (er.left * mWidthPerDay);
            float right = left + (er.width * mWidthPerDay);

            System.out.println(String.format("[Temporary] Same Day L:%.1f T:%.1f R:%.1f B:%.1f", left, top, right, bottom));

            if (left < right &&
                    left < getWidth() &&
                    top < getHeight() &&
                    right > mHeaderColumnWidth &&
                    bottom > mHeaderHeight + mHeaderRowPadding * 2 + mTimeTextHeight / 2 + mHeaderMarginBottom
            ) {
                er.rectF = new RectF(left, top, right, bottom);

                canvas.drawRoundRect(er.rectF, mEventCornerRadius, mEventCornerRadius, mTempBackgroundPaint);
            }
            else
                er.rectF = null;
        }

        canvas.restore();
    }

    /**
     * 종일 이벤트 블럭을 그려준다.
     *
     * @param canvas
     * @param date
     * @param offsetX
     */
    private void drawAlldayEvent(Canvas canvas, Calendar date, float offsetX, DayType dayType){
        if(mEventRects.isEmpty()) return;

        for(EventRect r : mEventRects){
            WeekViewEvent e = r.event;

            if(!e.isAllDay()) continue;

            if(isSameDay(e.getStartTime(), date)){
                float left = Math.max(0, offsetX);
                float right = left + (r.width * mWidthPerDay);

                float offsetY = (mHeaderRowPadding * 2) + mHeaderMarginBottom + (mTimeTextHeight / 2);

                float top = offsetY + (mHeaderHeight * r.top);
                float bottom = offsetY + r.top + (mHeaderHeight * r.bottom);

                // 이벤트를 그리고 그 위에 해당 이벤트의 텍스트를 삽입한다
                if (left < right
                        && left < getWidth()
                        && top < getHeight()
                        && right > mHeaderColumnWidth
                        && bottom > 0) {

                    r.rectF = new RectF(left, top, right, bottom);

                    switch(dayType){
                        case PAST: mEventBackgroundPaint.setColor(mPastEventBackgroundColor); break;
                        default: mEventBackgroundPaint.setColor(e.getColor() == 0 ? mDefaultEventColor : e.getColor()); break;
                    }

                    if(e.isFrontEvent()){
                        canvas.drawRoundRect(r.rectF.left, r.rectF.top, r.rectF.left + 30, r.rectF.bottom, mEventCornerRadius, mEventCornerRadius, mEventBackgroundPaint);
                        canvas.drawRect(r.rectF.left + 20, r.rectF.top, r.rectF.right, r.rectF.bottom, mEventBackgroundPaint);
                    }

                    if(e.isCenterEvent()){
                        canvas.drawRect(r.rectF, mEventBackgroundPaint);
                    }

                    if(e.isRearEvent()){
                        int diff = r.originalEvent.getEventDayDifference();

                        canvas.drawRect(r.rectF.left, r.rectF.top, r.rectF.right - 20, r.rectF.bottom, mEventBackgroundPaint);
                        canvas.drawRoundRect(r.rectF.right - 30, r.rectF.top, r.rectF.right, r.rectF.bottom, mEventCornerRadius, mEventCornerRadius, mEventBackgroundPaint);

                        drawAlldayEventTitle(canvas, e, right - ((right - left) * diff), top, (int) ((right - left) * diff));
                    }

                    if(e.isSingleEvent()){
                        canvas.drawRoundRect(r.rectF, mEventCornerRadius, mEventCornerRadius, mEventBackgroundPaint);
                        drawAlldayEventTitle(canvas, e, left, top, (int) (right - left));
                    }
                }
                else
                    r.rectF = null;
            }
        }
    }

    private void drawLeftTopEdgeToBlank(Canvas canvas){
        canvas.save();
        canvas.drawRect(0, 0,
                mHeaderColumnWidth,
                mHeaderHeight + (mHeaderRowPadding * 2) + mHeaderMarginBottom,
                mHeaderBackgroundPaint);
        canvas.restore();
    }

    /**
     * Get the time and date where the user clicked on.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private Calendar getTimeFromPoint(float x, float y){
        Calendar date = (Calendar) today().clone();
        date.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        int leftDaysWithGaps = (int) -(Math.ceil(mCurrentOrigin.x / mWidthPerDay));
        float startPixel = mCurrentOrigin.x + mWidthPerDay * leftDaysWithGaps + mHeaderColumnWidth;

        for (int dayNumber = leftDaysWithGaps + 1;
             dayNumber <= leftDaysWithGaps + mNumberOfVisibleDays + 1;
             dayNumber++) {
            float start = Math.max(startPixel, mHeaderColumnWidth);

            if (mWidthPerDay + startPixel - start > 0 && x > start && x < startPixel + mWidthPerDay){
                date.add(Calendar.DATE, dayNumber - 1);

                float pixelsFromZero = y - mCurrentOrigin.y - mHeaderHeight - mHeaderRowPadding * 2 - mTimeTextHeight/2 - mHeaderMarginBottom;

                int hour = (int)(pixelsFromZero / mHourHeight);
                int minute = (int) (60 * (pixelsFromZero - hour * mHourHeight) / mHourHeight);

                date.add(Calendar.HOUR, hour);
                date.set(Calendar.MINUTE, minute);

                return date;
            }

            startPixel += mWidthPerDay;
        }

        return null;
    }

    /**
     * Draw all the events of a particular day.
     * @param date The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas The canvas to draw upon.
     * @param dayType 일자마다 다른 타입
     */
    private void drawEvents(Canvas canvas, Calendar date, float startFromPixel, DayType dayType) {
        if(mEventRects.isEmpty()) return;

        for (EventRect er : mEventRects) {
            WeekViewEvent e = er.event;

            if(e.isAllDay() || e.isTemporary()) continue;

            if (isSameDay(e.getStartTime(), date)){
                float offsetY = mCurrentOrigin.y + mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight/2;

                float top = mHourHeight * 24 * er.top / 1440 + offsetY;
                float bottom = mHourHeight * 24 * er.bottom / 1440 + offsetY;
                float left = startFromPixel + er.left * mWidthPerDay;
                float right = left + er.width * mWidthPerDay;

                // Draw the event and the event name on top of it.
                if (left < right &&
                        left < getWidth() &&
                        top < getHeight() &&
                        right > mHeaderColumnWidth &&
                        bottom > mHeaderHeight + mHeaderRowPadding * 2 + mTimeTextHeight / 2 + mHeaderMarginBottom) {
                    er.rectF = new RectF(left, top, right, bottom);

                    switch(dayType){
                        case PAST: mEventBackgroundPaint.setColor(mPastEventBackgroundColor); break;
                        default: mEventBackgroundPaint.setColor(e.getColor() == 0 ? mDefaultEventColor : e.getColor()); break;
                    }

                    canvas.drawRoundRect(er.rectF, mEventCornerRadius, mEventCornerRadius, mEventBackgroundPaint);

                    drawEventTitle(e, er.rectF, canvas, top, left);
                }
                else
                    er.rectF = null;
            }
        }
    }

    /**
     * 종일 체크한 경우의 이벤트 명을 그려주는 부분.
     * 이벤트 명을 그려주는 시점은 해당 이벤트 블록들이 모두 다 그려진 후에 그 위에 덧그리도록 한다.
     * 이벤트 시작 할 때 함께 그리게 되면 블록이 이벤트 명을 덮게 된다.
     *
     * @param canvas 캔버스 객체
     * @param event 스케쥴 이벤트 객체
     * @param r Rect 객체
     * @param barWidth 이벤트 Bar UI 셀 넓이
     */
    private void drawAlldayEventTitle(Canvas canvas, WeekViewEvent event, float left, float top, int barWidth){
        // 이벤트 이름 준비
        SpannableStringBuilder bob = new SpannableStringBuilder();
        if (event.getName() != null) {
            bob.append(event.getName());
            bob.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, bob.length(), 0);
            bob.append(' ');
        }

        mEventTextPaint.setColor(Color.BLACK);

        // Get text dimensions.
        StaticLayout textLayout = new StaticLayout(
                TextUtils.ellipsize(
                        bob,
                        mEventTextPaint,
                        barWidth, TextUtils.TruncateAt.END),
                mEventTextPaint,
                barWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, false);

        canvas.save();

        System.out.println("Width: "+getWidth()+" left: "+left);

        canvas.translate(left, top + mEventPadding);
        textLayout.draw(canvas);

        canvas.restore();
    }

    /**
     * Draw the name of the event on top of the event rectangle.
     * @param event The event of which the title (and location) should be drawn.
     * @param rect The rectangle on which the text is to be drawn.
     * @param canvas The canvas to draw upon.
     * @param originalTop The original top position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     * @param originalLeft The original left position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     */
    private void drawEventTitle(WeekViewEvent event, RectF rect, Canvas canvas, float originalTop, float originalLeft) {
        if (rect.right - rect.left - mEventPadding * 2 < 0) return;
        if (rect.bottom - rect.top - mEventPadding * 2 < 0) return;

        // Prepare the name of the event.
        SpannableStringBuilder bob = new SpannableStringBuilder();
        if (event.getName() != null) {
            bob.append(event.getName());
            bob.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, bob.length(), 0);
            bob.append(' ');
        }

        int availableHeight = (int) (rect.bottom - originalTop - mEventPadding * 2);
        int availableWidth = (int) (rect.right - originalLeft - mEventPadding * 2);

        // Get text dimensions.
        StaticLayout textLayout = new StaticLayout(bob, mEventTextPaint, availableWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        int lineHeight = textLayout.getHeight() / textLayout.getLineCount();

        if (availableHeight >= lineHeight) {
            // Calculate available number of line counts.
            int availableLineCount = availableHeight / lineHeight;
            do {
                // Ellipsize text to fit into event rect.
                textLayout = new StaticLayout(TextUtils.ellipsize(bob, mEventTextPaint, availableLineCount * availableWidth, TextUtils.TruncateAt.END), mEventTextPaint, (int) (rect.right - originalLeft - mEventPadding * 2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                // Reduce line count.
                availableLineCount--;

                // Repeat until text is short enough.
            } while (textLayout.getHeight() > availableHeight);

            // Draw text.
            canvas.save();
            canvas.translate(originalLeft + mEventPadding, originalTop + mEventPadding);
            textLayout.draw(canvas);
            canvas.restore();
        }
    }

    private void doDrawTemporaryEvent(Calendar selected){
        WeekViewEvent e = new WeekViewEvent();

        Calendar assumeStartTime = (Calendar) selected.clone();
        Calendar assumeEndTime = (Calendar) assumeStartTime.clone();
        assumeEndTime.add(Calendar.HOUR_OF_DAY, 1);

        e.setStartTime(assumeStartTime);
        e.setEndTime(assumeEndTime);
        e.setColor(Color.MAGENTA);

        mTempEventRect = new EventRect(e, e, null);
        mTempEventRect.left = 0f;
        mTempEventRect.width = 1f;
        mTempEventRect.top = e.getStartTime().get(Calendar.HOUR_OF_DAY) * 60 + e.getStartTime().get(Calendar.MINUTE);
        mTempEventRect.bottom = e.getEndTime().get(Calendar.HOUR_OF_DAY) * 60 + e.getEndTime().get(Calendar.MINUTE);

        invalidate();
    }

    /**
     * A class to hold reference to the events and their visual representation. An EventRect is
     * actually the rectangle that is drawn on the calendar for a given event. There may be more
     * than one rectangle for a single event (an event that expands more than one day). In that
     * case two instances of the EventRect will be used for a single event. The given event will be
     * stored in "originalEvent". But the event that corresponds to rectangle the rectangle
     * instance will be stored in "event".
     */
    private class EventRect {
        public WeekViewEvent event;
        public WeekViewEvent originalEvent;
        public RectF rectF;
        public float left;
        public float width;
        public float top;
        public float bottom;

        /**
         * Create a new instance of event rect. An EventRect is actually the rectangle that is drawn
         * on the calendar for a given event. There may be more than one rectangle for a single
         * event (an event that expands more than one day). In that case two instances of the
         * EventRect will be used for a single event. The given event will be stored in
         * "originalEvent". But the event that corresponds to rectangle the rectangle instance will
         * be stored in "event".
         * @param event Represents the event which this instance of rectangle represents.
         * @param originalEvent The original event that was passed by the user.
         * @param rectF The rectangle.
         */
        public EventRect(WeekViewEvent event, WeekViewEvent originalEvent, RectF rectF) {
            this.event = event;
            this.rectF = rectF;
            this.originalEvent = originalEvent;
        }

        @NonNull
        @Override
        public String toString() { return event.toString()+String.format(" (%.1f, %.1f, %.1f, %.1f)", left, top, width, bottom); }
    }

    /**
     * Cache the event for smooth scrolling functionality.
     * @param event The event to cache.
     */
    private void cacheEvent(WeekViewEvent event, List<EventRect> cached) {
        if(event.getStartTime().compareTo(event.getEndTime()) >= 0) return;

        List<WeekViewEvent> splitedEvents = event.splitWeekViewEvents();
        for(WeekViewEvent splitedEvent: splitedEvents){
            cached.add(new EventRect(splitedEvent, event, null));
        }
    }

    /**
     * Sort and cache events.
     * @param events The events to be sorted and cached.
     */
    private void sortAndCacheEvents(List<? extends WeekViewEvent> events, List<EventRect> result) {
        sortEvents(events);

        for (WeekViewEvent event : events) {
            cacheEvent(event, result);
        }
    }

    /**
     * Sorts the events in ascending order.
     * @param events The events to be sorted.
     */
    private void sortEvents(List<? extends WeekViewEvent> events) {
        Collections.sort(events, new Comparator<WeekViewEvent>() {
            @Override
            public int compare(WeekViewEvent event1, WeekViewEvent event2) {
                long start1 = event1.getStartTime().getTimeInMillis();
                long start2 = event2.getStartTime().getTimeInMillis();
                int comparator = start1 > start2 ? 1 : (start1 < start2 ? -1 : 0);
                if (comparator == 0) {
                    long end1 = event1.getEndTime().getTimeInMillis();
                    long end2 = event2.getEndTime().getTimeInMillis();
                    comparator = end1 > end2 ? 1 : (end1 < end2 ? -1 : 0);
                }

                return comparator;
            }
        });
    }

    /**
     * 시간이 겹치는 이벤트에 대해서 그룹핑을 하기위해 한번 돌려준다.
     * 그룹핑 된 이벤트들은 dispatchEventDimension 으로 던져지며 자신의 위치를 가지게 된다.
     *
     * @param eventRects The events along with their wrapper class.
     */
    private void overlapEvents(List<EventRect> eventRects) {
        List<List<EventRect>> collisionGroups = new ArrayList<List<EventRect>>();
        for (EventRect eventRect : eventRects) {
            boolean isPlaced = false;

            for (List<EventRect> collisionGroup : collisionGroups) {
                for (EventRect groupEvent : collisionGroup) {

                    if (isEventsCollide(groupEvent.event, eventRect.event)
                            && groupEvent.event.isAllDay() == eventRect.event.isAllDay()) {
                        collisionGroup.add(eventRect);
                        isPlaced = true;
                        break;
                    }
                }
            }

            if (!isPlaced) {
                List<EventRect> newGroup = new ArrayList<EventRect>();
                newGroup.add(eventRect);
                collisionGroups.add(newGroup);
            }
        }

        for (List<EventRect> collisionGroup : collisionGroups) {
            dispatchEventDimension(collisionGroup);
        }
    }

    /**
     * 각 이벤트 들이 위치할 곳들을 정의 하는 곳
     * 이벤트가 겹치는 경우를 비롯해 모든 포지션은 해당 메소드에서 관리한다.
     *
     * @param collisionGroup The group of events which overlap with each other.
     */
    private void dispatchEventDimension(List<EventRect> collisionGroup) {
        List<List<EventRect>> spans = new ArrayList<List<EventRect>>();
        spans.add(new ArrayList<EventRect>());
        for (EventRect eventRect : collisionGroup) {
            boolean isPlaced = false;

            for (List<EventRect> span : spans) {
                if (span.isEmpty()) {
                    span.add(eventRect);

                    isPlaced = true;
                }else if (!isEventsCollide(eventRect.event, span.get(span.size()-1).event)) {
                    span.add(eventRect);

                    isPlaced = true;
                    break;
                }
            }

            if (!isPlaced) {
                List<EventRect> newSpan = new ArrayList<EventRect>();

                newSpan.add(eventRect);
                spans.add(newSpan);
            }
        }

        int maxSpanCount = 0;
        for (List<EventRect> span : spans){
            maxSpanCount = Math.max(maxSpanCount, span.size());
        }

        for (int i = 0; i < maxSpanCount; i++) {
            float j = 0;

            for (List<EventRect> span : spans) {
                if (span.size() - 1 >= i) {
                    EventRect er = span.get(i);
                    WeekViewEvent evt = er.event;

                    if(!evt.isAllDay()){
                        // 일반 스케쥴인 경우에는 Horizontal 로 겹치는 Stack 을 표현하기 위해서
                        // left 와 width 를 조절할 수 있도록 한다.
                        er.left = j / spans.size();
                        er.width = 1f / spans.size();

                        er.top = evt.getStartTime().get(Calendar.HOUR_OF_DAY) * 60 + evt.getStartTime().get(Calendar.MINUTE);
                        er.bottom = evt.getEndTime().get(Calendar.HOUR_OF_DAY) * 60 + evt.getEndTime().get(Calendar.MINUTE);

                        mEventRects.add(er);
                    }else{
                        /*
                        // 종일 스케쥴인 경우에는 Vertical 로 겹치는 Stack 을 표현하기 위해
                        // top 과 bottom 을 조절할 수 있도록 한다.
                        er.top = j / spans.size();
                        er.bottom = er.top + (1f / spans.size());

                        // Left 와 Width 의 경우에는, 종일 스케쥴은 시간 관계가 없기 때문에
                        // Left 는 0 으로 설정하고 width 는 시작과 끝의 일수 차이로 그릴 수 있도록 한다.
                        er.left = 0;
                        er.width = Math.round((evt.getEndTimeMillis() - evt.getStartTimeMillis()) / (24f * 60 * 60 * 1000));
                        */

                        float top = j / spans.size(), bottom = top + (1f / spans.size()), left = 0, width = 1f;

                        List<? extends WeekViewEvent> events = evt.splitWeekViewEvents(true);
                        for(WeekViewEvent e: events){
                            EventRect newER = new EventRect(e, evt, null);
                            newER.top = top;
                            newER.bottom = bottom;
                            newER.left = left;
                            newER.width = width;

                            mEventRects.add(newER);
                        }
                    }
                }

                j++;
            }
        }
    }

    /**
     * Checks if two events overlap.
     * @param event1 The first event.
     * @param event2 The second event.
     * @return true if the events overlap.
     */
    private boolean isEventsCollide(WeekViewEvent event1, WeekViewEvent event2) {
        long start1 = event1.getStartTime().getTimeInMillis();
        long end1 = event1.getEndTime().getTimeInMillis();
        long start2 = event2.getStartTime().getTimeInMillis();
        long end2 = event2.getEndTime().getTimeInMillis();

        return start1 <= end2 && start2 <= end1;
    }


    /**
     * Checks if time1 occurs after (or at the same time) time2.
     * @param time1 The time to check.
     * @param time2 The time to check against.
     * @return true if time1 and time2 are equal or if time1 is after time2. Otherwise false.
     */
    private boolean isTimeAfterOrEquals(Calendar time1, Calendar time2) {
        return !(time1 == null || time2 == null) && time1.getTimeInMillis() >= time2.getTimeInMillis();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mAreDimensionsInvalid = true;
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to setting and getting the properties.
    //
    /////////////////////////////////////////////////////////////////

    public void setOnEventClickListener (EventClickListener listener) {
        this.mEventClickListener = listener;
    }

    public EventClickListener getEventClickListener() {
        return mEventClickListener;
    }

    public @Nullable MonthLoader.MonthChangeListener getMonthChangeListener() {
        if (mWeekViewLoader instanceof MonthLoader)
            return ((MonthLoader) mWeekViewLoader).getOnMonthChangeListener();
        return null;
    }

    public void setMonthChangeListener(MonthLoader.MonthChangeListener monthChangeListener) {
        this.mWeekViewLoader = new MonthLoader(monthChangeListener);
    }

    /**
     * Get event loader in the week view. Event loaders define the  interval after which the events
     * are loaded in week view. For a MonthLoader events are loaded for every month. You can define
     * your custom event loader by extending WeekViewLoader.
     * @return The event loader.
     */
    public WeekViewLoader getWeekViewLoader(){
        return mWeekViewLoader;
    }

    /**
     * Set event loader in the week view. For example, a MonthLoader. Event loaders define the
     * interval after which the events are loaded in week view. For a MonthLoader events are loaded
     * for every month. You can define your custom event loader by extending WeekViewLoader.
     * @param loader The event loader.
     */
    public void setWeekViewLoader(WeekViewLoader loader){
        this.mWeekViewLoader = loader;
    }

    public EventLongPressListener getEventLongPressListener() {
        return mEventLongPressListener;
    }

    public void setEventLongPressListener(EventLongPressListener eventLongPressListener) {
        this.mEventLongPressListener = eventLongPressListener;
    }

    public void setEmptyViewClickListener(EmptyViewClickListener emptyViewClickListener){
        this.mEmptyViewClickListener = emptyViewClickListener;
    }

    public EmptyViewClickListener getEmptyViewClickListener(){
        return mEmptyViewClickListener;
    }

    public void setEmptyViewLongPressListener(EmptyViewLongPressListener emptyViewLongPressListener){
        this.mEmptyViewLongPressListener = emptyViewLongPressListener;
    }

    public EmptyViewLongPressListener getEmptyViewLongPressListener(){
        return mEmptyViewLongPressListener;
    }

    public void setScrollListener(ScrollListener scrolledListener){
        this.mScrollListener = scrolledListener;
    }

    public ScrollListener getScrollListener(){
        return mScrollListener;
    }

    public void setPinchListener(PinchListener pinchListener){
        this.pinchListener = pinchListener;
    }

    public PinchListener getPinchListener(){
        return pinchListener;
    }

    /**
     * Get the interpreter which provides the text to show in the header column and the header row.
     * @return The date, time interpreter.
     */
    public DateTimeInterpreter getDateTimeInterpreter() {
        if (mDateTimeInterpreter == null) {
            mDateTimeInterpreter = new DateTimeInterpreter() {
                @Override
                public String interpretDate(Calendar date) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE M/dd", Locale.getDefault());
                        return sdf.format(date.getTime()).toUpperCase();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "";
                    }
                }

                @Override
                public String interpretTime(int hour) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, 0);

                    try {
                        SimpleDateFormat sdf = DateFormat.is24HourFormat(getContext()) ? new SimpleDateFormat("HH:mm", Locale.getDefault()) : new SimpleDateFormat("hh a", Locale.getDefault());
                        return sdf.format(calendar.getTime());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "";
                    }
                }
            };
        }
        return mDateTimeInterpreter;
    }

    /**
     * Set the interpreter which provides the text to show in the header column and the header row.
     * @param dateTimeInterpreter The date, time interpreter.
     */
    public void setDateTimeInterpreter(DateTimeInterpreter dateTimeInterpreter){
        this.mDateTimeInterpreter = dateTimeInterpreter;

        // Refresh time column width.
        initTextTimeWidth();
    }


    /**
     * Get the number of visible days in a week.
     * @return The number of visible days in a week.
     */
    public int getNumberOfVisibleDays() {
        return mNumberOfVisibleDays;
    }

    /**
     * Set the number of visible days in a week.
     * @param numberOfVisibleDays The number of visible days in a week.
     */
    public void setNumberOfVisibleDays(int numberOfVisibleDays) {
        this.mNumberOfVisibleDays = numberOfVisibleDays;
        mCurrentOrigin.x = 0;
        mCurrentOrigin.y = 0;
        invalidate();
    }

    public int getHourHeight() {
        return mHourHeight;
    }

    public void setHourHeight(int hourHeight) {
        mNewHourHeight = hourHeight;
        invalidate();
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    /**
     * Set the first day of the week. First day of the week is used only when the week view is first
     * drawn. It does not of any effect after user starts scrolling horizontally.
     * <p>
     *     <b>Note:</b> This method will only work if the week view is set to display more than 6 days at
     *     once.
     * </p>
     * @param firstDayOfWeek The supported values are {@link java.util.Calendar#SUNDAY},
     * {@link java.util.Calendar#MONDAY}, {@link java.util.Calendar#TUESDAY},
     * {@link java.util.Calendar#WEDNESDAY}, {@link java.util.Calendar#THURSDAY},
     * {@link java.util.Calendar#FRIDAY}.
     */
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        mFirstDayOfWeek = firstDayOfWeek;
        invalidate();
    }

    public int getTextSize() {
        return mTextSize;
    }

    public void setTextSize(int textSize) {
        mTextSize = textSize;
        mTodayHeaderTextPaint.setTextSize(mTextSize);
        mHeaderTextPaint.setTextSize(mTextSize);
        mTimeTextPaint.setTextSize(mTextSize);
        invalidate();
    }

    public int getHeaderColumnPadding() {
        return mHeaderColumnPadding;
    }

    public void setHeaderColumnPadding(int headerColumnPadding) {
        mHeaderColumnPadding = headerColumnPadding;
        invalidate();
    }

    public int getHeaderColumnTextColor() {
        return mHeaderColumnTextColor;
    }

    public void setHeaderColumnTextColor(int headerColumnTextColor) {
        mHeaderColumnTextColor = headerColumnTextColor;
        mHeaderTextPaint.setColor(mHeaderColumnTextColor);
        mTimeTextPaint.setColor(mHeaderColumnTextColor);
        invalidate();
    }

    public int getHeaderRowPadding() {
        return mHeaderRowPadding;
    }

    public void setHeaderRowPadding(int headerRowPadding) {
        mHeaderRowPadding = headerRowPadding;
        invalidate();
    }

    public int getHeaderRowBackgroundColor() {
        return mHeaderRowBackgroundColor;
    }

    public void setHeaderRowBackgroundColor(int headerRowBackgroundColor) {
        mHeaderRowBackgroundColor = headerRowBackgroundColor;
        mHeaderBackgroundPaint.setColor(mHeaderRowBackgroundColor);
        invalidate();
    }

    public int getDayBackgroundColor() {
        return mDayBackgroundColor;
    }

    public void setDayBackgroundColor(int dayBackgroundColor) {
        mDayBackgroundColor = dayBackgroundColor;
        mDayBackgroundPaint.setColor(mDayBackgroundColor);
        invalidate();
    }

    public int getHourSeparatorColor() {
        return mHourSeparatorColor;
    }

    public void setHourSeparatorColor(int hourSeparatorColor) {
        mHourSeparatorColor = hourSeparatorColor;
        mHourSeparatorPaint.setColor(mHourSeparatorColor);
        invalidate();
    }

    public int getTodayBackgroundColor() {
        return mTodayBackgroundColor;
    }

    public void setTodayBackgroundColor(int todayBackgroundColor) {
        mTodayBackgroundColor = todayBackgroundColor;

        invalidate();
    }

    public int getHourSeparatorHeight() {
        return mHourSeparatorHeight;
    }

    public void setHourSeparatorHeight(int hourSeparatorHeight) {
        mHourSeparatorHeight = hourSeparatorHeight;
        mHourSeparatorPaint.setStrokeWidth(mHourSeparatorHeight);
        invalidate();
    }

    public int getTodayHeaderTextColor() {
        return mTodayHeaderTextColor;
    }

    public void setTodayHeaderTextColor(int todayHeaderTextColor) {
        mTodayHeaderTextColor = todayHeaderTextColor;
        mTodayHeaderTextPaint.setColor(mTodayHeaderTextColor);
        invalidate();
    }

    public int getEventTextSize() {
        return mEventTextSize;
    }

    public void setEventTextSize(int eventTextSize) {
        mEventTextSize = eventTextSize;
        mEventTextPaint.setTextSize(mEventTextSize);
        invalidate();
    }

    public int getEventTextColor() {
        return mEventTextColor;
    }

    public void setEventTextColor(int eventTextColor) {
        mEventTextColor = eventTextColor;
        mEventTextPaint.setColor(mEventTextColor);
        invalidate();
    }

    public int getEventPadding() {
        return mEventPadding;
    }

    public void setEventPadding(int eventPadding) {
        mEventPadding = eventPadding;
        invalidate();
    }

    public int getHeaderColumnBackgroundColor() {
        return mHeaderColumnBackgroundColor;
    }

    public void setHeaderColumnBackgroundColor(int headerColumnBackgroundColor) {
        mHeaderColumnBackgroundColor = headerColumnBackgroundColor;
        mHeaderColumnBackgroundPaint.setColor(mHeaderColumnBackgroundColor);
        invalidate();
    }

    public int getDefaultEventColor() {
        return mDefaultEventColor;
    }

    public void setDefaultEventColor(int defaultEventColor) {
        mDefaultEventColor = defaultEventColor;
        invalidate();
    }

    public int getEventCornerRadius() {
        return mEventCornerRadius;
    }

    /**
     * Set corner radius for event rect.
     *
     * @param eventCornerRadius the radius in px.
     */
    public void setEventCornerRadius(int eventCornerRadius) {
        mEventCornerRadius = eventCornerRadius;
    }

    /**
     * Get whether "now" line should be displayed. "Now" line is defined by the attributes
     * `nowLineColor` and `nowLineThickness`.
     * @return True if "now" line should be displayed.
     */
    public boolean isShowNowLine() {
        return mShowNowLine;
    }

    /**
     * Set whether "now" line should be displayed. "Now" line is defined by the attributes
     * `nowLineColor` and `nowLineThickness`.
     * @param showNowLine True if "now" line should be displayed.
     */
    public void setShowNowLine(boolean showNowLine) {
        this.mShowNowLine = showNowLine;
        invalidate();
    }

    /**
     * Get the "now" line color.
     * @return The color of the "now" line.
     */
    public int getNowLineColor() {
        return mNowLineColor;
    }

    /**
     * Set the "now" line color.
     * @param nowLineColor The color of the "now" line.
     */
    public void setNowLineColor(int nowLineColor) {
        this.mNowLineColor = nowLineColor;
        invalidate();
    }

    /**
     * Get the "now" line thickness.
     * @return The thickness of the "now" line.
     */
    public int getNowLineThickness() {
        return mNowLineThickness;
    }

    /**
     * Set the "now" line thickness.
     * @param nowLineThickness The thickness of the "now" line.
     */
    public void setNowLineThickness(int nowLineThickness) {
        this.mNowLineThickness = nowLineThickness;
        invalidate();
    }

    /**
     * Get the height of AllDay-events.
     * @return Height of AllDay-events.
     */
    public int getAllDayEventHeight() {
        return mAllDayEventHeight;
    }

    /**
     * Set the height of AllDay-events.
     */
    public void setAllDayEventHeight(int height) {
        mAllDayEventHeight = height;
    }

    /**
     * Get scroll duration
     * @return scroll duration
     */
    public int getScrollDuration() {
        return mScrollDuration;
    }

    /**
     * Set the scroll duration
     */
    public void setScrollDuration(int scrollDuration) {
        mScrollDuration = scrollDuration;
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to scrolling.
    //
    /////////////////////////////////////////////////////////////////

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        boolean val = mGestureDetector.onTouchEvent(event);

        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event.getAction() == MotionEvent.ACTION_UP && !mIsZooming && mCurrentFlingDirection == Direction.NONE) {
            if (mCurrentScrollDirection == Direction.RIGHT || mCurrentScrollDirection == Direction.LEFT) {
                goToNearestOrigin();
            }
            mCurrentScrollDirection = Direction.NONE;
        }

        return val;
    }

    private void goToNearestOrigin(){
        double leftDays = mCurrentOrigin.x / mWidthPerDay;

        if (mCurrentFlingDirection != Direction.NONE) {
            // snap to nearest day
            leftDays = Math.round(leftDays);
        } else if (mCurrentScrollDirection == Direction.LEFT) {
            // snap to last day
            leftDays = Math.floor(leftDays);
        } else if (mCurrentScrollDirection == Direction.RIGHT) {
            // snap to next day
            leftDays = Math.ceil(leftDays);
        } else {
            // snap to nearest day
            leftDays = Math.round(leftDays);
        }

        int nearestOrigin = (int) (mCurrentOrigin.x - leftDays * mWidthPerDay);

        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller.forceFinished(true);
            // Snap to date.
            mScroller.startScroll((int) mCurrentOrigin.x, (int) mCurrentOrigin.y, -nearestOrigin, 0, (int) (Math.abs(nearestOrigin) / mWidthPerDay * mScrollDuration));
            ViewCompat.postInvalidateOnAnimation(WeekView.this);
        }
        // Reset scrolling and fling direction.
        mCurrentScrollDirection = mCurrentFlingDirection = Direction.NONE;
    }


    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.isFinished()) {
            if (mCurrentFlingDirection != Direction.NONE) {
                // Snap to day after fling is finished.
                goToNearestOrigin();
            }
        } else {
            if (mCurrentFlingDirection != Direction.NONE && forceFinishScroll()) {
                goToNearestOrigin();
            } else if (mScroller.computeScrollOffset()) {
                mCurrentOrigin.y = mScroller.getCurrY();
                mCurrentOrigin.x = mScroller.getCurrX();
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    /**
     * Check if scrolling should be stopped.
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private boolean forceFinishScroll() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // current velocity only available since api 14
            return mScroller.getCurrVelocity() <= mMinimumFlingVelocity;
        } else {
            return false;
        }
    }


    /////////////////////////////////////////////////////////////////
    //
    //      Public methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Show today on the week view.
     */
    public void goToToday() {
        Calendar today = Calendar.getInstance();
        goToDate(today);
    }

    /**
     * Show a specific day on the week view.
     * @param date The date to show.
     */
    public void goToDate(Calendar date) {
        mScroller.forceFinished(true);
        mCurrentScrollDirection = mCurrentFlingDirection = Direction.NONE;

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        if(mAreDimensionsInvalid) {
            mScrollToDay = date;
            return;
        }

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long day = 1000L * 60L * 60L * 24L;
        long dateInMillis = date.getTimeInMillis() + date.getTimeZone().getOffset(date.getTimeInMillis());
        long todayInMillis = today.getTimeInMillis() + today.getTimeZone().getOffset(today.getTimeInMillis());
        long dateDifference = (dateInMillis/day) - (todayInMillis/day);
        mCurrentOrigin.x = - dateDifference * mWidthPerDay;
        invalidate();
    }

    /**
     * Refreshes the view and loads the events again.
     */
    public void notifyDatasetChanged(){
        invalidate();
    }

    /**
     * Vertically scroll to a specific hour in the week view.
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    public void goToHour(double hour){
        if (mAreDimensionsInvalid) {
            mScrollToHour = hour;
            return;
        }

        int verticalOffset = 0;
        if (hour > 24)
            verticalOffset = mHourHeight * 24;
        else if (hour > 0)
            verticalOffset = (int) (mHourHeight * hour);

        if (verticalOffset > mHourHeight * 24 - getHeight() + mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom)
            verticalOffset = (int)(mHourHeight * 24 - getHeight() + mHeaderHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom);

        mCurrentOrigin.y = -verticalOffset;
        invalidate();
    }

    /**
     * Get the first hour that is visible on the screen.
     * @return The first hour that is visible.
     */
    public double getFirstVisibleHour(){
        return -mCurrentOrigin.y / mHourHeight;
    }



    /////////////////////////////////////////////////////////////////
    //
    //      Interfaces.
    //
    /////////////////////////////////////////////////////////////////

    public interface EventClickListener {
        /**
         * Triggered when clicked on one existing event
         * @param event: event clicked.
         * @param eventRect: view containing the clicked event.
         */
        void onEventClick(WeekViewEvent event, RectF eventRect);
    }

    public interface EventLongPressListener {
        /**
         * Similar to {@link com.alamkanak.weekview.WeekView.EventClickListener} but with a long press.
         * @param event: event clicked.
         * @param eventRect: view containing the clicked event.
         */
        void onEventLongPress(WeekViewEvent event, RectF eventRect);
    }

    public interface EmptyViewClickListener {
        /**
         * Triggered when the users clicks on a empty space of the calendar.
         * @param time: {@link Calendar} object set with the date and time of the clicked position on the view.
         */
        void onEmptyViewClicked(Calendar time);
    }

    public interface EmptyViewLongPressListener {
        /**
         * Similar to {@link com.alamkanak.weekview.WeekView.EmptyViewClickListener} but with long press.
         * @param time: {@link Calendar} object set with the date and time of the long pressed position on the view.
         */
        void onEmptyViewLongPress(Calendar time);
    }

    public interface ScrollListener {
        /**
         * Called when the first visible day has changed.
         *
         * (this will also be called during the first draw of the weekview)
         * @param newFirstVisibleDay The new first visible day
         * @param oldFirstVisibleDay The old first visible day (is null on the first call).
         */
        void onFirstVisibleDayChanged(Calendar newFirstVisibleDay, Calendar oldFirstVisibleDay);
    }
}
