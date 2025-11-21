package com.example.thechair;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thechair.AuthFlow.AuthFlow;

import com.example.thechair.Customer.CustomerHome;
import com.example.thechair.Professional.ServiceProviderHome;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Small delay for visual smoothness
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser != null) {
                checkUserRole(currentUser.getUid());
            } else {
                startActivity(new Intent(SplashScreen.this, AuthFlow.class));
                finish();
            }
        }, 1);
    }

    // ----------------------------------------------------------
    //                   CHECK USER ROLE
    // ----------------------------------------------------------
    private void checkUserRole(String userId) {
        db.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String role = documentSnapshot.getString("role");

                        if ("customer".equalsIgnoreCase(role)) {
                            startActivity(new Intent(SplashScreen.this, CustomerHome.class));

                        } else if ("professional".equalsIgnoreCase(role)) {
                            startActivity(new Intent(SplashScreen.this, ServiceProviderHome.class));

                        } else {
                            Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, AuthFlow.class));
                        }

                    } else {
                        startActivity(new Intent(SplashScreen.this, AuthFlow.class));
                    }

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking user role", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SplashScreen.this, AuthFlow.class));
                    finish();
                });
    }
}
