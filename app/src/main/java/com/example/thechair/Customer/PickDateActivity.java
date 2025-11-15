package com.example.thechair.Customer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thechair.Adapters.TimeSlotAdapter;
import com.example.thechair.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PickDateActivity extends AppCompatActivity {

    TextView tvProName, tvServiceName, tvServiceprice, tvServiceDuration, tvproProfession, tvSelectedDate;
    ImageView profileImage;
    Button btnpickDate;
    String professionalId;

    String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pick_date);

        tvProName = findViewById(R.id.tvProName);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceprice = findViewById(R.id.tvServicePrice);
        tvServiceDuration = findViewById(R.id.tvServiceDuration);
        tvproProfession = findViewById(R.id.proProfession);
        profileImage = findViewById(R.id.profileImage);
        btnpickDate = findViewById(R.id.btnPickDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        btnpickDate.setOnClickListener(v -> showDatePicker());

        // get extras (clean values)
        tvProName.setText(getIntent().getStringExtra("professionalName"));
        professionalId = getIntent().getStringExtra("professionalId");
        tvproProfession.setText(getIntent().getStringExtra("professionalProfession"));
        tvServiceName.setText("Service: " + getIntent().getStringExtra("serviceName"));
        tvServiceprice.setText("Price: $" + getIntent().getStringExtra("servicePrice"));
        tvServiceDuration.setText("Duration: " + getIntent().getStringExtra("serviceDuration") + " Minutes");

        String proImage = getIntent().getStringExtra("professionalProfilePic");
        if (proImage != null) {
            Glide.with(this).load(proImage).into(profileImage);
        }
    }

    private void showDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar cal = Calendar.getInstance();
                    cal.set(year, month, dayOfMonth);

                    SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH);
                    tvSelectedDate.setText(displayFormat.format(cal.getTime()));

                    fetchAvailabilityForSelectedDate(cal);

                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );

        picker.show();
    }

    private void fetchAvailabilityForSelectedDate(Calendar cal) {

        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String dateKey = apiFormat.format(cal.getTime());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .document(professionalId)
                .collection("availability")
                .document(dateKey)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        tvSelectedDate.setText("Not available on this date");
                        showTimeSlots(new ArrayList<>(), dateKey);
                        return;
                    }

                    String start = doc.getString("startTime");
                    String end = doc.getString("endTime");

                    if (start == null || end == null) {
                        tvSelectedDate.setText("No hours set for this date");
                        showTimeSlots(new ArrayList<>(), dateKey);
                        return;
                    }

                    fetchExistingBookings(start, end, dateKey);

                })
                .addOnFailureListener(e -> tvSelectedDate.setText("Error loading availability"));
    }

    private void fetchExistingBookings(String start, String end, String dateKey) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

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

                if (!endCal.getTime().after(endDate)) {

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
                }

                cal.add(Calendar.MINUTE, 30);
            }

            showTimeSlots(slots, dateKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showTimeSlots(List<String> slots, String dateKey) {

        RecyclerView rview = findViewById(R.id.rviewTimeSlots);
        rview.setLayoutManager(new LinearLayoutManager(this));

        TimeSlotAdapter adapter = new TimeSlotAdapter(slots, time -> {

            selectedTime = time;

            Intent intent = new Intent(this, ConfirmBooking.class);

            // professional info
            intent.putExtra("professionalId", professionalId);
            intent.putExtra("professionalName", getIntent().getStringExtra("professionalName"));
            intent.putExtra("professionalProfession", getIntent().getStringExtra("professionalProfession"));
            intent.putExtra("professionalProfilePic", getIntent().getStringExtra("professionalProfilePic"));

            // service info
            intent.putExtra("serviceName", getIntent().getStringExtra("serviceName"));
            intent.putExtra("servicePrice", getIntent().getStringExtra("servicePrice"));
            intent.putExtra("serviceDuration", getIntent().getStringExtra("serviceDuration"));

            // date + time
            intent.putExtra("selectedDate", dateKey);
            intent.putExtra("selectedTime", selectedTime);

            startActivity(intent);
        });

        rview.setAdapter(adapter);
    }
}
