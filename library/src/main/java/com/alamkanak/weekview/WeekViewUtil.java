package com.alamkanak.weekview;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by jesse on 6/02/2016.
 */
public class WeekViewUtil {


    /////////////////////////////////////////////////////////////////
    //
    //      Helper methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Checks if two times are on the same day.
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    public static boolean isSameDay(Calendar dayOne, Calendar dayTwo) {
        return dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR) && dayOne.get(Calendar.DAY_OF_YEAR) == dayTwo.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isTodayPast(Calendar date){
        Calendar today = today();

        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) > date.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isToday(Calendar date){
        Calendar today = today();

        return isSameDay(today, date);
    }

    /**
     * Returns a calendar instance at the start of this day
     * @return the calendar instance
     */
    public static Calendar today(){
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

    public static String getFormattedDate(Calendar date){
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        return df.format(date.getTime());
    }

    public static int getDayDifference(Calendar startDate, Calendar endDate){
        long diff = endDate.getTimeInMillis() - startDate.getTimeInMillis();

        return Math.round(diff / (24f * 60 * 60 * 1000));
    }
}
