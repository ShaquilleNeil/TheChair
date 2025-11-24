package com.example.thechair.Professional;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.Adapters.ServiceAdapter;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyServices extends AppCompatActivity {

    private RecyclerView rviewServices;
    private Button btnAddService;
    private ServiceAdapter adapter;
    private final ArrayList<Map<String, Object>> servicesList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_services);

        rviewServices = findViewById(R.id.rviewServices);
        btnAddService = findViewById(R.id.btnAddService);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ServiceAdapter(this, servicesList);
        rviewServices.setLayoutManager(new LinearLayoutManager(this));
        rviewServices.setAdapter(adapter);

        // Load once when screen opens
        loadServices();

        btnAddService.setOnClickListener(v -> {
            Intent intent = new Intent(MyServices.this, AddService.class);
            startActivity(intent);
        });
    }

    private void loadServices() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        List<Map<String, Object>> rawServices =
                                (List<Map<String, Object>>) snapshot.get("services");

                        servicesList.clear();

                        if (rawServices != null && !rawServices.isEmpty()) {
                            servicesList.addAll(rawServices);
                        } else {
                            Toast.makeText(this, "No services found", Toast.LENGTH_SHORT).show();
                        }

                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load services: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload when returning from AddService screen
        loadServices();
    }
}
