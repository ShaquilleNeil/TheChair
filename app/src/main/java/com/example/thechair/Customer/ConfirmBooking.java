package com.example.thechair.Customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ConfirmBooking extends AppCompatActivity {

    ImageView profileImage;
    TextView tvProName, tvServiceName, tvServicePrice, tvServiceDuration, tvServiceTime, tvSelectedDate;
    Button btnConfirm;
    String proId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_booking);

        tvProName = findViewById(R.id.tvProName);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServicePrice = findViewById(R.id.tvServicePrice);
        tvServiceTime = findViewById(R.id.tvSelectedTime);
        tvServiceDuration = findViewById(R.id.tvServiceDuration);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnConfirm = findViewById(R.id.btnConfirm);
        profileImage = findViewById(R.id.ivProPic);

        // ---------------------------
        //  Read Intent Values Safely
        // ---------------------------

        String proName = getIntent().getStringExtra("professionalName");
        proId = getIntent().getStringExtra("professionalId");
        String serviceName = getIntent().getStringExtra("serviceName");
        String servicePrice = getIntent().getStringExtra("servicePrice");
        String serviceDuration = getIntent().getStringExtra("serviceDuration");
        String serviceTime = getIntent().getStringExtra("selectedTime");
        String selectedDate = getIntent().getStringExtra("selectedDate");
        String imageUrl = getIntent().getStringExtra("professionalProfilePic");

        // ---------------------------
        //  SAFE GLIDE (fixes crash)
        // ---------------------------
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_person);
        }

        // ---------------------------
        // Fill UI
        // ---------------------------

        tvProName.setText("Professional's Name: " + proName);
        tvServiceName.setText("Service: " + serviceName);
        tvServicePrice.setText("Price: " + servicePrice);
        tvServiceTime.setText("Time: " + serviceTime);
        tvServiceDuration.setText("Duration: " + serviceDuration);
        tvSelectedDate.setText("Date: " + selectedDate);

        btnConfirm.setOnClickListener(v -> createBooking());
    }

    // ----------------------------------------------------------
    //                  CREATE BOOKING
    // ----------------------------------------------------------

    public void createBooking() {

        String proName = getIntent().getStringExtra("professionalName");
        proId = getIntent().getStringExtra("professionalId");
        String serviceName = getIntent().getStringExtra("serviceName");
        String servicePrice = getIntent().getStringExtra("servicePrice");
        String serviceDuration = getIntent().getStringExtra("serviceDuration");
        String serviceTime = getIntent().getStringExtra("selectedTime");
        String selectedDate = getIntent().getStringExtra("selectedDate");
        String proPicUrl = getIntent().getStringExtra("professionalProfilePic");



        if (serviceTime == null || selectedDate == null) {
            Toast.makeText(this, "Missing booking data", Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanPrice = servicePrice.replaceAll("[^0-9.]", "");
        String cleanDuration = serviceDuration.replaceAll("[^0-9]", "");

        double price;
        int duration;

        try {
            price = Double.parseDouble(cleanPrice);
            duration = Integer.parseInt(cleanDuration);
        } catch (Exception e) {
            Toast.makeText(this, "Bad price or duration format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate end time
        String endTime = addMinutes(serviceTime, duration + 30);

        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").document(customerId).get()
                .addOnSuccessListener(userDoc -> {

                    String customerName = userDoc.getString("name");
                    if (customerName == null) customerName = "Unknown";

                    String bookingId = UUID.randomUUID().toString();

                    Map<String, Object> bookingData = new HashMap<>();
                    bookingData.put("bookingId", bookingId);
                    bookingData.put("professionalId", proId);
                    bookingData.put("professionalName", proName);
                    bookingData.put("proPic", proPicUrl);
                    bookingData.put("customerId", customerId);
                    bookingData.put("customerName", customerName);
                    bookingData.put("serviceName", serviceName);
                    bookingData.put("servicePrice", price);
                    bookingData.put("serviceDuration", duration);
                    bookingData.put("serviceTime", serviceTime);
                    bookingData.put("endTime", endTime);
                    bookingData.put("selectedDate", selectedDate);
                    bookingData.put("status", "pending");
                    bookingData.put("timestamp", System.currentTimeMillis());

                    // Save globally
                    db.collection("bookings")
                            .document(bookingId)
                            .set(bookingData)
                            .addOnSuccessListener(aVoid -> {

                                // Save inside professional
                                db.collection("Users")
                                        .document(proId)
                                        .collection("bookings")
                                        .document(bookingId)
                                        .set(bookingData);

                                // Save inside customer
                                db.collection("Users")
                                        .document(customerId)
                                        .collection("bookings")
                                        .document(bookingId)
                                        .set(bookingData);

                                Toast.makeText(this, "Booking created successfully", Toast.LENGTH_SHORT).show();

                                // Go home
                                Intent intent = new Intent(ConfirmBooking.this, CustomerHome.class);
                                intent.putExtra("openTab", "home");
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                });
    }

    // ----------------------------------------------------------
    //                TIME CALCULATION HELPER
    // ----------------------------------------------------------

    private String addMinutes(String time, int minutesToAdd) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            Date date = sdf.parse(time);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.MINUTE, minutesToAdd);

            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return time;
        }
    }
}
