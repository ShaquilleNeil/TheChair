package com.example.thechair.Professional;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.Adapters.Availability;
import com.example.thechair.Adapters.AvailabilityAdapter;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyAvailability extends AppCompatActivity {

    private RecyclerView recyclerAvailability;
    private Button btnSaveAvailability;
    private FirebaseFirestore db;
    private AvailabilityAdapter adapter;
    private List<Availability> availabilityList = new ArrayList<>();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_availability);

        recyclerAvailability = findViewById(R.id.recyclerAvailability);
        btnSaveAvailability = findViewById(R.id.btnSaveAvailability);

        db = FirebaseFirestore.getInstance();
        userId = UserManager.getInstance().getUser().getId();

        recyclerAvailability.setLayoutManager(new LinearLayoutManager(this));

        loadAvailability();

        btnSaveAvailability.setOnClickListener(v -> saveAvailability());
    }


    private void loadAvailability() {
        CollectionReference availRef = db.collection("users")
                .document(userId)
                .collection("availability");

        availRef.get().addOnSuccessListener(task -> {
            availabilityList.clear();

            if (task != null && !task.isEmpty()) {
                for (DocumentSnapshot doc : task.getDocuments()) {
                    Availability day = doc.toObject(Availability.class);
                    if (day != null) availabilityList.add(day);
                }
                ensureAllDaysPresent();
            } else {
                setDefaultAvailability();
            }

            adapter = new AvailabilityAdapter(this, availabilityList);
            recyclerAvailability.setAdapter(adapter);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load availability: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void ensureAllDaysPresent() {
        Map<String, Availability> map = new HashMap<>();
        for (Availability a : availabilityList) {
            map.put(a.getDay().toLowerCase(), a);
        }

        String[] allDays = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String d : allDays) {
            if (!map.containsKey(d)) {
                availabilityList.add(new Availability(d, false, "--", "--"));
            }
        }

        // Sort in Monday→Sunday order
        availabilityList.sort((a, b) -> dayIndex(a.getDay()) - dayIndex(b.getDay()));
    }


    private void setDefaultAvailability() {
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String day : days) {
            availabilityList.add(new Availability(day, false, "--", "--"));
        }
    }


    private void saveAvailability() {
        if (availabilityList.isEmpty()) return;

        CollectionReference availRef = db.collection("Users")
                .document(userId)
                .collection("availability");

        WriteBatch batch = db.batch();

        for (Availability day : availabilityList) {
            batch.set(availRef.document(day.getDay().toLowerCase()), day);
        }

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Availability saved successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving availability: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private int dayIndex(String day) {
        switch (day.toLowerCase()) {
            case "monday": return 1;
            case "tuesday": return 2;
            case "wednesday": return 3;
            case "thursday": return 4;
            case "friday": return 5;
            case "saturday": return 6;
            case "sunday": return 7;
            default: return 99;
        }
    }
}
