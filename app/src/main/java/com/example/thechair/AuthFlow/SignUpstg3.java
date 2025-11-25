// Shaq’s Notes:
// Signup Step 3 — the final stage.
// This screen completes the user’s account setup by choosing a role (customer/professional),
// optionally entering a profession, optionally uploading a profile image,
// and finally creating the Firebase Auth account and Firestore user document.
//
// Flow summary:
// 1. Receive user object + password from previous steps.
// 2. User selects role (Customer or Professional). If Professional → show profession field.
// 3. User may pick an image (stored in Firebase Storage under profileImages/uid.jpg).
// 4. On Sign Up:
//      - Validate role and profession (if needed)
//      - Create FirebaseAuth account
//      - Upload profile image (optional)
//      - Save full appUsers object to Firestore
//      - Navigate to correct home screen based on role
//
// This is the final step before the user enters the app.

package com.example.thechair.AuthFlow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
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

    // -------------------- UI Elements --------------------
    CheckBox checkBoxTerms;
    RadioGroup radioGroupUserType;
    ImageView imageviewprofile;
    TextView tvprofession;
    EditText editTextProfession;

    // Firebase Storage ref (used for profile image upload)
    StorageReference storageReference;

    Button btnsignup, btnupload;

    Uri imageUri;           // holds selected image
    FirebaseAuth mAuth;
    DatabaseReference databaseUsers; // legacy RTDB ref (not needed for Firestore-only signup)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.authflow_sign_upstg3_activity);

        // -------------------- Bind UI --------------------
        checkBoxTerms = findViewById(R.id.checkBoxTerms);
        radioGroupUserType = findViewById(R.id.radioGroupUserType);
        imageviewprofile = findViewById(R.id.imageViewprofile);
        tvprofession = findViewById(R.id.tvprofession);
        editTextProfession = findViewById(R.id.editTextProfession);

        btnsignup = findViewById(R.id.buttonSignUp);
        btnupload = findViewById(R.id.buttonuploadimage);

        // Firebase
        databaseUsers = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // -------------------- Role selection logic --------------------
        radioGroupUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioServiceProvider) {
                // Professional: show profession field
                tvprofession.setVisibility(View.VISIBLE);
                editTextProfession.setVisibility(View.VISIBLE);
            } else {
                // Customer: hide profession field
                tvprofession.setVisibility(View.GONE);
                editTextProfession.setVisibility(View.GONE);
            }
        });

        // -------------------- Image Picker --------------------
        btnupload.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        // -------------------- Final Sign Up --------------------
        btnsignup.setOnClickListener(v -> {
            if (checkBoxTerms.isChecked()) {
                registerUser();
            } else {
                Toast.makeText(SignUpstg3.this, "Please accept terms and conditions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------------------- Image Picker Launcher --------------------
    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imageviewprofile.setImageURI(uri);
                }
            });

    // -------------------- Main Registration Logic --------------------
    private void registerUser() {

        // Retrieve user object from previous step
        appUsers user = (appUsers) getIntent().getSerializableExtra("user");
        String pass = getIntent().getStringExtra("password");

        // Validate role
        int selectedId = radioGroupUserType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select a user type", Toast.LENGTH_SHORT).show();
            return;
        }

        // If professional, ensure profession is filled
        String profession = editTextProfession.getText().toString();
        if (selectedId == R.id.radioServiceProvider && profession.isEmpty()) {
            Toast.makeText(this, "Please enter a profession", Toast.LENGTH_SHORT).show();
            return;
        }

        // Attach profession if needed
        if (selectedId == R.id.radioServiceProvider) {
            user.setProfession(profession);
        }

        // Attach role to user model
        String role = selectedId == R.id.radioCustomer ? "customer" : "professional";
        user.setRole(role);

        // Create FirebaseAuth account
        mAuth.createUserWithEmailAndPassword(user.getEmail(), pass)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        Toast.makeText(SignUpstg3.this,
                                "Something went wrong: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        String uid = currentUser.getUid();
                        user.setId(uid);
                        saveUserWithImage(user, uid);
                    }
                });
    }

    // -------------------- Upload Image (optional) --------------------
    private void saveUserWithImage(appUsers user, String uid) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (imageUri != null) {
            // Upload file to Storage
            StorageReference imageRef = storageReference.child("profileImages/" + uid + ".jpg");

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        // Store download URL in user object
                                        user.setProfilepic(uri.toString());
                                        saveUserToFirestore(user, db);
                                    })
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(SignUpstg3.this,
                                    "Image upload failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        } else {
            // No image chosen — save user as-is
            saveUserToFirestore(user, db);
        }
    }

    // -------------------- Save Final User Document to Firestore --------------------
    private void saveUserToFirestore(appUsers user, FirebaseFirestore db) {

        db.collection("Users").document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpstg3.this, "Registration successful", Toast.LENGTH_SHORT).show();

                    // Redirect to correct home screen
                    if ("professional".equals(user.getRole())) {
                        startActivity(new Intent(SignUpstg3.this, ServiceProviderHome.class));
                    } else {
                        startActivity(new Intent(SignUpstg3.this, CustomerHome.class));
                    }

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SignUpstg3.this,
                                "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
