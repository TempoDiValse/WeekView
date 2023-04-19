package com.alamkanak.weekview;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.alamkanak.weekview.WeekViewUtil.*;
import static com.alamkanak.weekview.WeekViewUtil.getDayDifference;

import androidx.annotation.NonNull;

/**
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://april-shower.com
 */
public class WeekViewEvent {
    public static final int EVENT_TYPE_TEMPORARY = 0x1324;
    public static final int EVENT_TYPE_NORMAL = 0x1325;
    public static final int EVENT_TYPE_ALLDAY = 0x1326;
    public static final int EVENT_POSITION_SINGLE = 0x1026;
    public static final int EVENT_POSITION_FRONT = 0x1027;
    public static final int EVENT_POSITION_CENTER = 0x1028;
    public static final int EVENT_POSITION_REAR = 0x1029;

    private long mId;
    private Calendar mStartTime;
    private Calendar mEndTime;
    private String mName;
    private int mColor;

    private int eventType = EVENT_TYPE_NORMAL;
    private int allDayEventPosition = EVENT_POSITION_SINGLE;

    public WeekViewEvent(){
        eventType = EVENT_TYPE_TEMPORARY;
    }

    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startYear Year when the event starts.
     * @param startMonth Month when the event starts.
     * @param startDay Day when the event starts.
     * @param startHour Hour (in 24-hour format) when the event starts.
     * @param startMinute Minute when the event starts.
     * @param endYear Year when the event ends.
     * @param endMonth Month when the event ends.
     * @param endDay Day when the event ends.
     * @param endHour Hour (in 24-hour format) when the event ends.
     * @param endMinute Minute when the event ends.
     */
    public WeekViewEvent(long id, String name, int startYear, int startMonth, int startDay, int startHour, int startMinute, int endYear, int endMonth, int endDay, int endHour, int endMinute) {
        this.mId = id;

        this.mStartTime = Calendar.getInstance();
        this.mStartTime.set(Calendar.YEAR, startYear);
        this.mStartTime.set(Calendar.MONTH, startMonth-1);
        this.mStartTime.set(Calendar.DAY_OF_MONTH, startDay);
        this.mStartTime.set(Calendar.HOUR_OF_DAY, startHour);
        this.mStartTime.set(Calendar.MINUTE, startMinute);

        this.mEndTime = Calendar.getInstance();
        this.mEndTime.set(Calendar.YEAR, endYear);
        this.mEndTime.set(Calendar.MONTH, endMonth-1);
        this.mEndTime.set(Calendar.DAY_OF_MONTH, endDay);
        this.mEndTime.set(Calendar.HOUR_OF_DAY, endHour);
        this.mEndTime.set(Calendar.MINUTE, endMinute);

        this.mName = name;
    }

    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     * @param allDay Is the event an all day event.
     */
    public WeekViewEvent(long id, String name, Calendar startTime, Calendar endTime, boolean allDay) {
        this.mId = id;
        this.mName = name;
        this.mStartTime = startTime;
        this.mEndTime = endTime;

        this.eventType = allDay ? EVENT_TYPE_ALLDAY : EVENT_TYPE_NORMAL;
    }

    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     */
    public WeekViewEvent(long id, String name, Calendar startTime, Calendar endTime) {
        this(id, name, startTime, endTime, false);
    }


    public Calendar getStartTime() {
        return mStartTime;
    }
    public long getStartTimeMillis() { return getStartTime().getTimeInMillis(); }

    public void setStartTime(Calendar startTime) {
        this.mStartTime = startTime;
    }

    public Calendar getEndTime() {
        return mEndTime;
    }
    public long getEndTimeMillis() { return getEndTime().getTimeInMillis(); }

    public void setEndTime(Calendar endTime) {
        this.mEndTime = endTime;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
    }

    public boolean isAllDay() { return eventType == EVENT_TYPE_ALLDAY; }
    public boolean isTemporary() { return eventType == EVENT_TYPE_TEMPORARY; }

    public void setAllDay(boolean allDay) { this.eventType = allDay ? EVENT_TYPE_ALLDAY : EVENT_TYPE_TEMPORARY; }
    private void setAllDayEventPosition(int position){
        this.allDayEventPosition = position;
    }

    public int getEventDayDifference(){ return getDayDifference(getStartTime(), getEndTime()); }

