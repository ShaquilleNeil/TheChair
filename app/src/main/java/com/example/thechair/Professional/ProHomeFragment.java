package com.example.thechair.Professional;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.Adapters.AppointmentAdapter;
import com.example.thechair.Adapters.Booking;
import com.example.thechair.Adapters.DateAdapter;
import com.example.thechair.Adapters.ImageLoaderTask;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProHomeFragment extends Fragment {

    private TextView username, tvNoAppointments;
    private RecyclerView recyclerDates, recyclerAppointments;
    private DateAdapter dateAdapter;
    private AppointmentAdapter appointmentAdapter;
    private ImageView profileimage;
    private Button btnMyServices, btnMyAvailability;

    // Store bookings grouped by display date: "Tuesday, Nov 25"
    private final Map<String, List<Booking>> bookingsByDate = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pro_home, container, false);

        recyclerDates = view.findViewById(R.id.rviewDates);
        recyclerAppointments = view.findViewById(R.id.rviewAppointments);
        username = view.findViewById(R.id.tvStylistName);
        profileimage = view.findViewById(R.id.profileImage);
        btnMyServices = view.findViewById(R.id.btnServices);
        btnMyAvailability = view.findViewById(R.id.btnAvailability);
        tvNoAppointments = view.findViewById(R.id.tvNoAppointments);




        loadUser();
        loadBookings();

        recyclerDates.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        // ⭐ FIX: SHOW 30 DAYS, not 7 days
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) {     // show full month
            dateList.add(today.plusDays(i));
        }

        dateAdapter = new DateAdapter(dateList, date -> {
            String label = formatDate(date);            // e.g., "Tuesday, Nov 25"
            showBookingsForDate(label);
        });

        recyclerDates.setAdapter(dateAdapter);

        btnMyServices.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MyServices.class));
        });

        btnMyAvailability.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MyAvailability.class));
        });

        return view;
    }

    // -------------------- LOAD USER --------------------

    private void loadUser() {
        UserManager userManager = UserManager.getInstance();
        appUsers cachedUser = userManager.getUser();

        if (cachedUser != null) {
            username.setText(cachedUser.getName());
            Bitmap cachedBitmap = userManager.getProfileBitmap();
            profileimage.setImageBitmap(cachedBitmap);
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            appUsers firebaseUserData = document.toObject(appUsers.class);
                            if (firebaseUserData != null) {
                                username.setText(firebaseUserData.getName());
                                String profilePic = firebaseUserData.getProfilepic();

                                if (profilePic != null) {
                                    new ImageLoaderTask(profilePic, profileimage, userManager).execute();
                                }
                                userManager.setUser(firebaseUserData);
                            }
                        }
                    });
        }
    }

    // -------------------- LOAD BOOKINGS --------------------

    private void loadBookings() {
        String proId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(proId)
                .collection("bookings")
                .get()
                .addOnSuccessListener(query -> {

                    bookingsByDate.clear();

                    for (var doc : query.getDocuments()) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking == null) continue;

                        // ⭐ FIX: Firestore date = "2025-11-25" — convert to LocalDate
                        LocalDate date = LocalDate.parse(booking.selectedDate);

                        // Convert to UI label: "Tuesday, Nov 25"
                        String dateKey = formatDate(date);

                        bookingsByDate
                                .computeIfAbsent(dateKey, k -> new ArrayList<>())
                                .add(booking);
                    }

                    // Show today's bookings immediately
                    LocalDate today = LocalDate.now();
                    showBookingsForDate(formatDate(today));
                });
    }

    // -------------------- SHOW BOOKINGS --------------------

    private void showBookingsForDate(String dateLabel) {
        List<Booking> list = bookingsByDate.getOrDefault(dateLabel, new ArrayList<>());
        if(list.isEmpty()) {
            recyclerAppointments.setVisibility(View.GONE);
            tvNoAppointments.setVisibility(View.VISIBLE);

        }else{
            recyclerAppointments.setVisibility(View.VISIBLE);
            tvNoAppointments.setVisibility(View.GONE);

            appointmentAdapter = new AppointmentAdapter(list);
            recyclerAppointments.setAdapter(appointmentAdapter);
        }

    }

    // -------------------- FORMAT DATE --------------------

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
        return date.format(formatter);
    }
}
