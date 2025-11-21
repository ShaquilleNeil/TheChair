package com.example.thechair.Professional;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thechair.Adapters.DirectionsHelper;
import com.example.thechair.Adapters.GalleryAdapter;
import com.example.thechair.Adapters.ReviewsPopup;
import com.example.thechair.Adapters.ServiceAdapter;
import com.example.thechair.Customer.PickServiceActivity;
import com.example.thechair.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
import java.util.Map;

public class PublicProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView proName, proProfession, reviewCount;
    private RecyclerView servicesRecyclerView, galleryRecyclerView;
    private Button bookNowButton, btnDirections;

    private GalleryAdapter galleryAdapter;

    private FirebaseFirestore db;
    private String professionalId;
    private String profilePicUrl;
    private RatingBar ratingBar;
    private Double rating;

    private boolean servicesExpanded = false;

    private boolean portfolioExpanded = false;


    private LatLng proLatLng;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_public_profile, container, false);

        profileImage = view.findViewById(R.id.profileImage);
        proName = view.findViewById(R.id.proName);
        proProfession = view.findViewById(R.id.proProfession);
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView);
        galleryRecyclerView = view.findViewById(R.id.galleryRecyclerView);
        bookNowButton = view.findViewById(R.id.bookNowButton);
        btnDirections = view.findViewById(R.id.btnDirections);
        reviewCount = view.findViewById(R.id.reviewCount);
        ratingBar = view.findViewById(R.id.ratingBar);





        setupCollapsibles(view);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            professionalId = getArguments().getString("professionalId");
        }

        if (professionalId != null) {
            loadProfessionalData(professionalId);
        }



        bookNowButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PickServiceActivity.class);
            intent.putExtra("professionalId", professionalId);
            intent.putExtra("professionalName", proName.getText().toString());
            intent.putExtra("professionalProfession", proProfession.getText().toString());
            intent.putExtra("professionalProfilePic", profilePicUrl);

            startActivity(intent);

        });

        btnDirections.setOnClickListener(v -> {
            if (proLatLng != null) {
                DirectionsHelper.openExternalGoogleMaps(
                        requireContext(),
                        proLatLng,
                        proName.getText().toString()
                );
            } else {
                Toast.makeText(requireContext(), "Location unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        reviewCount.setOnClickListener(v -> {
            ReviewsPopup popup = new ReviewsPopup(requireContext(), professionalId);
            popup.show();
        });




        return view;
    }

    private void loadProfessionalData(String userId) {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name = doc.getString("name");
                    String profession = doc.getString("profession");
                    String profilePic = doc.getString("profilepic");
                    rating = doc.getDouble("rating");

                    if (rating != null) {
                        ratingBar.setRating(rating.floatValue());
                    } else {
                        ratingBar.setRating(0f);
                    }

                    Long count = doc.getLong("ratingCount");
                    if (count != null)
                        reviewCount.setText("(" + count + " reviews)");
                    else
                        reviewCount.setText("(0 reviews)");


                    proName.setText(name != null ? name : "Unknown");
                    proProfession.setText(profession != null ? profession : "Professional");

                    profilePicUrl = doc.getString("profilepic");

                    GeoPoint geo = doc.getGeoPoint("geo");

                    if (geo != null) {
                        proLatLng = new LatLng(geo.getLatitude(), geo.getLongitude());
                    }


                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(profileImage);
                    }


                    Object servicesObj = doc.get("services");
                    if (servicesObj instanceof List) {
                        List<Map<String, Object>> services = (List<Map<String, Object>>) servicesObj;
                        ServiceAdapter adapter = new ServiceAdapter(requireContext(), services);
                        adapter.setOnServiceClickListener(service -> {
                            Toast.makeText(requireContext(),
                                    "Selected " + service.get("name"),
                                    Toast.LENGTH_SHORT).show();
                        });
                        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        servicesRecyclerView.setAdapter(adapter);
                    }

                    Object galleryObj = doc.get("portfolioImages");
                    if (galleryObj instanceof List) {
                        List<String> urls = (List<String>) galleryObj;
                        GalleryAdapter galleryAdapter = new GalleryAdapter(requireContext(), urls);
                        galleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
                        galleryRecyclerView.setAdapter(galleryAdapter);

                        galleryAdapter.setOnItemClickListener(url -> {
                            Intent intent = new Intent(getContext(), ImageViewer.class);
                            intent.putExtra("imageUrl", url);
                            startActivity(intent);
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupCollapsibles(View view) {

        /* -------------------- SERVICES SECTION -------------------- */

        LinearLayout servicesHeader = view.findViewById(R.id.servicesHeader);
        FrameLayout servicesContent = view.findViewById(R.id.servicesContent);
        ImageView servicesArrow = view.findViewById(R.id.servicesArrow);

        servicesHeader.setOnClickListener(v -> {
            servicesExpanded = !servicesExpanded;

            if (servicesExpanded) {
                servicesContent.setVisibility(View.VISIBLE);
                servicesArrow.animate().rotation(180f).setDuration(200).start();
            } else {
                servicesContent.setVisibility(View.GONE);
                servicesArrow.animate().rotation(0f).setDuration(200).start();
            }
        });


        /* -------------------- PORTFOLIO SECTION -------------------- */

        LinearLayout portfolioHeader = view.findViewById(R.id.portfolioHeader);
        FrameLayout portfolioContent = view.findViewById(R.id.portfolioContent);
        ImageView portfolioArrow = view.findViewById(R.id.portfolioArrow);

        portfolioHeader.setOnClickListener(v -> {
            portfolioExpanded = !portfolioExpanded;

            if (portfolioExpanded) {
                portfolioContent.setVisibility(View.VISIBLE);
                portfolioArrow.animate().rotation(180f).setDuration(200).start();
            } else {
                portfolioContent.setVisibility(View.GONE);
                portfolioArrow.animate().rotation(0f).setDuration(200).start();
            }
        });
    }
}
