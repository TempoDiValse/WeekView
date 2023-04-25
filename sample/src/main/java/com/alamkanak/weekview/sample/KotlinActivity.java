package com.alamkanak.weekview.sample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import kr.lavalse.kweekview.model.WeekEvent;
import kr.lavalse.kweekview.WeekView;

public class KotlinActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_kotlin);

        ((WeekView) findViewById(R.id.weekView)).setOnWeekChangeListener(new WeekView.OnWeekChangeListener() {
            @Nullable
            @Override
            public List<WeekEvent> onWeekChanged(int year, int month, int date, int week) {
                System.out.println(String.format("%d/%d/%d [%d]", year, month, date, week));

                List<WeekEvent> events = new ArrayList<>();

                // 13:30 ~ 17:30
                WeekEvent e1 = new WeekEvent();
                e1.setId("AF001");
                e1.setBackgroundColor("#333333");
                Calendar startAt = Calendar.getInstance();
                startAt.set(Calendar.WEEK_OF_YEAR, week);
                startAt.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                startAt.set(Calendar.HOUR_OF_DAY, 13);
                startAt.set(Calendar.MINUTE, 30);

                Calendar endAt = (Calendar) startAt.clone();
                endAt.add(Calendar.HOUR_OF_DAY, 1);
                endAt.set(Calendar.MINUTE, 0);
                e1.setStartAndEndDate(startAt, endAt, false);
                events.add(e1);

                // 15:20 ~ 16:30
                WeekEvent e2 = new WeekEvent();
                e2.setId("AF002");
                e2.setBackgroundColor("#FA1324");
                startAt = Calendar.getInstance();
                startAt.set(Calendar.WEEK_OF_YEAR, week);
                startAt.set(Calendar.HOUR_OF_DAY, 15);
                startAt.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                startAt.set(Calendar.MINUTE, 20);
                endAt = (Calendar) startAt.clone();
                endAt.add(Calendar.HOUR_OF_DAY, 1);
                endAt.set(Calendar.MINUTE, 30);
                e2.setStartAndEndDate(startAt, endAt, false);
                events.add(e2);

                // 14:00 ~ 16:00
                WeekEvent e3 = new WeekEvent();
                e3.setId("AF003");
                e3.setBackgroundColor("#015AEF");
                startAt = Calendar.getInstance();
                startAt.set(Calendar.WEEK_OF_YEAR, week);
                startAt.set(Calendar.HOUR_OF_DAY, 14);
                startAt.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                startAt.set(Calendar.MINUTE, 0);
                endAt = (Calendar) startAt.clone();
                endAt.add(Calendar.HOUR_OF_DAY, 14);
                endAt.set(Calendar.MINUTE, 0);
                e3.setStartAndEndDate(startAt, endAt, false);
                events.add(e3);

                WeekEvent a1 = new WeekEvent();
                a1.setId("AF004");
                a1.setBackgroundColor("#68F132");
                startAt = Calendar.getInstance();
                startAt.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                endAt = (Calendar) startAt.clone();
                endAt.add(Calendar.DATE, 2);
                a1.setStartAndEndDate(startAt, endAt, true);
                events.add(a1);

                WeekEvent a2 = new WeekEvent();
                a2.setId("AF005");
                a2.setBackgroundColor("#FAF132");
                startAt = Calendar.getInstance();
                startAt.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                endAt = (Calendar) startAt.clone();
                endAt.add(Calendar.DATE, 1);
                a2.setStartAndEndDate(startAt, endAt, true);
                events.add(a2);

                WeekEvent a3 = new WeekEvent();
                a3.setId("AF006");
                a3.setBackgroundColor("#FAAA32");
                startAt = Calendar.getInstance();
                startAt.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                endAt = (Calendar) startAt.clone();
                a3.setStartAndEndDate(startAt, endAt, true);
                events.add(a3);

                return events;
            }
        });
    }
}
