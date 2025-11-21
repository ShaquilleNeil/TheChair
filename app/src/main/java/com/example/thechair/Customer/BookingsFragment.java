package com.example.thechair.Customer;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.Adapters.Booking;
import com.example.thechair.Adapters.BookingAdapter;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingsFragment extends Fragment {

    private RecyclerView rvToday, rvUpcoming, rvPast;
    private BookingAdapter todayAdapter, upcomingAdapter, pastAdapter;

    private ArrayList<Booking> todayList = new ArrayList<>();
    private ArrayList<Booking> upcomingList = new ArrayList<>();
    private ArrayList<Booking> pastList = new ArrayList<>();

    public BookingsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_bookings, container, false);

        // Init RecyclerViews
        rvToday = view.findViewById(R.id.rvToday);
        rvUpcoming = view.findViewById(R.id.rvUpcoming);
        rvPast = view.findViewById(R.id.rvPast);

        // collapse default
        rvToday.setVisibility(View.GONE);
        rvUpcoming.setVisibility(View.GONE);
        rvPast.setVisibility(View.GONE);

        rvToday.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPast.setLayoutManager(new LinearLayoutManager(getContext()));

        // Attach adapters
        todayAdapter = new BookingAdapter(getContext(), todayList);
        upcomingAdapter = new BookingAdapter(getContext(), upcomingList);
        pastAdapter = new BookingAdapter(getContext(), pastList);

        rvToday.setAdapter(todayAdapter);
        rvUpcoming.setAdapter(upcomingAdapter);
        rvPast.setAdapter(pastAdapter);

        loadBookings();
        setupCollapsible(view);

        return view;
    }

    private void setupCollapsible(View view) {
        LinearLayout sectionToday = view.findViewById(R.id.sectionToday);
        ImageView ivTodayArrow = view.findViewById(R.id.ivTodayArrow);
        sectionToday.setOnClickListener(v -> toggle(rvToday, ivTodayArrow));

        LinearLayout sectionUpcoming = view.findViewById(R.id.sectionUpcoming);
        ImageView ivUpcomingArrow = view.findViewById(R.id.ivUpcomingArrow);
        sectionUpcoming.setOnClickListener(v -> toggle(rvUpcoming, ivUpcomingArrow));

        LinearLayout sectionPast = view.findViewById(R.id.sectionPast);
        ImageView ivPastArrow = view.findViewById(R.id.ivPastArrow);
        sectionPast.setOnClickListener(v -> toggle(rvPast, ivPastArrow));
    }

    private void toggle(RecyclerView recyclerView, ImageView arrow) {
        if (recyclerView.getVisibility() == View.VISIBLE) {
            recyclerView.setVisibility(View.GONE);
            arrow.setRotation(180);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            arrow.setRotation(0);
        }
    }

    private void loadBookings() {

        String customerID = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        todayList.clear();
        upcomingList.clear();
        pastList.clear();

        db.collection("Users")
                .document(customerID)
                .collection("bookings")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(query -> {

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

                    String todayStr = sdf.format(new Date());
                    Date todayDate = null;

                    try {
                        todayDate = sdf.parse(todayStr);
                    } catch (Exception ignored) {}

                    for (var doc : query) {

                        Booking booking = doc.toObject(Booking.class);
                        Date bookingDate;

                        try {
                            bookingDate = sdf.parse(booking.getSelectedDate());
                        } catch (Exception e) {
                            continue;
                        }

                        if (bookingDate.equals(todayDate) && !booking.getStatus().equalsIgnoreCase("Cancelled")) {
                            todayList.add(booking);
                        } else if (bookingDate.after(todayDate) && !booking.getStatus().equalsIgnoreCase("Cancelled")) {
                            upcomingList.add(booking);
                        } else {
                            pastList.add(booking);
                        }

                    }

                    todayAdapter.notifyDataSetChanged();
                    upcomingAdapter.notifyDataSetChanged();
                    pastAdapter.notifyDataSetChanged();

                });
    }
}
