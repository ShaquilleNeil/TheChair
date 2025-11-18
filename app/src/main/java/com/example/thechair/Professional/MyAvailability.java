package com.example.thechair.Professional;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.Locale;
import java.util.HashMap;
import java.util.Set;

public class MyAvailability extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView tvStartTime, tvEndTime;

    private FirebaseFirestore db;
    private String proId;

    private String startTime = null;
    private String endTime = null;

    private final Set<String> selectedDates = new HashSet<>();
    private final Set<String> savedDates = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_availability);

        db = FirebaseFirestore.getInstance();
        proId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        calendarView = findViewById(R.id.calendarView);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);

        setupTimePickers();
        setupCalendar();
        loadExistingAvailability();

        findViewById(R.id.btnSaveAvailability).setOnClickListener(v -> saveAvailability());
    }

    private void setupCalendar() {

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            String key = formatDate(date);

            if (selectedDates.contains(key)) {
                loadTimesForDate(key);
                selectedDates.remove(key);
            } else {
                selectedDates.add(key);
            }

            refreshDecorators();
        });

        refreshDecorators();
    }

    private void refreshDecorators() {
        calendarView.removeDecorators();
        calendarView.addDecorator(new AvailabilityDecorator(savedDates));
        calendarView.addDecorator(new SelectedDecorator(selectedDates));
    }

    private class SelectedDecorator implements DayViewDecorator {
        private final Set<String> days;

        SelectedDecorator(Set<String> days) { this.days = days; }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return days.contains(formatDate(day));
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(getDrawable(R.drawable.available_day_background));
            view.addSpan(new android.text.style.ForegroundColorSpan(Color.WHITE));
        }
    }

    private class AvailabilityDecorator implements DayViewDecorator {
        private final Set<String> days;

        AvailabilityDecorator(Set<String> days) { this.days = days; }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return days.contains(formatDate(day));
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(getDrawable(R.drawable.available_day_background));
        }
    }

    private void setupTimePickers() {
        tvStartTime.setOnClickListener(v -> {
            new TimePickerDialog(
                    this,
                    (view, hour, minute) -> {
                        startTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                        tvStartTime.setText(startTime);
                    },
                    9, 0, true
            ).show();
        });

        tvEndTime.setOnClickListener(v -> {
            new TimePickerDialog(
                    this,
                    (view, hour, minute) -> {
                        endTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                        tvEndTime.setText(endTime);
                    },
                    17, 0, true
            ).show();
        });
    }

    private void saveAvailability() {

        if (selectedDates.isEmpty()) {
            Toast.makeText(this, "Select at least one date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startTime == null || endTime == null) {
            Toast.makeText(this, "Select start and end time", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String key : selectedDates) {
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

        savedDates.addAll(selectedDates);
        selectedDates.clear();

        refreshDecorators();
        scrollToFirstSavedDate();
    }

    private void loadTimesForDate(String key) {
        db.collection("Users")
                .document(proId)
                .collection("availability")
                .document(key)
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

    private void loadExistingAvailability() {
        db.collection("Users")
                .document(proId)
                .collection("availability")
                .get()
                .addOnSuccessListener(query -> {
                    savedDates.clear();
                    for (var doc : query.getDocuments()) {
                        savedDates.add(doc.getId());
                    }
                    refreshDecorators();
                    scrollToFirstSavedDate();
                });
    }

    private String formatDate(CalendarDay day) {
        int month = day.getMonth() + 1;
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                day.getYear(), month, day.getDay());
    }

    private void scrollToFirstSavedDate() {
        if (savedDates.isEmpty()) return;

        String first = savedDates.iterator().next();

        String[] p = first.split("-");
        int year = Integer.parseInt(p[0]);
        int month = Integer.parseInt(p[1]);
        int day = Integer.parseInt(p[2]);

        calendarView.setCurrentDate(CalendarDay.from(year, month, day));
    }
}
