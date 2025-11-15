package com.example.thechair.Professional;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MyAvailability extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView tvStartTime, tvEndTime;
    private FirebaseFirestore db;
    private String proId;

    private String startTime = null;
    private String endTime = null;

    private final List<LocalDate> selectedDates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_availability);

        db = FirebaseFirestore.getInstance();
        proId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        calendarView = findViewById(R.id.calendarView);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);

        setupCalendar();
        loadExistingAvailability();

        setupTimePickers();

        findViewById(R.id.btnSaveAvailability).setOnClickListener(v -> saveAvailability());
    }

    /** -------------------- CALENDAR SETUP -------------------- **/
    private void setupCalendar() {
        YearMonth currentMonth = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentMonth = YearMonth.now();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            calendarView.setup(
                    currentMonth.minusMonths(12),
                    currentMonth.plusMonths(12),
                    DayOfWeek.MONDAY
            );
        }

        calendarView.scrollToMonth(currentMonth);

        calendarView.setDayBinder(new MyDayBinder());
    }

    /** -------------------- DAY BINDER -------------------- **/
    private class MyDayBinder implements MonthDayBinder<DayViewContainer> {

        @Override
        public DayViewContainer create(android.view.View view) {
            return new DayViewContainer(view);
        }

        @Override
        public void bind(DayViewContainer container, CalendarDay day) {
            container.bind(day);
        }
    }

    private class DayViewContainer extends ViewContainer {

        TextView dayText;
        CalendarDay day;

        DayViewContainer(android.view.View view) {
            super(view);
            dayText = view.findViewById(R.id.dayText);

            view.setOnClickListener(v -> {
                if (day == null || day.getPosition() != DayPosition.MonthDate)
                    return;

                LocalDate date = day.getDate();

                if (selectedDates.contains(date)) {
                    loadTimesForDate(date);
                    selectedDates.remove(date);
                } else {
                    selectedDates.add(date);
                }

                calendarView.notifyDateChanged(date);

            });
        }

        void bind(CalendarDay day) {
            this.day = day;

            LocalDate date = day.getDate();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dayText.setText(String.valueOf(date.getDayOfMonth()));
            }

            if (selectedDates.contains(date)) {
                dayText.setBackgroundColor(Color.BLACK);
                dayText.setTextColor(Color.WHITE);
            } else {
                dayText.setBackgroundColor(Color.TRANSPARENT);
                dayText.setTextColor(Color.BLACK);
            }
        }
    }

    /** -------------------- TIME PICKERS -------------------- **/
    private void setupTimePickers() {

        tvStartTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hour, minute) -> {
                        startTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                        tvStartTime.setText(startTime);
                    },
                    9, 0, true
            );
            dialog.show();
        });

        tvEndTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hour, minute) -> {
                        endTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                        tvEndTime.setText(endTime);
                    },
                    17, 0, true
            );
            dialog.show();
        });
    }

    /** -------------------- SAVE AVAILABILITY -------------------- **/
    private void saveAvailability() {

        if (selectedDates.isEmpty()) {
            Toast.makeText(this, "Select at least one date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startTime == null || endTime == null) {
            Toast.makeText(this, "Select start and end time", Toast.LENGTH_SHORT).show();
            return;
        }

        for (LocalDate date : selectedDates) {
            String key = date.toString(); // YYYY-MM-DD

            HashMap<String, Object> data = new HashMap<>();
            data.put("date", key);
            data.put("startTime", startTime);
            data.put("endTime", endTime);

            db.collection("Users")
                    .document(proId)
                    .collection("availability")
                    .document(key)
                    .set(data, SetOptions.merge());

        }

        Toast.makeText(this, "Availability saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadExistingAvailability() {

        db.collection("Users")  // or "Users" depending on your structure
                .document(proId)
                .collection("availability")
                .get()
                .addOnSuccessListener(query -> {

                    selectedDates.clear();

                    for (var doc : query.getDocuments()) {
                        String dateStr = doc.getString("date");
                        if (dateStr != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            LocalDate d = LocalDate.parse(dateStr);
                            selectedDates.add(d);
                        }
                    }

                    // refresh the entire calendar so selected dates highlight
                    calendarView.notifyCalendarChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load availability", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTimesForDate(LocalDate date) {
        db.collection("Users")
                .document(proId)
                .collection("availability")
                .document(date.toString())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        startTime = doc.getString("startTime");
                        endTime = doc.getString("endTime");

                        if (startTime != null) tvStartTime.setText(startTime);
                        if (endTime != null) tvEndTime.setText(endTime);
                    }
                });
    }


}
