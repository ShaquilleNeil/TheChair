// Shaq’s Notes:
// This fragment splits a customer’s bookings into TODAY, UPCOMING, and PAST groups.
// It pulls booking documents from Firestore under:
//     Users/{customerID}/bookings
//
// Logic:
// • Convert Firestore timestamps to dates using a yyyy-MM-dd format
// • Compare each booking’s date with "today’s" date
// • Skip cancelled bookings for Today + Upcoming (you only want active sessions)
// • Everything else falls into Past, including cancelled or completed sessions
//
// UI behavior:
// • Each section is collapsible with animated arrow rotation
// • Each section holds its own RecyclerView + BookingAdapter
// • Lists start collapsed for clean display
//
// This is your "booking history hub": clean, modular, and easy to evolve.

package com.example.thechair.Customer;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BookingsFragment extends Fragment {

    // RecyclerViews for the 3 sections
    private RecyclerView rvToday, rvUpcoming, rvPast;

    // Adapters
    private BookingAdapter todayAdapter, upcomingAdapter, pastAdapter;

    // Lists
    private final ArrayList<Booking> todayList = new ArrayList<>();
    private final ArrayList<Booking> upcomingList = new ArrayList<>();
    private final ArrayList<Booking> pastList = new ArrayList<>();

    public BookingsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_bookings, container, false);

        // -------------------- Init Rvs --------------------
        rvToday = view.findViewById(R.id.rvToday);
        rvUpcoming = view.findViewById(R.id.rvUpcoming);
        rvPast = view.findViewById(R.id.rvPast);

        // Default collapsed
        rvToday.setVisibility(View.GONE);
        rvUpcoming.setVisibility(View.GONE);
        rvPast.setVisibility(View.GONE);

        rvToday.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPast.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup adapters
        todayAdapter = new BookingAdapter(getContext(), todayList);
        upcomingAdapter = new BookingAdapter(getContext(), upcomingList);
        pastAdapter = new BookingAdapter(getContext(), pastList);

        rvToday.setAdapter(todayAdapter);
        rvUpcoming.setAdapter(upcomingAdapter);
        rvPast.setAdapter(pastAdapter);

        // Load Firestore bookings
        loadBookings();

        // Setup expandable headers
        setupCollapsible(view);

        return view;
    }

    // -------------------- Expand / Collapse Logic --------------------
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
            arrow.setRotation(180); // flipped arrow
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            arrow.setRotation(0);   // default arrow
        }
    }

    // -------------------- Load Bookings from Firestore --------------------
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

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

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
                            continue; // skip malformed dates
                        }

                        boolean isCancelled =
                                booking.getStatus().equalsIgnoreCase("Cancelled");

                        // -------------------- Categorization --------------------

                        if (bookingDate.equals(todayDate) && !isCancelled) {
                            todayList.add(booking);
                        }
                        else if (bookingDate.after(todayDate) && !isCancelled) {
                            upcomingList.add(booking);
                        }
                        else {
                            // Past includes:
                            // • completed
                            // • cancelled
                            // • anything before today
                            pastList.add(booking);
                        }
                    }

                    // Update lists
                    todayAdapter.notifyDataSetChanged();
                    upcomingAdapter.notifyDataSetChanged();
                    pastAdapter.notifyDataSetChanged();
                });
    }
}
