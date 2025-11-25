// Shaq’s Notes:
// This screen is the whole flow where a customer lands on a pro’s schedule,
// sees which days are open, taps a date, sees real-time filtered timeslots,
// and jumps into ConfirmBooking with the full payload.
//
// The logic is clean: load availability days → decorate calendar → fetch
// specific daily availability → subtract booked ranges → build half-hour slots
// → launch the next screen with safe extras. Month-offset errors are fixed,
// booking overlap is respected, and the adapter callback is kept tight.

package com.example.thechair.Customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thechair.Adapters.TimeSlotAdapter;
import com.example.thechair.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PickDateActivity extends AppCompatActivity {

    // UI
    TextView tvProName, tvServiceName, tvServicePrice, tvServiceDuration,
            tvProProfession, tvSelectedDate;
    ImageView profileImage;
    RecyclerView rviewTimeSlots;

    // Calendar
    MaterialCalendarView calendarView;

    // Data
    String professionalId;
    String selectedTime;

    List<String> availableDates = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pick_date);

        tvProName        = findViewById(R.id.tvProName);
        tvServiceName    = findViewById(R.id.tvServiceName);
        tvServicePrice   = findViewById(R.id.tvServicePrice);
        tvServiceDuration= findViewById(R.id.tvServiceDuration);
        tvProProfession  = findViewById(R.id.proProfession);
        tvSelectedDate   = findViewById(R.id.tvSelectedDate);
        profileImage     = findViewById(R.id.profileImage);
        calendarView     = findViewById(R.id.calendarView);
        rviewTimeSlots   = findViewById(R.id.rviewTimeSlots);

        // Pull incoming data
        professionalId = getIntent().getStringExtra("professionalId");
        tvProName.setText(getIntent().getStringExtra("professionalName"));
        tvProProfession.setText(getIntent().getStringExtra("professionalProfession"));
        tvServiceName.setText("Service: " + getIntent().getStringExtra("serviceName"));
        tvServicePrice.setText("Price: $" + getIntent().getStringExtra("servicePrice"));
        tvServiceDuration.setText("Duration: " + getIntent().getStringExtra("serviceDuration") + " Minutes");

        String img = getIntent().getStringExtra("professionalProfilePic");
        if (img != null) Glide.with(this).load(img).into(profileImage);

        loadAvailableDates();
    }

    // ================================================
    //                 LOAD DATES
    // ================================================
    private void loadAvailableDates() {
        db.collection("Users")
                .document(professionalId)
                .collection("availability")
                .get()
                .addOnSuccessListener(q -> {
                    availableDates.clear();
                    for (var d : q.getDocuments())
                        availableDates.add(d.getId());  // doc ID = yyyy-MM-dd

                    highlightDates();
                    scrollToFirstAvailable();
                });
    }

    // Highlight specific days on calendar
    private void highlightDates() {
        List<CalendarDay> highlights = new ArrayList<>();

        for (String key : availableDates) {
            CalendarDay cd = keyToCalendarDay(key);
            if (cd != null) highlights.add(cd);
        }

        calendarView.addDecorator(new DayViewDecorator() {
            @Override public boolean shouldDecorate(CalendarDay day) {
                return highlights.contains(day);
            }

            @Override public void decorate(DayViewFacade v) {
                v.setBackgroundDrawable(
                        getDrawable(R.drawable.available_day_background)
                );
            }
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {

            String key = calendarDayToKey(date);

            // Not available
            if (!availableDates.contains(key)) {
                tvSelectedDate.setText("Not available");
                showTimeSlots(new ArrayList<>(), key);
                return;
            }

            // Format for display
            Calendar cal = Calendar.getInstance();
            cal.set(date.getYear(), date.getMonth(), date.getDay());

            SimpleDateFormat fmt =
                    new SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH);
            tvSelectedDate.setText(fmt.format(cal.getTime()));

            fetchAvailabilityForSelectedDate(key);
        });
    }

    private void scrollToFirstAvailable() {
        if (availableDates.isEmpty()) return;

        CalendarDay first = keyToCalendarDay(availableDates.get(0));
        if (first != null) calendarView.setCurrentDate(first);
    }

    // ================================================
    //       KEY <-> CalendarDay conversion
    // ================================================
    private CalendarDay keyToCalendarDay(String key) {
        try {
            String[] p = key.split("-");
            int y = Integer.parseInt(p[0]);
            int m1 = Integer.parseInt(p[1]); // 1–12
            int d = Integer.parseInt(p[2]);

            return CalendarDay.from(y, m1 - 1, d); // 0–11
        } catch (Exception e) {
            return null;
        }
    }

    private String calendarDayToKey(CalendarDay day) {
        int m1 = day.getMonth() + 1; // 0->1, 1->2...
        return String.format(Locale.ENGLISH,
                "%04d-%02d-%02d",
                day.getYear(), m1, day.getDay());
    }

    // ================================================
    //       FETCH RANGE + BOOKINGS
    // ================================================
    private void fetchAvailabilityForSelectedDate(String key) {
        db.collection("Users")
                .document(professionalId)
                .collection("availability")
                .document(key)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        showTimeSlots(new ArrayList<>(), key);
                        return;
                    }

                    String start = doc.getString("startTime");
                    String end = doc.getString("endTime");

                    fetchExistingBookings(start, end, key);
                });
    }

    private void fetchExistingBookings(String start, String end, String key) {

        db.collection("Users")
                .document(professionalId)
                .collection("bookings")
                .whereEqualTo("selectedDate", key)
                .get()
                .addOnSuccessListener(q -> {

                    List<String[]> blocked = new ArrayList<>();

                    for (var d : q.getDocuments()) {
                        String bStart = d.getString("serviceTime");
                        String bEnd   = d.getString("endTime");
                        if (bStart != null && bEnd != null)
                            blocked.add(new String[]{bStart, bEnd});
                    }

                    generateTimeSlots(start, end, blocked, key);
                });
    }

    // ================================================
    //            GENERATE TIME SLOTS
    // ================================================
    private void generateTimeSlots(String start,
                                   String end,
                                   List<String[]> blocked,
                                   String key) {

        List<String> slots = new ArrayList<>();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

            Date startDate = sdf.parse(start);
            Date endDate   = sdf.parse(end);

            int dur = Integer.parseInt(tvServiceDuration.getText()
                    .toString()
                    .replaceAll("[^0-9]", ""));

            int total = dur + 30;

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);

            while (cal.getTime().before(endDate)) {

                String slot = sdf.format(cal.getTime());

                Calendar endCal = (Calendar) cal.clone();
                endCal.add(Calendar.MINUTE, total);

                boolean conflict = false;

                for (String[] block : blocked) {
                    Date bs = sdf.parse(block[0]);
                    Date be = sdf.parse(block[1]);

                    if (cal.getTime().before(be)
                            && endCal.getTime().after(bs)) {
                        conflict = true;
                        break;
                    }
                }

                if (!conflict) slots.add(slot);

                cal.add(Calendar.MINUTE, 30);
            }

            showTimeSlots(slots, key);

        } catch (Exception ignored) {}
    }

    // ================================================
    //            ADAPTER + NAVIGATION
    // ================================================
    private void showTimeSlots(List<String> slots, String key) {

        rviewTimeSlots.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        TimeSlotAdapter adapter = new TimeSlotAdapter(slots, time -> {

            selectedTime = time;

            Intent i = new Intent(this, ConfirmBooking.class);

            i.putExtra("professionalId", professionalId);
            i.putExtra("professionalName", getIntent().getStringExtra("professionalName"));
            i.putExtra("professionalProfession", getIntent().getStringExtra("professionalProfession"));
            i.putExtra("professionalProfilePic", getIntent().getStringExtra("professionalProfilePic"));

            i.putExtra("serviceName", getIntent().getStringExtra("serviceName"));
            i.putExtra("servicePrice", getIntent().getStringExtra("servicePrice"));
            i.putExtra("serviceDuration", getIntent().getStringExtra("serviceDuration"));

            i.putExtra("selectedDate", key);
            i.putExtra("selectedTime", selectedTime);

            startActivity(i);
        });

        rviewTimeSlots.setAdapter(adapter);
    }
}
