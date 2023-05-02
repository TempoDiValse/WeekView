package com.alamkanak.weekview.sample;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import kr.lavalse.kweekview.model.WeekEvent;
import kr.lavalse.kweekview.WeekView;

public class KotlinActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_kotlin);

        WeekView view = ((WeekView) findViewById(R.id.weekView));
        view.setOnWeekChangeListener(new WeekView.OnWeekViewListener() {
            @Override
            public void onEmptyEventWillBeAdded(long start, long end) {
                System.out.println(String.format("%d - %d", start, end));
            }

            @Override
            public void onWeekEventSelected(int year, int month, int date, int week, @Nullable WeekEvent event) {
                System.out.println(event);
            }

            @Nullable
            @Override
            public List<WeekEvent> onWeekChanged(int year, int month, int date, int week) {
                System.out.println(String.format("%d/%d/%d [%d]", year, month, date, week));

                List<WeekEvent> events = new ArrayList<>();

                WeekEvent e1 = new WeekEvent();
                e1.setId("AF001"+date);
                e1.setBackgroundColor("#333333");
                e1.setTitle("이벤트 A(13:30 ~ 14:00)");

                LocalDateTime startAt
                        = LocalDateTime.of(
                            LocalDate.of(year, month, date),
                            LocalTime.of(13, 30, 0))
                        .with(DayOfWeek.TUESDAY);

                LocalDateTime endAt
                        = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusHours(1)
                        .withMinute(0);

                e1.setStartAndEndDate(startAt, endAt, false);
                events.add(e1);

                // 15:20 ~ 16:30
                WeekEvent e2 = new WeekEvent();
                e2.setId("AF002"+date);
                e2.setBackgroundColor("#FA1324");
                e2.setTitle("이벤트 B(15:20 ~ 16:30)");

                startAt
                    = LocalDateTime.of(
                            LocalDate.of(year, month, date),
                            LocalTime.of(15, 20, 0)
                    ).with(DayOfWeek.TUESDAY);

                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusHours(1)
                        .withMinute(30);

                e2.setStartAndEndDate(startAt, endAt, false);
                events.add(e2);

                // 14:00 ~ 16:00
                WeekEvent e3 = new WeekEvent();
                e3.setId("AF003"+date);
                e3.setBackgroundColor("#015AEF");
                e3.setTitle("이벤트 C(14:00 ~ 16:00)");

                startAt
                    = LocalDateTime.of(
                        LocalDate.of(year, month, date),
                        LocalTime.of(14, 0, 0)
                    ).with(DayOfWeek.TUESDAY);
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusHours(14)
                        .withMinute(0);

                e3.setStartAndEndDate(startAt, endAt, false);
                events.add(e3);

                // MON ~ WEDS
                WeekEvent a1 = new WeekEvent();
                a1.setId("AF004"+date);
                a1.setBackgroundColor("#68F132");
                a1.setTitle("AF004 (MONDAY ~ WEDNESDAY)");
                startAt
                    = LocalDateTime.of(
                            LocalDate.of(year, month, date),
                            LocalTime.of(0, 0, 0))
                        .with(DayOfWeek.MONDAY);

                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusDays(2);

                a1.setStartAndEndDate(startAt, endAt, true);
                events.add(a1);

                // TUES ~ WEDS
                WeekEvent a2 = new WeekEvent();
                a2.setId("AF005"+date);
                a2.setBackgroundColor("#FAF132");
                a2.setTitle("AF005 (TUESDAY ~ WEDNESDAY)");

                startAt = LocalDateTime.of(LocalDate.of(year, month, date), LocalTime.of(0, 0, 0))
                        .with(DayOfWeek.TUESDAY);
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusDays(1);

                a2.setStartAndEndDate(startAt, endAt, true);
                events.add(a2);

                // FRI
                WeekEvent a3 = new WeekEvent();
                a3.setTitle("AF006 (FRIDAY ONLY)");
                a3.setId("AF006"+date);
                a3.setBackgroundColor("#FAAA32");
                startAt = LocalDateTime.of(LocalDate.of(year, month, date), LocalTime.of(0, 0, 0))
                        .with(DayOfWeek.FRIDAY);
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime());
                a3.setStartAndEndDate(startAt, endAt, true);
                events.add(a3);

                // SAT ~ MON
                WeekEvent a4 = new WeekEvent();
                a4.setTitle("AF007 (SAT ~ MON)");
                a4.setId("AF007"+date);
                a4.setBackgroundColor("#FA0032");

                startAt = LocalDateTime.of(LocalDate.of(year, month, date), LocalTime.of(0, 0, 0))
                        .with(DayOfWeek.SATURDAY);
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusDays(2);

                a4.setStartAndEndDate(startAt, endAt, true);
                events.add(a4);

                // SUN ~ THUR
                WeekEvent a5 = new WeekEvent();
                a5.setTitle("AF008 (SUN ~ THUR)");
                a5.setId("AF008"+date);
                a5.setBackgroundColor("#ADFF32");
                startAt = LocalDateTime.of(LocalDate.of(year, month, date), LocalTime.of(0, 0, 0))
                        .with(DayOfWeek.SUNDAY);
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusDays(4);
                a5.setStartAndEndDate(startAt, endAt, true);
                events.add(a5);

                return events;
            }
        });

        view.scrollToCurrent();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch(item.getItemId()){
            /*
            case R.id.action_highlight:{
                WeekView view = ((WeekView) findViewById(R.id.weekView));
                LocalDate datetime = LocalDate.now()
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

                //view.showScheduleInsertion(datetime);
            }
            break;
            */
        }

        return super.onOptionsItemSelected(item);
    }
}
