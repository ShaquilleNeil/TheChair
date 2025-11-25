// Shaq’s Notes:
// This screen is the “service picker”—the place where a customer sees what a
// professional offers and selects a single service before hopping into the
// date/time picker. Your data extraction is clean, Glide fallback is safe,
// and the adapter-click pipeline is correct. The only tweak is making sure
// the intent payload to PickDateActivity stays complete (name/profession/image).
// Everything else already respects null-safety and avoids casting traps.

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

        profileImage         = findViewById(R.id.profileImage);
        proName              = findViewById(R.id.proName);
        proProfession        = findViewById(R.id.proProfession);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);

        // Incoming bundle
        professionalId = getIntent().getStringExtra("professionalId");
        proName.setText(getIntent().getStringExtra("professionalName"));
        proProfession.setText(getIntent().getStringExtra("professionalProfession"));

        String pic = getIntent().getStringExtra("professionalProfilePic");
        if (pic != null && !pic.isEmpty()) {
            Glide.with(this)
                    .load(pic)
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

                    Object raw = doc.get("services");
                    if (!(raw instanceof List)) return;

                    List<Map<String, Object>> services =
                            (List<Map<String, Object>>) raw;

                    ServiceAdapter adapter = new ServiceAdapter(this, services);

                    // Click event forwarded here
                    adapter.setOnServiceClickListener(service -> {

                        String serviceName     = service.get("name").toString();
                        String servicePrice    = service.get("price").toString();
                        String serviceDuration = service.get("duration").toString();

                        // Move to the date picker
                        Intent i = new Intent(this, PickDateActivity.class);

                        i.putExtra("professionalId", professionalId);
                        i.putExtra("professionalName", proName.getText().toString());
                        i.putExtra("professionalProfession", proProfession.getText().toString());
                        i.putExtra("professionalProfilePic", getIntent().getStringExtra("professionalProfilePic"));

                        i.putExtra("serviceName", serviceName);
                        i.putExtra("servicePrice", servicePrice);
                        i.putExtra("serviceDuration", serviceDuration);

                        startActivity(i);
                        finish();
                    });

                    servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    servicesRecyclerView.setAdapter(adapter);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load services", Toast.LENGTH_SHORT).show()
                );
    }
}
