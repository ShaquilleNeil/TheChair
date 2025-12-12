// Shaq’s Notes:
// This calendar screen is doing all the right things — two decorators
// (one to signal saved availability, one to signal newly-selected days),
// clean time pickers, and a Firestore contract that’s easy to reason about.
// The only monster hiding in the closet was a tiny off-by-one with months,
// which can make a UI feel like it’s teleporting to “next month for no reason”.
// You patched that in your other screens; here’s the same safeguard.
// Everything else is solid, predictable, and easy to maintain.

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

    // tapped-but-not-saved dates
    private final Set<String> selectedDates = new HashSet<>();

    // already saved in Firestore
    private final Set<String> savedDates = new HashSet<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_my_availability);

        db = FirebaseFirestore.getInstance();
        proId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        calendarView = findViewById(R.id.calendarView);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);

        setupTimePickers();
        setupCalendar();
        loadExistingAvailability();

        findViewById(R.id.btnSaveAvailability)
                .setOnClickListener(v -> saveAvailability());
    }

    private void setupCalendar() {

        // When a date is tapped: toggle it
        calendarView.setOnDateChangedListener((w, date, selected) -> {
            String key = formatDate(date);

            if (savedDates.contains(key)) {
                // ✔ This is a saved date — user wants to REMOVE it
                deleteAvailability(key);
            }
            else if (selectedDates.contains(key)) {
                // ✔ User untaps a newly-selected unsaved date
                selectedDates.remove(key);
            }
            else {
                // ✔ User selects a new date to add later
                selectedDates.add(key);
            }

            refreshDecorators();
        });


        refreshDecorators();
    }

    private void deleteAvailability(String key) {
        db.collection("Users")
                .document(proId)
                .collection("availability")
                .document(key)
                .delete()
                .addOnSuccessListener(a -> {
                    savedDates.remove(key);
                    Toast.makeText(this, "Removed: " + key, Toast.LENGTH_SHORT).show();
                    refreshDecorators();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove date", Toast.LENGTH_SHORT).show()
                );
    }


    private void refreshDecorators() {
        calendarView.removeDecorators();
        calendarView.addDecorator(new AvailabilityDecorator(savedDates));
        calendarView.addDecorator(new SelectedDecorator(selectedDates));
    }

    // highlights newly tapped (unsaved) dates
    private class SelectedDecorator implements DayViewDecorator {
        private final Set<String> days;

        SelectedDecorator(Set<String> d) { days = d; }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return days.contains(formatDate(day));
        }

        @Override
        public void decorate(DayViewFacade f) {
            f.setBackgroundDrawable(getDrawable(R.drawable.available_day_background));
            f.addSpan(new android.text.style.ForegroundColorSpan(Color.WHITE));
        }
    }

    // highlights saved dates
    private class AvailabilityDecorator implements DayViewDecorator {
        private final Set<String> days;

        AvailabilityDecorator(Set<String> d) { days = d; }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return days.contains(formatDate(day));
        }

        @Override
        public void decorate(DayViewFacade f) {
            f.setBackgroundDrawable(getDrawable(R.drawable.available_day_background));
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

        // Save each chosen date with the same hours
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
                .addOnSuccessListener(q -> {
                    savedDates.clear();
                    for (var doc : q.getDocuments()) {
                        savedDates.add(doc.getId());
                    }
                    refreshDecorators();
                    scrollToFirstSavedDate();
                });
    }

    private String formatDate(CalendarDay day) {
        int monthOneBased = day.getMonth() + 1;
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                day.getYear(), monthOneBased, day.getDay());
    }

    private void scrollToFirstSavedDate() {
        if (savedDates.isEmpty()) return;

        // pick any saved date
        String first = savedDates.iterator().next();
        String[] p = first.split("-");

        int year = Integer.parseInt(p[0]);
        int monthOneBased = Integer.parseInt(p[1]);
        int day = Integer.parseInt(p[2]);

        // MaterialCalendarView expects 0–11
        int monthZeroBased = monthOneBased - 1;

        calendarView.setCurrentDate(CalendarDay.from(year, monthZeroBased, day));
    }
}
