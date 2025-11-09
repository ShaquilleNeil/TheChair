package com.example.thechair.Professional;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.example.thechair.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddService extends AppCompatActivity {

    private EditText inputServiceName, inputServicePrice, inputServiceDuration;
    private Button btnAddService;

    private FirebaseFirestore db;
    private appUsers currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_service);

        db = FirebaseFirestore.getInstance();
        currentUser = UserManager.getInstance().getUser();

        inputServiceName = findViewById(R.id.inputServiceName);
        inputServicePrice = findViewById(R.id.inputServicePrice);
        inputServiceDuration = findViewById(R.id.inputServiceDuration);
        btnAddService = findViewById(R.id.btnAddService);

        btnAddService.setOnClickListener(v -> addService());
    }

    private void addService() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = inputServiceName.getText().toString().trim();
        String priceStr = inputServicePrice.getText().toString().trim();
        String durationStr = inputServiceDuration.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int duration;

        try {
            price = Double.parseDouble(priceStr);
            duration = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price or duration", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create service object and convert to map
        appUsers.Service newService = new appUsers.Service(name, price, duration);
        Map<String, Object> serviceMap = new HashMap<>();
        serviceMap.put("name", name);
        serviceMap.put("price", price);
        serviceMap.put("duration", duration);

        // Generate keyword variations for better search results
        List<String> keywords = generateKeywords(name.toLowerCase());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = currentUser.getId();
        DocumentReference userRef = db.collection("Users").document(userId);

        // Use a Firestore batch for atomic updates
        WriteBatch batch = db.batch();
        batch.update(userRef, "services", FieldValue.arrayUnion(serviceMap));

        // Add all keyword variations as tags
        for (String keyword : keywords) {
            batch.update(userRef, "tags", FieldValue.arrayUnion(keyword));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Service added and tags updated successfully", Toast.LENGTH_SHORT).show();

                    // Update local cache
                    List<appUsers.Service> localServices = currentUser.getServices();
                    if (localServices == null) localServices = new ArrayList<>();
                    localServices.add(newService);
                    currentUser.setServices(localServices);

                    List<String> localTags = currentUser.getTags();
                    if (localTags == null) localTags = new ArrayList<>();
                    for (String keyword : keywords) {
                        if (!localTags.contains(keyword)) localTags.add(keyword);
                    }
                    currentUser.setTags(localTags);

                    UserManager.getInstance().setUser(currentUser);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding service: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private List<String> generateKeywords(String name) {
        List<String> keywords = new ArrayList<>();
        String[] parts = name.split(" ");
        for (String part : parts) {
            keywords.add(part.trim());
        }
        keywords.add(name.trim()); // add the full phrase too
        return keywords;
    }



}
