package com.example.thechair.Professional;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thechair.Adapters.GalleryAdapter;
import com.example.thechair.Adapters.ServiceAdapter;
import com.example.thechair.Customer.PickServiceActivity;
import com.example.thechair.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class PublicProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView proName, proProfession;
    private RecyclerView servicesRecyclerView, galleryRecyclerView;
    private Button bookNowButton;

    private FirebaseFirestore db;
    private String professionalId;
    private String profilePicUrl;

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

        return view;
    }

    private void loadProfessionalData(String userId) {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name = doc.getString("name");
                    String profession = doc.getString("profession");
                    String profilePic = doc.getString("profilepic");

                    proName.setText(name != null ? name : "Unknown");
                    proProfession.setText(profession != null ? profession : "Professional");

                    profilePicUrl = doc.getString("profilepic");

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
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }
}
