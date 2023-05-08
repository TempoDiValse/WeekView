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
            public void onCurrentWeekChanged(int year, int month, int date, int week) {
                LocalDate startLd = LocalDate.of(year, month, date);
                LocalDate endLd = startLd.plusDays(6);

                if(startLd.getYear() == endLd.getYear() && startLd.getMonth() == endLd.getMonth()){
                    setTitle(String.format("%d년 %d월", year, month));
                }else{
                    setTitle(String.format("%d년 %d월 ~ %d년 %d월", startLd.getYear(), startLd.getMonthValue(), endLd.getYear(), endLd.getMonthValue()));
                }
            }

            @Override
            public void onEmptyEventWillBeAdded(long start, long end) {
                System.out.println(String.format("Be Batched: %d - %d", start, end));
            }

            @Override
            public void onWeekEventSelected(int year, int month, int date, int week, @Nullable WeekEvent event) {
                System.out.println("Selected: "+event);
            }

            @Nullable
            @Override
            public List<WeekEvent> onWeekChanged(int year, int month, int date, int week) {
                List<WeekEvent> events = new ArrayList<>();

                WeekEvent e1 = new WeekEvent();
                e1.setId("AF001"+date);
                e1.setBackgroundColor("#333333");
                e1.setTitle("이벤트 A(13:30 ~ 14:00)");

                LocalDateTime startAt
                        = LocalDateTime.of(
                            LocalDate.of(year, month, date),
                            LocalTime.of(13, 30, 0))
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

                LocalDateTime endAt
                        = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusHours(1)
                        .withMinute(0);

                e1.setStartAndEndDate(startAt, endAt, false);
                events.add(e1);

                // 15:20 ~ 16:30
                WeekEvent e2 = new WeekEvent();
                e2.setId("AF002"+date);
                e2.setBackgroundColor("#E37964");
                e2.setTitle("이벤트 B(15:20 ~ 16:30)");

                startAt
                    = LocalDateTime.of(
                            LocalDate.of(year, month, date),
                            LocalTime.of(15, 20, 0)
                    ).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

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
                    ).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusHours(14)
                        .withMinute(0);

                e3.setStartAndEndDate(startAt, endAt, false);
                events.add(e3);

                WeekEvent e4 = new WeekEvent();
                e4.setId("AF009"+date);
                e4.setBackgroundColor("#FFD034");
                e4.setTitle("이벤트 D(17:20 ~ 19:50)");

                startAt
                        = LocalDateTime.of(
                        LocalDate.of(year, month, date),
                        LocalTime.of(17, 20, 0)
                ).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusHours(2)
                        .withMinute(30);

                e4.setStartAndEndDate(startAt, endAt, false);
                events.add(e4);

                WeekEvent e5 = new WeekEvent();
                e5.setId("AF010"+date);
                e5.setBackgroundColor("#EA29FA");
                e5.setTitle("이벤트 D(15:50 ~ 18:00)");

                startAt
                        = LocalDateTime.of(
                        LocalDate.of(year, month, date),
                        LocalTime.of(15, 50, 0)
                ).with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));

                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusHours(2);

                e5.setStartAndEndDate(startAt, endAt, false);
                events.add(e5);

                // MON ~ WEDS
                WeekEvent a1 = new WeekEvent();
                a1.setId("AF004"+date);
                a1.setBackgroundColor("#38F132");
                a1.setTitle("AF004 (MONDAY ~ WEDNESDAY)");
                startAt
                    = LocalDateTime.of(
                            LocalDate.of(year, month, date),
                            LocalTime.of(0, 0, 0))
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusDays(2);

                a1.setStartAndEndDate(startAt, endAt, true);
                events.add(a1);

                // TUES ~ WEDS
                WeekEvent a2 = new WeekEvent();
                a2.setId("AF005"+date);
                a2.setBackgroundColor("#FAFFFF");
                a2.setTitle("AF005 (TUESDAY ~ WEDNESDAY)");

                startAt = LocalDateTime.of(LocalDate.of(year, month, date), LocalTime.of(0, 0, 0))
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
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
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime());
                a3.setStartAndEndDate(startAt, endAt, true);
                events.add(a3);

                // SAT ~ MON
                WeekEvent a4 = new WeekEvent();
                a4.setTitle("AF007 (SAT ~ MON)");
                a4.setId("AF007"+date);
                a4.setBackgroundColor("#FA0032");

                startAt = LocalDateTime.of(LocalDate.of(year, month, date), LocalTime.of(0, 0, 0))
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
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
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                endAt = LocalDateTime.of(startAt.toLocalDate(), startAt.toLocalTime())
                        .plusDays(4);
                a5.setStartAndEndDate(startAt, endAt, true);
                events.add(a5);

                return events;
            }
        });

        view.scrollToTime(9);
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
            case R.id.action_prev: {
                WeekView view = ((WeekView) findViewById(R.id.weekView));

                view.moveToPrev();
            }
            break;

            case R.id.action_next: {
                WeekView view = ((WeekView) findViewById(R.id.weekView));

                view.moveToNext();
            }
            break;

            case R.id.action_scroll_to: {
                WeekView view = ((WeekView) findViewById(R.id.weekView));

                view.scrollToTime(6, true);
            }
            break;

            case R.id.action_today: {
                WeekView view = ((WeekView) findViewById(R.id.weekView));

                view.moveToToday();
            }
            break;

            case R.id.action_create: {
                WeekView view = ((WeekView) findViewById(R.id.weekView));

                LocalDateTime ldt = LocalDateTime.now()
                        .withDayOfMonth(12)
                        .withHour(15)
                        .withMinute(20);

                view.createSchedule(ldt);
            }
            break;
        }

        return super.onOptionsItemSelected(item);
    }
}
