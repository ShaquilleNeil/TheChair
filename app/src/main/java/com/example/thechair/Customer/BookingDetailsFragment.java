// Shaq’s Notes:
// This fragment displays all the details of a completed/pending booking.
// It retrieves the booking info via arguments, shows the professional’s data,
// the booked service, price, times, address, and allows the user to:
//    • View pro profile
//    • Get Google Maps directions
//    • Cancel the booking
//    • Rebook the same service
//    • Rate the service (once)
//
// Internally:
// - Rating saves a new document in Users/{proId}/ratings
// - Updates the pro’s average rating (rating + ratingCount)
// - Marks booking as isRated on both customer & pro sides
// - Cancelling updates status in both locations
//
// This fragment ties together Firestore reads, writes, and UI logic for the
// booking lifecycle.

package com.example.thechair.Customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.thechair.Adapters.DirectionsHelper;
import com.example.thechair.Professional.PublicProfileFragment;
import com.example.thechair.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookingDetailsFragment extends Fragment {

    // -------------------- UI Elements --------------------
    private ImageView proImage;
    private TextView proName, proProfession, serviceName, appointmentDate,
            appointmentTime, appointmentDuration, appointmentPrice, appointmentStatus, address;
    private Button btnViewProfile, btnDirections, btnCancel, btnRebook, btnRate;

    // -------------------- Booking Data --------------------
    private String bookingId;
    private String professionalId;
    private String professionalName;
    private String profilePic;

    private String service;
    private String date;
    private String time;
    private String status;

    private int price;
    private int duration;

    private String fullAddress;
    private LatLng proLatLng;
    private boolean isRated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_booking_details, container, false);

        // -------------------- Bind UI --------------------
        proImage = view.findViewById(R.id.proImage);
        proName = view.findViewById(R.id.proName);
        proProfession = view.findViewById(R.id.proProfession);
        serviceName = view.findViewById(R.id.serviceName);
        appointmentDate = view.findViewById(R.id.appointmentDate);
        appointmentTime = view.findViewById(R.id.appointmentTime);
        appointmentDuration = view.findViewById(R.id.appointmentDuration);
        appointmentPrice = view.findViewById(R.id.appointmentPrice);
        appointmentStatus = view.findViewById(R.id.appointmentStatus);
        address = view.findViewById(R.id.address);

        btnViewProfile = view.findViewById(R.id.btnViewProfile);
        btnDirections = view.findViewById(R.id.btnDirections);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnRebook = view.findViewById(R.id.btnRebook);
        btnRate = view.findViewById(R.id.btnRate);
        btnRate.setVisibility(View.GONE);

        // -------------------- Pull data from bundle --------------------
        if (getArguments() != null) {
            bookingId = getArguments().getString("bookingId");
            professionalId = getArguments().getString("professionalId");
            professionalName = getArguments().getString("professionalName");
            profilePic = getArguments().getString("profilePic");

            service = getArguments().getString("serviceName");
            date = getArguments().getString("selectedDate");
            status = getArguments().getString("status");
            isRated = getArguments().getBoolean("isRated", false);

            time = getArguments().getString("serviceTime") +
                    " - " + getArguments().getString("endTime");

            price = getArguments().getInt("servicePrice");
            duration = getArguments().getInt("serviceDuration");

            fullAddress = getArguments().getString("address", "Address unavailable");

            double lat = getArguments().getDouble("lat", 0);
            double lng = getArguments().getDouble("lng", 0);
            if (lat != 0 && lng != 0) {
                proLatLng = new LatLng(lat, lng);
            }
        }

        // -------------------- Populate UI --------------------
        proName.setText(professionalName);
        serviceName.setText("Service: " + service);
        appointmentDate.setText("Date: " + date);
        appointmentTime.setText("Time: " + time);
        appointmentDuration.setText("Duration: " + duration + " min");
        appointmentPrice.setText("Price: $" + price);
        appointmentStatus.setText("Status: " + status);
        address.setText(fullAddress);

        Glide.with(this)
                .load(profilePic)
                .placeholder(R.drawable.ic_person)
                .into(proImage);

        fetchProfessionalAddress();


        // -------------------- Buttons --------------------

        // Directions → Google Maps
        btnDirections.setOnClickListener(v -> {
            if (proLatLng != null) {
                DirectionsHelper.openExternalGoogleMaps(
                        requireContext(),
                        proLatLng,
                        professionalName
                );
            } else {
                Toast.makeText(requireContext(), "Location unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        // Rate button only shows when:
//   (status == completed OR status == cancelled) AND not already rated
        if ((status.equalsIgnoreCase("completed")
                || status.equalsIgnoreCase("cancelled"))
                && !isRated) {
            btnCancel.setVisibility(View.GONE);
            btnRate.setVisibility(View.VISIBLE);
        }

        // Cancel booking
        btnCancel.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cancel Booking?")
                    .setMessage("Are you sure you want to cancel this appointment?")
                    .setPositiveButton("Yes, cancel", (dialog, which) -> cancelBooking())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Show rating popup
        btnRate.setOnClickListener(v -> showRatingDialog());

        // Rebook the same service
        btnRebook.setOnClickListener(v -> {
            startRebook();
            Toast.makeText(requireContext(), "Rebooking " + service, Toast.LENGTH_SHORT).show();
        });

        // View the professional’s public profile
        btnViewProfile.setOnClickListener(v -> {
            Fragment fragment = new PublicProfileFragment();
            Bundle bundle = new Bundle();
            bundle.putString("professionalId", professionalId);
            fragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.appMainView, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    // -------------------- Rebooking logic --------------------
    private void startRebook() {
        Intent intent = new Intent(getContext(), PickDateActivity.class);

        intent.putExtra("professionalId", professionalId);
        intent.putExtra("professionalName", professionalName);
        intent.putExtra("professionalProfession", proProfession.getText().toString());
        intent.putExtra("professionalProfilePic", profilePic);

        intent.putExtra("serviceName", service);
        intent.putExtra("servicePrice", String.valueOf(price));
        intent.putExtra("serviceDuration", String.valueOf(duration));

        startActivity(intent);
    }

    // -------------------- Cancel Booking --------------------
    private void cancelBooking() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String customerID = FirebaseAuth.getInstance().getUid();

        // Update pro's booking subcollection
        db.collection("Users")
                .document(professionalId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {

                    // Update customer's booking subcollection
                    db.collection("Users")
                            .document(customerID)
                            .collection("bookings")
                            .document(bookingId)
                            .update("status", "cancelled")
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(requireContext(),
                                        "Cancelled " + service,
                                        Toast.LENGTH_SHORT).show();

                                requireActivity().getSupportFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Failed to cancel " + service,
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to cancel " + service,
                                Toast.LENGTH_SHORT).show());
    }

    // -------------------- Show Rating Dialog --------------------
    private void showRatingDialog() {

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.rating_window, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etComment = dialogView.findViewById(R.id.etComment);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rate Experience")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {

                    float rating = ratingBar.getRating();
                    String comment = etComment.getText().toString();

                    if (rating == 0f) {
                        Toast.makeText(requireContext(),
                                "Please rate the experience",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    submitRating(rating, comment);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // -------------------- Submit Rating --------------------
    private void submitRating(float rating, String comment) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String customerID = FirebaseAuth.getInstance().getUid();

        // Get customer name
        db.collection("Users")
                .document(customerID)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String customerName = userDoc.getString("name");
                    String ratingid = UUID.randomUUID().toString();

                    // Rating object
                    Map<String, Object> ratingData = new HashMap<>();
                    ratingData.put("customerID", customerID);
                    ratingData.put("customerName", customerName);
                    ratingData.put("bookingId", bookingId);
                    ratingData.put("rating", rating);
                    ratingData.put("comment", comment);
                    ratingData.put("timestamp", System.currentTimeMillis());

                    // Save rating in pro's subcollection
                    db.collection("Users")
                            .document(professionalId)
                            .collection("ratings")
                            .document(ratingid)
                            .set(ratingData)
                            .addOnSuccessListener(aVoid -> {
                                UpdateProfessionalRating(rating);
                                Toast.makeText(requireContext(),
                                        "Rating submitted",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Failed to submit rating",
                                            Toast.LENGTH_SHORT).show());
                });
    }

    // -------------------- Update Pro Rating Stats --------------------
    private void UpdateProfessionalRating(float newRating) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference proRef = db.collection("Users")
                .document(professionalId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(proRef);

                    Double oldRating = snap.getDouble("rating");
                    Long oldCount = snap.getLong("ratingCount");

                    if (oldRating == null) oldRating = 0.0;
                    if (oldCount == null) oldCount = 0L;

                    // Weighted average formula
                    double updatedRating = ((oldRating * oldCount) + newRating) / (oldCount + 1);
                    long updatedCount = oldCount + 1;

                    transaction.update(proRef, "rating", updatedRating);
                    transaction.update(proRef, "ratingCount", updatedCount);

                    return null;
                })
                .addOnSuccessListener(aVoid -> markBookingAsRated())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to update rating: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    // -------------------- Mark Booking as Rated --------------------
    private void markBookingAsRated() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String customerId = FirebaseAuth.getInstance().getUid();

        Map<String, Object> update = new HashMap<>();
        update.put("isRated", true);

        // Update pro copy
        db.collection("Users")
                .document(professionalId)
                .collection("bookings")
                .document(bookingId)
                .update(update);

        // Update customer copy
        db.collection("Users")
                .document(customerId)
                .collection("bookings")
                .document(bookingId)
                .update(update);

        btnRate.setVisibility(View.GONE);

        Toast.makeText(requireContext(),
                "Thank you for your feedback!",
                Toast.LENGTH_SHORT).show();
    }

    private void fetchProfessionalAddress() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .document(professionalId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        address.setText("Address unavailable");
                        return;
                    }

                    // ADDRESS MAP
                    Map<String, Object> addr = (Map<String, Object>) doc.get("address");

                    if (addr != null) {
                        String street = (String) addr.get("street");
                        String room = (String) addr.get("room");
                        String city = (String) addr.get("city");
                        String province = (String) addr.get("province");
                        String postal = (String) addr.get("postalCode");

                        String fullAddress = street + " " + room + ", " +
                                city + " " + province + " " + postal;

                        address.setText(fullAddress);
                    } else {
                        address.setText("Address unavailable");
                    }

                    // GEOPOINT
                    GeoPoint geo = doc.getGeoPoint("geo");

                    if (geo != null) {
                        proLatLng = new LatLng(geo.getLatitude(), geo.getLongitude());
                    }
                })
                .addOnFailureListener(e -> address.setText("Address unavailable"));
    }

}
