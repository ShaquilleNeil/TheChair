package com.example.thechair.AuthFlow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thechair.Customer.CustomerHome;
import com.example.thechair.R;
import com.example.thechair.Professional.ServiceProviderHome;
import com.example.thechair.Adapters.appUsers;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SignUpstg3 extends AppCompatActivity {

    CheckBox checkBoxTerms;
    RadioGroup radioGroupUserType;
    ImageView imageviewprofile;

    StorageReference storageReference;
    Button btnsignup, btnupload;

    Uri imageUri;
    FirebaseAuth mAuth;
    DatabaseReference databaseUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.authflow_sign_upstg3_activity);

        checkBoxTerms = findViewById(R.id.checkBoxTerms);
        radioGroupUserType = findViewById(R.id.radioGroupUserType);
        imageviewprofile = findViewById(R.id.imageViewprofile);
        databaseUsers = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference();



        btnsignup = findViewById(R.id.buttonSignUp);
        btnupload = findViewById(R.id.buttonuploadimage);





        mAuth = FirebaseAuth.getInstance();

        btnupload.setOnClickListener(v -> selectImageLauncher.launch("image/*"));


        btnsignup.setOnClickListener(v -> {
            if (checkBoxTerms.isChecked()) {
                registerUser();
                Intent intent = new Intent(SignUpstg3.this, LogIn.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(SignUpstg3.this, "Please accept terms and conditions", Toast.LENGTH_SHORT).show();
            }

        });







    }

    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imageviewprofile.setImageURI(uri);
                }
            });


    private void registerUser() {
        appUsers user = (appUsers) getIntent().getSerializableExtra("user");
        String pass = getIntent().getStringExtra("password");

        // Validate role selection
        int selectedId = radioGroupUserType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select a user type", Toast.LENGTH_SHORT).show();
            return;
        }
        String role = selectedId == R.id.radioCustomer ? "customer" : "professional";
        user.setRole(role);

        mAuth.createUserWithEmailAndPassword(user.getEmail(), pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            String uid = currentUser.getUid();
                            user.setId(uid);
                            // Save user with image if selected
                            saveUserWithImage(user, uid);
                        }
                    } else {
                        Toast.makeText(SignUpstg3.this, "Something went wrong: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserWithImage(appUsers user, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (imageUri != null) {
            // Upload profile image
            StorageReference imageRef = storageReference.child("profileImages/" + uid + ".jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                user.setProfilepic(uri.toString());
                                saveUserToFirestore(user, db);
                            })
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(SignUpstg3.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            saveUserToFirestore(user, db);
        }
    }

    private void saveUserToFirestore(appUsers user, FirebaseFirestore db) {
        db.collection("Users").document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpstg3.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    if ("professional".equals(user.getRole())) {
                        startActivity(new Intent(SignUpstg3.this, ServiceProviderHome.class));
                    } else {
                        startActivity(new Intent(SignUpstg3.this, CustomerHome.class));
                    }
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SignUpstg3.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


}