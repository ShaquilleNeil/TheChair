package com.example.thechair.Customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thechair.Adapters.ServiceAdapter;
import com.example.thechair.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class PickServiceActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView proName, proProfession;

    private FirebaseFirestore db;
    private String professionalId;
    private RecyclerView servicesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pick_service);

        profileImage = findViewById(R.id.profileImage);
        proName = findViewById(R.id.proName);
        proProfession = findViewById(R.id.proProfession);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);

        // Receive Intent data
        professionalId = getIntent().getStringExtra("professionalId");

        proName.setText(getIntent().getStringExtra("professionalName"));
        proProfession.setText(getIntent().getStringExtra("professionalProfession"));

        String proProfilePic = getIntent().getStringExtra("professionalProfilePic");
        if (proProfilePic != null && !proProfilePic.isEmpty()) {
            Glide.with(this)
                    .load(proProfilePic)
                    .placeholder(R.drawable.ic_person)
                    .into(profileImage);
        }

        db = FirebaseFirestore.getInstance();

        loadServices(professionalId);
    }

    private void loadServices(String professionalId) {
        db.collection("Users").document(professionalId).get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Object servicesObj = doc.get("services");

                    if (servicesObj instanceof List) {
                        List<Map<String, Object>> services =
                                (List<Map<String, Object>>) servicesObj;

                        // Create adapter with 2 parameters
                        ServiceAdapter adapter = new ServiceAdapter(this, services);

                        // Handle the clicks separately
                        adapter.setOnServiceClickListener(service -> {
                           String serviceName = service.get("name").toString();
                           String servicePrice = service.get("price").toString();
                           String serviceDuration = service.get("duration").toString();

                           // Pass the data to the next activity
                            Intent intent = new Intent(this, PickDateActivity.class);
                            intent.putExtra("professionalId", professionalId);
                            intent.putExtra("professionalName", proName.getText().toString());
                            intent.putExtra("professionalProfilePic", getIntent().getStringExtra("professionalProfilePic"));
                            intent.putExtra("serviceName", serviceName);
                            intent.putExtra("servicePrice", servicePrice);
                            intent.putExtra("serviceDuration", serviceDuration);
                            startActivity(intent);
                            finish();

                        });

                        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                        servicesRecyclerView.setAdapter(adapter);
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load services", Toast.LENGTH_SHORT).show()
                );
    }
}
