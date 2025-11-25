// Shaq’s Notes:
// This activity is doing a neat little dance: it collects the service info,
// normalizes it, then atomically writes both the service and its search tags
// to Firestore. The dual-update batch is the clever part: it keeps your search
// index (tags) and your actual data (services) perfectly in sync.
// You’ve also wired in a local cache update, which keeps navigation snappy.
// Only tiny improvement might be trimming tags more defensively, but the flow
// is solid as a barber’s fade.

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
    protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Invalid price or duration", Toast.LENGTH_SHORT).show();
            return;
        }

        appUsers.Service newService = new appUsers.Service(name, price, duration);

        Map<String, Object> serviceMap = new HashMap<>();
        serviceMap.put("name", name);
        serviceMap.put("price", price);
        serviceMap.put("duration", duration);

        List<String> keywords = generateKeywords(name.toLowerCase());

        String userId = currentUser.getId();
        DocumentReference userRef = db.collection("Users").document(userId);

        WriteBatch batch = db.batch();

        batch.update(userRef, "services", FieldValue.arrayUnion(serviceMap));

        for (String kw : keywords) {
            batch.update(userRef, "tags", FieldValue.arrayUnion(kw));
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Service added and tags updated", Toast.LENGTH_SHORT).show();

                    List<appUsers.Service> local = currentUser.getServices();
                    if (local == null) local = new ArrayList<>();
                    local.add(newService);
                    currentUser.setServices(local);

                    List<String> tagCache = currentUser.getTags();
                    if (tagCache == null) tagCache = new ArrayList<>();
                    for (String kw : keywords) {
                        if (!tagCache.contains(kw)) tagCache.add(kw);
                    }
                    currentUser.setTags(tagCache);

                    UserManager.getInstance().setUser(currentUser);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding service: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private List<String> generateKeywords(String name) {
        List<String> out = new ArrayList<>();
        for (String p : name.split(" ")) {
            if (!p.trim().isEmpty()) out.add(p.trim());
        }
        out.add(name.trim());
        return out;
    }
}
