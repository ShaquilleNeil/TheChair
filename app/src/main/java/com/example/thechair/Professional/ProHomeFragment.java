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
import com.example.thechair.Adapters.DateAdapter;
import com.example.thechair.Adapters.HairAppointment;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.example.thechair.Customer.HomeFragment;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProHomeFragment extends Fragment {

    private TextView username;
    private RecyclerView recyclerDates, recyclerAppointments;
    private DateAdapter dateAdapter;
    private AppointmentAdapter appointmentAdapter;
    private Map<LocalDate, List<HairAppointment>> appointmentsByDate;
    private ImageView profileimage;
    private Button btnMyServices, btnMyAvailability;

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


        recyclerDates.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        setupData(); // we'll define this below

        // create 7-day date list
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            dateList.add(today.plusDays(i));
        }

        dateAdapter = new DateAdapter(dateList, this::showAppointmentsForDate);
        recyclerDates.setAdapter(dateAdapter);

        // show today's appointments first
        showAppointmentsForDate(today);

        btnMyServices.setOnClickListener( v -> {
            // Navigate to MyServicesactivity
            Intent intent = new Intent(getActivity(), MyServices.class);
            startActivity(intent);


        });

        btnMyAvailability.setOnClickListener(v -> {
            // Navigate to MyAvailabilityactivity
            Intent intent = new Intent(getActivity(), MyAvailability.class);
            startActivity(intent);
        });




        return view;
    }


    private void loadUser() {
        UserManager userManager = UserManager.getInstance();
        appUsers cachedUser = userManager.getUser();

        if (cachedUser != null) {
            username.setText(cachedUser.getName());

            // Show cached image immediately if available
            Bitmap cachedBitmap = userManager.getProfileBitmap();
            if (cachedBitmap != null) {
                profileimage.setImageBitmap(cachedBitmap);
            } else {
                profileimage.setImageResource(R.drawable.banner);
            }
        }

        // Fetch latest from Firebase
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            appUsers firebaseUserData = documentSnapshot.toObject(appUsers.class);
                            if (firebaseUserData != null) {
                                username.setText(firebaseUserData.getName());

                                String profilePic = firebaseUserData.getProfilepic();
                                if (profilePic != null) {
                                    new ProHomeFragment.ImageLoaderTask(profilePic, profileimage, userManager).execute();
                                } else {
                                    profileimage.setImageResource(R.drawable.banner);
                                }

                                userManager.setUser(firebaseUserData);
                            }
                        }
                    });
        }
    }

    private void setupData() {
        appointmentsByDate = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        appointmentsByDate.put(today, List.of(
                new HairAppointment("Alison", "Haircut", "10:00 AM", "First-time client"),
                new HairAppointment("David", "Hair coloring", "1:30 PM", "Bring reference photo")
        ));

        appointmentsByDate.put(tomorrow, List.of(
                new HairAppointment("Sarah", "Dread retwist", "9:00 AM", "Returning client"),
                new HairAppointment("Maya", "Silk press", "3:30 PM", "Long hair")
        ));
    }


    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;
        private final UserManager userManager;

        public ImageLoaderTask(String url, ImageView imageView, UserManager userManager) {
            this.url = url;
            this.imageView = imageView;
            this.userManager = userManager;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                userManager.setProfileBitmap(bitmap); // cache the bitmap
            } else {
                imageView.setImageResource(R.drawable.banner);
            }
        }
    }

    private void showAppointmentsForDate(LocalDate date) {
        List<HairAppointment> list = appointmentsByDate.getOrDefault(date, new ArrayList<>());
        appointmentAdapter = new AppointmentAdapter(list);
        recyclerAppointments.setAdapter(appointmentAdapter);
    }
}
