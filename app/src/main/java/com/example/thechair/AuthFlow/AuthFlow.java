package com.example.thechair.AuthFlow;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thechair.Customer.CustomerHome;
import com.example.thechair.Professional.ServiceProviderHome;
import com.example.thechair.R;
import com.example.thechair.Adapters.appUsers;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthFlow extends AppCompatActivity {

    // Tabs
    private TextView tabLogin, tabSignup;

    // Containers
    private LinearLayout loginForm, signupForm;

    // Login fields
    private EditText loginEmail, loginPassword;
    private Button btnLogin;

    // Signup fields
    private EditText signupName, signupEmail, signupPassword, signupConfirm;
    private Button btnNextSignup;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.auth_flow_activity); // your merged XML

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupSegmentedControl();
        setupLoginLogic();
        setupSignupLogic();
    }

    private void initViews() {
        // Tabs
        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);

        // Forms
        loginForm = findViewById(R.id.loginForm);
        signupForm = findViewById(R.id.signupForm);

        // Login fields
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Signup fields
        signupName = findViewById(R.id.signupName);
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        signupConfirm = findViewById(R.id.signupConfirm);
        btnNextSignup = findViewById(R.id.btnNextSignup);
    }

    // -------------------------------
    // Segmented control: tab switching
    // -------------------------------
    private void setupSegmentedControl() {
        tabLogin.setOnClickListener(v -> showLoginTab());
        tabSignup.setOnClickListener(v -> showSignupTab());

        // Default = Login tab
        showLoginTab();
    }

    private void showLoginTab() {
        loginForm.setVisibility(View.VISIBLE);
        signupForm.setVisibility(View.GONE);

        tabLogin.setBackgroundResource(R.drawable.bg_segment_selected);
        tabSignup.setBackgroundResource(R.drawable.bg_segment_unselected);

        tabLogin.setTextColor(Color.WHITE);
        tabSignup.setTextColor(Color.parseColor("#4A4A4A"));
    }

    private void showSignupTab() {
        loginForm.setVisibility(View.GONE);
        signupForm.setVisibility(View.VISIBLE);

        tabSignup.setBackgroundResource(R.drawable.bg_segment_selected);
        tabLogin.setBackgroundResource(R.drawable.bg_segment_unselected);

        tabSignup.setTextColor(Color.WHITE);
        tabLogin.setTextColor(Color.parseColor("#4A4A4A"));
    }

    // -------------------------------
    // Login logic
    // -------------------------------
    private void setupLoginLogic() {
        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(AuthFlow.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(AuthFlow.this, "User not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("Users").document(user.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (!documentSnapshot.exists()) {
                                    Toast.makeText(AuthFlow.this, "User data not found", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String role = documentSnapshot.getString("role");
                                if ("customer".equals(role)) {
                                    startActivity(new Intent(AuthFlow.this, CustomerHome.class));
                                    finish();
                                } else if ("professional".equals(role)) {
                                    startActivity(new Intent(AuthFlow.this, ServiceProviderHome.class));
                                    finish();
                                } else {
                                    Toast.makeText(AuthFlow.this, "Unknown role", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AuthFlow.this, "Failed to get user role", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            });
                });
    }

    // -------------------------------
    // Signup logic (step 1)
    // -------------------------------
    private void setupSignupLogic() {
        btnNextSignup.setOnClickListener(v -> handleSignupNext());
    }

    private void handleSignupNext() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();
        String confirm = signupConfirm.getText().toString().trim();

        // 1. Validate *before* Firestore
        if (TextUtils.isEmpty(name) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupEmail.setError("Invalid email");
            signupEmail.requestFocus();
            return;
        }

        if (password.length() < 8) {
            signupPassword.setError("Password must be at least 8 characters");
            signupPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            signupConfirm.setError("Passwords do not match");
            signupConfirm.requestFocus();
            return;
        }

        // 2. Firestore check → async
        db.collection("Users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        Toast.makeText(AuthFlow.this, "Email already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 3. NO duplicate found → proceed
                    appUsers user = new appUsers();
                    user.setName(name);
                    user.setEmail(email);

                    Intent intent = new Intent(AuthFlow.this, SignUpstg2.class);
                    intent.putExtra("user", user);
                    intent.putExtra("password", password);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AuthFlow.this, "Error checking email", Toast.LENGTH_SHORT).show();
                });
    }

}
