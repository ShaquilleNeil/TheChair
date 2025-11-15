package com.example.thechair.Professional;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProHomeFragment extends Fragment {

    private TextView username;
    private RecyclerView recyclerDates, recyclerAppointments;
    private DateAdapter dateAdapter;
    private AppointmentAdapter appointmentAdapter;
    private ImageView profileimage;
    private Button btnMyServices, btnMyAvailability;

    // NEW REAL BOOKINGS MAP
    private Map<String, List<Booking>> bookingsByDate = new HashMap<>();

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

        loadUser();
        loadBookings();   // ← IMPORTANT!!!

        recyclerDates.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        // create 7-day date list
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            dateList.add(today.plusDays(i));
        }

        dateAdapter = new DateAdapter(dateList, date -> {
            String label = formatDate(date);
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
            profileimage.setImageBitmap(cachedBitmap != null ? cachedBitmap : null);
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

//    public static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
//        private final String url;
//        private final ImageView imageView;
//        private final UserManager userManager;
//
//        public ImageLoaderTask(String url, ImageView imageView, UserManager userManager) {
//            this.url = url;
//            this.imageView = imageView;
//            this.userManager = userManager;
//        }
//
//        @Override
//        protected Bitmap doInBackground(String... strings) {
//            try {
//                URL urlConnection = new URL(url);
//                HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
//                connection.setDoInput(true);
//                connection.connect();
//                InputStream input = connection.getInputStream();
//                return BitmapFactory.decodeStream(input);
//            } catch (Exception e) { e.printStackTrace(); }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Bitmap bitmap) {
//            if (bitmap != null) {
//                imageView.setImageBitmap(bitmap);
//                userManager.setProfileBitmap(bitmap);
//            }
//        }
//    }

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

                        String dateKey = booking.selectedDate;  // "Monday, Nov 17"

                        bookingsByDate
                                .computeIfAbsent(dateKey, k -> new ArrayList<>())
                                .add(booking);
                    }

                    LocalDate today = LocalDate.now();
                    showBookingsForDate(formatDate(today));
                });
    }

    // -------------------- SHOW BOOKINGS FOR A SELECTED DATE --------------------

    private void showBookingsForDate(String dateLabel) {
        List<Booking> list = bookingsByDate.getOrDefault(dateLabel, new ArrayList<>());
        appointmentAdapter = new AppointmentAdapter(list);
        recyclerAppointments.setAdapter(appointmentAdapter);
    }

    // -------------------- FORMAT DATE --------------------

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
        return date.format(formatter);
    }
}