    public boolean isFrontEvent(){ return allDayEventPosition == EVENT_POSITION_FRONT; }
    public boolean isCenterEvent(){ return allDayEventPosition == EVENT_POSITION_CENTER; }
    public boolean isRearEvent(){ return allDayEventPosition == EVENT_POSITION_REAR; }
    public boolean isSingleEvent(){ return allDayEventPosition == EVENT_POSITION_SINGLE; }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeekViewEvent that = (WeekViewEvent) o;

        return mId == that.mId;
    }

    @Override
    public int hashCode() {
        return (int) (mId ^ (mId >>> 32));
    }

    public List<WeekViewEvent> splitWeekViewEvents(){ return splitWeekViewEvents(false); }

    /**
     * Day 가 넘어 가는 경우에 23시 59분 기점으로 하나의 이벤트를 여러 Date 로 잘라버리는 메소드
     */
    public List<WeekViewEvent> splitWeekViewEvents(boolean force){
        List<WeekViewEvent> events = new ArrayList<WeekViewEvent>();
        // The first millisecond of the next day is still the same day. (no need to split events for this).
        Calendar endTime = (Calendar) this.getEndTime().clone();
        endTime.add(Calendar.MILLISECOND, -1);

        if(isAllDay() && !force){
            events.add(this);
            return events;
        }

        if (!isSameDay(this.getStartTime(), endTime)) {
            endTime = (Calendar) this.getStartTime().clone();
            endTime.set(Calendar.HOUR_OF_DAY, 23);
            endTime.set(Calendar.MINUTE, 59);
            endTime.set(Calendar.SECOND, 59);

            WeekViewEvent event1 = new WeekViewEvent(this.getId(), this.getName(), this.getStartTime(), endTime);
            event1.setColor(this.getColor());
            if(isAllDay()){
                event1.setAllDay(true);
                event1.setAllDayEventPosition(EVENT_POSITION_FRONT);
            }

            events.add(event1);

            // Add other days.
            Calendar otherDay = (Calendar) this.getStartTime().clone();
            otherDay.add(Calendar.DATE, 1);

            while (!isSameDay(otherDay, this.getEndTime())) {
                Calendar overDay = (Calendar) otherDay.clone();
                overDay.set(Calendar.HOUR_OF_DAY, 0);
                overDay.set(Calendar.MINUTE, 0);

                Calendar endOfOverDay = (Calendar) overDay.clone();
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23);
                endOfOverDay.set(Calendar.MINUTE, 59);
                endOfOverDay.set(Calendar.SECOND, 59);

                WeekViewEvent eventMore = new WeekViewEvent(this.getId(), this.getName(), overDay, endOfOverDay);
                eventMore.setColor(this.getColor());
                if(isAllDay()){
                    eventMore.setAllDay(true);
                    eventMore.setAllDayEventPosition(EVENT_POSITION_CENTER);
                }

                events.add(eventMore);

                // Add next day.
                otherDay.add(Calendar.DATE, 1);
            }

            // Add last day.
            Calendar startTime = (Calendar) this.getEndTime().clone();
            startTime.set(Calendar.HOUR_OF_DAY, 0);
            startTime.set(Calendar.MINUTE, 0);

            if(startTime.getTimeInMillis() == getEndTimeMillis()){
                WeekViewEvent lastEvent = events.get(events.size() - 1);

                lastEvent.setAllDayEventPosition(EVENT_POSITION_REAR);
            }else{
                WeekViewEvent event2 = new WeekViewEvent(this.getId(), this.getName(), startTime, this.getEndTime());
                event2.setColor(this.getColor());
                if(isAllDay()){
                    event2.setAllDay(true);
                    event2.setAllDayEventPosition(EVENT_POSITION_REAR);
                }

                events.add(event2);
            }
        } else {
            events.add(this);
        }

        if(events.size() == 1){
            WeekViewEvent e = events.get(0);

            if(e.isAllDay()){
                e.setAllDayEventPosition(EVENT_POSITION_SINGLE);
            }
        }

        return events;
    }

    @NonNull
    @Override
    public String toString() {
        String start = WeekViewUtil.getFormattedDate(getStartTime());
        String end = WeekViewUtil.getFormattedDate(getEndTime());

        if(!isAllDay()){
            return String.format("[%d] %s ~ %s", getId(), start, end);
        }else{
            return String.format("[%d] %s ~ %s (AllDay)", getId(), start, end);
        }
    }
}
