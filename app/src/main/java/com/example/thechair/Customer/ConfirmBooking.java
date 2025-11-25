// Shaq’s Notes:
// This activity is your “final handshake” before a booking becomes real.
// It collects the chosen pro, service, date/time, and safely writes a full
// booking document to three places:
//
//   1) /bookings/{bookingId}                 – global index
//   2) /Users/{proId}/bookings/{bookingId}   – pro’s schedule
//   3) /Users/{customerId}/bookings/{bookingId} – customer’s history
//
// It also pushes a Google Calendar event, which gives the booking real-world gravity.
// Glide is used defensively so bad URLs don’t nuke your whole screen.
// The addMinutes helper handles finish-times with a quiet elegance.
//
// The structure is sound and readable—this feels like production-grade Android logic.

package com.example.thechair.Customer;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
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

        //-------------- Bind Views --------------
        tvProName = findViewById(R.id.tvProName);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServicePrice = findViewById(R.id.tvServicePrice);
        tvServiceTime = findViewById(R.id.tvSelectedTime);
        tvServiceDuration = findViewById(R.id.tvServiceDuration);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnConfirm = findViewById(R.id.btnConfirm);
        profileImage = findViewById(R.id.ivProPic);

        //-------------- Extract Intent Values --------------
        String proName = getIntent().getStringExtra("professionalName");
        proId = getIntent().getStringExtra("professionalId");
        String serviceName = getIntent().getStringExtra("serviceName");
        String servicePrice = getIntent().getStringExtra("servicePrice");
        String serviceDuration = getIntent().getStringExtra("serviceDuration");
        String serviceTime = getIntent().getStringExtra("selectedTime");
        String selectedDate = getIntent().getStringExtra("selectedDate");
        String imageUrl = getIntent().getStringExtra("professionalProfilePic");

        //-------------- Safe Profile Image Load --------------
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_person);
        }

        //-------------- Populate UI --------------
        tvProName.setText("Professional's Name: " + proName);
        tvServiceName.setText("Service: " + serviceName);
        tvServicePrice.setText("Price: " + servicePrice);
        tvServiceTime.setText("Time: " + serviceTime);
        tvServiceDuration.setText("Duration: " + serviceDuration);
        tvSelectedDate.setText("Date: " + selectedDate);

        btnConfirm.setOnClickListener(v -> createBooking());
    }

    // ----------------------------------------------------------
    //                 CREATE BOOKING + SAVE TO FIRESTORE
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

        //-------------- Clean numeric values --------------
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

        //-------------- Compute end time (service + buffer) --------------
        String endTime = addMinutes(serviceTime, duration + 30);

        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //-------------- Fetch Customer Name --------------
        db.collection("Users").document(customerId)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String customerName = userDoc.getString("name");
                    if (customerName == null) customerName = "Unknown";

                    // Unique ID for booking
                    String bookingId = UUID.randomUUID().toString();

                    //-------------- Build booking object --------------
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

                    //-------------- Save to Firestore --------------
                    db.collection("bookings")
                            .document(bookingId)
                            .set(bookingData)
                            .addOnSuccessListener(aVoid -> {

                                // Pro side
                                db.collection("Users")
                                        .document(proId)
                                        .collection("bookings")
                                        .document(bookingId)
                                        .set(bookingData);

                                // Customer side
                                db.collection("Users")
                                        .document(customerId)
                                        .collection("bookings")
                                        .document(bookingId)
                                        .set(bookingData);

                                // Add to calendar
                                addToGoogleCalendar(bookingData);

                                Toast.makeText(this, "Booking created successfully", Toast.LENGTH_SHORT).show();

                                // Return to home
                                Intent intent = new Intent(ConfirmBooking.this, CustomerHome.class);
                                intent.putExtra("openTab", "home");
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                });
    }

    // ----------------------------------------------------------
    //              CREATE EVENT IN GOOGLE CALENDAR
    // ----------------------------------------------------------

    private void addToGoogleCalendar(Map<String, Object> bookingData) {
        try {
            String date = bookingData.get("selectedDate").toString();      // yyyy-MM-dd
            String time = bookingData.get("serviceTime").toString();       // HH:mm
            int duration = Integer.parseInt(bookingData.get("serviceDuration").toString());

            String proName = bookingData.get("professionalName").toString();
            String serviceName = bookingData.get("serviceName").toString();

            String[] d = date.split("-");
            int year = Integer.parseInt(d[0]);
            int month = Integer.parseInt(d[1]) - 1;
            int day = Integer.parseInt(d[2]);

            String[] t = time.split(":");
            int hour = Integer.parseInt(t[0]);
            int minute = Integer.parseInt(t[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, hour, minute);

            long startMillis = cal.getTimeInMillis();
            long endMillis = startMillis + (duration * 60_000L);

            Intent calendarIntent = new Intent(Intent.ACTION_INSERT);
            calendarIntent.setData(CalendarContract.Events.CONTENT_URI);

            calendarIntent.putExtra(CalendarContract.Events.TITLE,
                    serviceName + " with " + proName);

            calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);
            calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
            calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION,
                    "Booking made in The Chair app.");

            startActivity(calendarIntent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not add to Google Calendar", Toast.LENGTH_SHORT).show();
        }
    }

    // ----------------------------------------------------------
    //                  ADD-MINUTES HELPER
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
