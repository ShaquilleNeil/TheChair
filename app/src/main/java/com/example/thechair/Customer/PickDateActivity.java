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

    TextView tvProName, tvServiceName, tvServicePrice, tvServiceDuration,
            tvProProfession, tvSelectedDate;
    ImageView profileImage;
    RecyclerView rviewTimeSlots;

    MaterialCalendarView calendarView;
    String professionalId;
    String selectedTime;

    List<String> availableDates = new ArrayList<>(); // "yyyy-MM-dd"
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pick_date);

        tvProName = findViewById(R.id.tvProName);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServicePrice = findViewById(R.id.tvServicePrice);
        tvServiceDuration = findViewById(R.id.tvServiceDuration);
        tvProProfession = findViewById(R.id.proProfession);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        profileImage = findViewById(R.id.profileImage);
        calendarView = findViewById(R.id.calendarView);
        rviewTimeSlots = findViewById(R.id.rviewTimeSlots);

        // Intent data:
        professionalId = getIntent().getStringExtra("professionalId");
        tvProName.setText(getIntent().getStringExtra("professionalName"));
        tvProProfession.setText(getIntent().getStringExtra("professionalProfession"));
        tvServiceName.setText("Service: " + getIntent().getStringExtra("serviceName"));
        tvServicePrice.setText("Price: $" + getIntent().getStringExtra("servicePrice"));
        tvServiceDuration.setText("Duration: " + getIntent().getStringExtra("serviceDuration") + " Minutes");

        String proImage = getIntent().getStringExtra("professionalProfilePic");
        if (proImage != null) Glide.with(this).load(proImage).into(profileImage);

        loadAvailableDates();
    }

    /** ----------------- LOAD AVAILABLE DATES ----------------- */

    private void loadAvailableDates() {
        db.collection("Users")
                .document(professionalId)
                .collection("availability")
                .get()
                .addOnSuccessListener(query -> {
                    availableDates.clear();
                    for (var doc : query.getDocuments()) {
                        // doc id is "yyyy-MM-dd"
                        availableDates.add(doc.getId());
                    }
                    highlightDates();
                    scrollToFirstAvailable();
                });
    }

    /** ----------------- HIGHLIGHT DATES ----------------- */

    private void highlightDates() {

        List<CalendarDay> highlightDays = new ArrayList<>();

        for (String d : availableDates) {
            CalendarDay cd = keyToCalendarDay(d);
            if (cd != null) {
                highlightDays.add(cd);
            }
        }

        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return highlightDays.contains(day);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(
                        getDrawable(R.drawable.available_day_background)
                );
            }
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {

            // date.getMonth() is 0–11, we convert to 1–12 for the key
            String dateKey = calendarDayToKey(date); // "yyyy-MM-dd"

            if (!availableDates.contains(dateKey)) {
                tvSelectedDate.setText("Not available");
                showTimeSlots(new ArrayList<>(), dateKey);
                return;
            }

            // For Calendar, month is 0-based and date.getMonth() is also 0-based,
            // so we pass it directly (NO +1 here).
            Calendar cal = Calendar.getInstance();
            cal.set(date.getYear(), date.getMonth(), date.getDay());

            SimpleDateFormat displayFormat =
                    new SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH);
            tvSelectedDate.setText(displayFormat.format(cal.getTime()));

            fetchAvailabilityForSelectedDate(dateKey);
        });
    }

    /** Scroll to the first available date (same month logic fix) */
    private void scrollToFirstAvailable() {
        if (availableDates.isEmpty()) return;

        CalendarDay first = keyToCalendarDay(availableDates.get(0));
        if (first != null) {
            calendarView.setCurrentDate(first);
        }
    }

    /** ----------------- HELPERS: KEY <-> CalendarDay ----------------- */

    // Firestore key "yyyy-MM-dd" -> CalendarDay (month 0–11)
    private CalendarDay keyToCalendarDay(String key) {
        try {
            String[] p = key.split("-");
            if (p.length != 3) return null;

            int year = Integer.parseInt(p[0]);
            int monthOneBased = Integer.parseInt(p[1]); // 1–12 from Firestore
            int day = Integer.parseInt(p[2]);

            int monthZeroBased = monthOneBased - 1;     // 0–11 for CalendarDay
            return CalendarDay.from(year, monthZeroBased, day);
        } catch (Exception e) {
            return null;
        }
    }

    // CalendarDay (0–11 month) -> Firestore key "yyyy-MM-dd"
    private String calendarDayToKey(CalendarDay day) {
        int monthOneBased = day.getMonth() + 1; // convert back 0–11 -> 1–12
        return String.format(Locale.ENGLISH, "%04d-%02d-%02d",
                day.getYear(), monthOneBased, day.getDay());
    }

    /** ----------------- FETCH AVAILABILITY + BOOKINGS ----------------- */

    private void fetchAvailabilityForSelectedDate(String dateKey) {

        db.collection("Users")
                .document(professionalId)
                .collection("availability")
                .document(dateKey)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        showTimeSlots(new ArrayList<>(), dateKey);
                        return;
                    }

                    String start = doc.getString("startTime");
                    String end = doc.getString("endTime");

                    fetchExistingBookings(start, end, dateKey);
                });
    }

    private void fetchExistingBookings(String start, String end, String dateKey) {

        db.collection("Users")
                .document(professionalId)
                .collection("bookings")
                .whereEqualTo("selectedDate", dateKey)
                .get()
                .addOnSuccessListener(query -> {

                    List<String[]> blocked = new ArrayList<>();

                    for (var doc : query.getDocuments()) {
                        String bStart = doc.getString("serviceTime");
                        String bEnd = doc.getString("endTime");
                        if (bStart != null && bEnd != null) {
                            blocked.add(new String[]{bStart, bEnd});
                        }
                    }

                    generateTimeSlots(start, end, blocked, dateKey);
                });
    }

    /** ----------------- TIME SLOT GENERATION ----------------- */

    private void generateTimeSlots(String start, String end, List<String[]> blockedList, String dateKey) {

        List<String> slots = new ArrayList<>();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);

            int duration = Integer.parseInt(tvServiceDuration.getText().toString().replaceAll("[^0-9]", ""));
            int totalDuration = duration + 30;

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);

            while (cal.getTime().before(endDate)) {

                String slot = sdf.format(cal.getTime());

                Calendar endCal = (Calendar) cal.clone();
                endCal.add(Calendar.MINUTE, totalDuration);

                boolean overlaps = false;
                for (String[] block : blockedList) {
                    Date blockStart = sdf.parse(block[0]);
                    Date blockEnd = sdf.parse(block[1]);

                    if (cal.getTime().before(blockEnd) &&
                            endCal.getTime().after(blockStart)) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps) slots.add(slot);
                cal.add(Calendar.MINUTE, 30);
            }

            showTimeSlots(slots, dateKey);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showTimeSlots(List<String> slots, String dateKey) {

        rviewTimeSlots.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        TimeSlotAdapter adapter = new TimeSlotAdapter(slots, time -> {

            selectedTime = time;

            Intent intent = new Intent(this, ConfirmBooking.class);

            intent.putExtra("professionalId", professionalId);
            intent.putExtra("professionalName", getIntent().getStringExtra("professionalName"));
            intent.putExtra("professionalProfession", getIntent().getStringExtra("professionalProfession"));
            intent.putExtra("professionalProfilePic", getIntent().getStringExtra("professionalProfilePic"));

            intent.putExtra("serviceName", getIntent().getStringExtra("serviceName"));
            intent.putExtra("servicePrice", getIntent().getStringExtra("servicePrice"));
            intent.putExtra("serviceDuration", getIntent().getStringExtra("serviceDuration"));

            intent.putExtra("selectedDate", dateKey);
            intent.putExtra("selectedTime", selectedTime);

            startActivity(intent);
        });

        rviewTimeSlots.setAdapter(adapter);
    }
}
