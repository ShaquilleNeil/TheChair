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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookingDetailsFragment extends Fragment {

    private ImageView proImage;
    private TextView proName, proProfession, serviceName, appointmentDate,
            appointmentTime, appointmentDuration, appointmentPrice, appointmentStatus, address;

    private Button btnViewProfile, btnDirections, btnCancel, btnRebook, btnRate;

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

        // =======================
        // BIND UI
        // =======================
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



        // =======================
        // GET BUNDLE VALUES
        // =======================
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

        // =======================
        // FILL UI
        // =======================
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

        // =======================
        // BUTTONS
        // =======================
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

        if (status.equalsIgnoreCase("cancelled") || !isRated) {
            btnCancel.setVisibility(View.GONE);
            btnRate.setVisibility(View.VISIBLE);


        }else if (status.equalsIgnoreCase("completed") || !isRated){
            btnCancel.setVisibility(View.GONE);
            btnRate.setVisibility(View.VISIBLE);


        }

        btnCancel.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cancel Booking?")
                    .setMessage("Are you sure you want to cancel this appointment?")
                    .setPositiveButton("Yes, cancel", (dialog, which) -> {
                        cancelBooking();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();


        });

        btnRate.setOnClickListener(v -> {
            showRatingDialog();

        });


        btnRebook.setOnClickListener(v -> {
            startRebook();
            Toast.makeText(requireContext(),
                    "Rebooking " + service,
                    Toast.LENGTH_SHORT).show();
        });

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

    private void cancelBooking() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String customerID = FirebaseAuth.getInstance().getUid();

        db.collection("Users")
                .document(professionalId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
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

    private void showRatingDialog() {
     View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.rating_window, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etComment = dialogView.findViewById(R.id.etComment);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rate Experience")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String comment = etComment.getText().toString();
                    float rating = ratingBar.getRating();
                    if(rating == 0f) {
                        Toast.makeText(requireContext(), "Please rate the experience", Toast.LENGTH_SHORT).show();
                        return;

                    }

                 submitRating(rating, comment);


                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void submitRating(float rating, String comment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String customerID = FirebaseAuth.getInstance().getUid();
        db.collection("Users")
                .document(customerID)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String customerName = userDoc.getString("name");
                    String ratingid = UUID.randomUUID().toString();

                    Map<String, Object> ratingData = new HashMap<>();
                    ratingData.put("customerID", customerID);
                    ratingData.put("customerName", customerName);
                    ratingData.put("bookingId", bookingId);
                    ratingData.put("rating", rating);
                    ratingData.put("comment", comment);
                    ratingData.put("timestamp", System.currentTimeMillis());

                    db.collection("Users")
                            .document(professionalId)
                            .collection("ratings")
                            .document(ratingid)
                            .set(ratingData)
                            .addOnSuccessListener(aVoid -> {
                                UpdateProfessionalRating(rating);
                                Toast.makeText(requireContext(), "Rating submitted", Toast.LENGTH_SHORT).show();

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed to submit rating", Toast.LENGTH_SHORT).show());


                });





    }

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

            double updatedRating = ((oldRating * oldCount) + newRating) / (oldCount + 1);
            long updatedCount = oldCount + 1;

            transaction.update(proRef, "rating", updatedRating);
            transaction.update(proRef, "ratingCount", updatedCount);

            return null;
        }).addOnSuccessListener(aVoid -> {
            markBookingAsRated();
        }).addOnFailureListener(e -> {
            Toast.makeText(requireContext(),
                    "Failed to update rating: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void markBookingAsRated() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String customerId = FirebaseAuth.getInstance().getUid();

        Map<String, Object> update = new HashMap<>();
        update.put("isRated", true);

        // pro side
        db.collection("Users")
                .document(professionalId)
                .collection("bookings")
                .document(bookingId)
                .update(update);

        // customer side
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


}
