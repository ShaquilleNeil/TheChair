/** ------------------------------------------------------------
 *  Shaq’s Notes:
 *  - This screen shows the professional’s list of services.
 *  - Services are stored in Firestore under Users/{proId}/services[].
 *  - We load them once on screen open, and again onResume()
 *    so that returning from AddService instantly refreshes.
 *  - Adapter takes a List<Map<String,Object>> since services
 *    are simple Firestore objects with name/price/duration.
 *  - btnAddService pushes to AddService screen.
 *  - No business logic here — this is purely a display/refresh UI.
 * ------------------------------------------------------------- */

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

    // RecyclerView + button
    private RecyclerView rviewServices;
    private Button btnAddService;

    // Adapter + local storage for services
    private ServiceAdapter adapter;
    private final ArrayList<Map<String, Object>> servicesList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_services);

        // UI binding
        rviewServices = findViewById(R.id.rviewServices);
        btnAddService = findViewById(R.id.btnAddService);

        // Setup Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // RecyclerView setup
        adapter = new ServiceAdapter(this, servicesList);
        rviewServices.setLayoutManager(new LinearLayoutManager(this));
        rviewServices.setAdapter(adapter);

        // Load current services once
        loadServices();

        // Navigate to AddService screen
        btnAddService.setOnClickListener(v -> {
            Intent intent = new Intent(MyServices.this, AddService.class);
            startActivity(intent);
        });
    }

    /** ------------------------------------------------------------
     *  Load all service objects from Firestore:
     *  - services is a List<Map<String,Object>>
     *  - stored under Users/{uid}/services
     * ------------------------------------------------------------- */
    private void loadServices() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Firestore services list
                    List<Map<String, Object>> rawServices =
                            (List<Map<String, Object>>) snapshot.get("services");

                    servicesList.clear();

                    if (rawServices != null && !rawServices.isEmpty()) {
                        servicesList.addAll(rawServices);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load services: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /** ------------------------------------------------------------
     *  onResume():
     *  - Reload services when returning from AddService.
     * ------------------------------------------------------------- */
    @Override
    protected void onResume() {
        super.onResume();
        loadServices();
    }
}
