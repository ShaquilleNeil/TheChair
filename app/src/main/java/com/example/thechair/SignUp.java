package com.example.thechair;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUp extends AppCompatActivity {
    EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword, editTextAddress1, editTextAddress2, editTextCity, editTextProvince, editTextPostalCode, editTextPhoneNumber;
    CheckBox checkBoxTerms;

    TextView textViewLogin;

    RadioGroup radioGroupUserType;

    Button buttonSignUp;

    FirebaseAuth mAuth;
    DatabaseReference databaseUsers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.sign_up_activity);

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        editTextAddress1 = findViewById(R.id.editTextAddress1);
        editTextAddress2 = findViewById(R.id.editTextAddress2);

        editTextCity = findViewById(R.id.editTextCity);

        editTextProvince = findViewById(R.id.editTextProvince);

        editTextPostalCode = findViewById(R.id.editTextPostalCode);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);

        checkBoxTerms = findViewById(R.id.checkBoxTerms);

        textViewLogin = findViewById(R.id.textViewLogin);

        buttonSignUp = findViewById(R.id.buttonSignUp);

        radioGroupUserType = findViewById(R.id.radioGroupUserType);


        mAuth = FirebaseAuth.getInstance();
//        databaseUsers = FirebaseDatabase.getInstance().getReference("Users");







        buttonSignUp.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               registerUser();
           }
       });


       textViewLogin.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Intent intent = new Intent(SignUp.this, LogIn.class);
               startActivity(intent);

           }
       });


    }

    private void registerUser(){
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirm = editTextConfirmPassword.getText().toString().trim();

        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)){
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Invalid Email");
            editTextEmail.requestFocus();
            return;
        }

        if(password.length() < 8){
            editTextPassword.setError("Password must be longer than 8 characters");
            editTextPassword.requestFocus();
            return;
        }

        if(!password.equals(confirm)){
            editTextConfirmPassword.setError("Passwords do not match");
            editTextConfirmPassword.requestFocus();
            return;
        }


        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful())  {
                    Toast.makeText(SignUp.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    saveUser();
//                    startActivity(new Intent(SignUp.this, LogIn.class));
                    finish();
                }else {
                    Toast.makeText(SignUp.this, "Something went wrong",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String address1 = editTextAddress1.getText().toString().trim();
        String address2 = editTextAddress2.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();
        String province = editTextProvince.getText().toString().trim();
        String postalCode = editTextPostalCode.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        String role;
        int selectedId = radioGroupUserType.getCheckedRadioButtonId();
        if (selectedId == R.id.radioCustomer) {
            role = "customer";
        } else if (selectedId == R.id.radioServiceProvider) {
            role = "professional";
        } else {
            role = "";
        }

        // Validation
        if (address1.isEmpty()) { Toast.makeText(this, "Please enter street address", Toast.LENGTH_SHORT).show(); return; }
        if (city.isEmpty()) { Toast.makeText(this, "Please enter city", Toast.LENGTH_SHORT).show(); return; }
        if (province.isEmpty()) { Toast.makeText(this, "Please enter province", Toast.LENGTH_SHORT).show(); return; }
        if (postalCode.isEmpty()) { Toast.makeText(this, "Please enter postal code", Toast.LENGTH_SHORT).show(); return; }
        if (phoneNumber.isEmpty()) { Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show(); return; }

        FirebaseUser currentUser = mAuth.getCurrentUser();


        if (currentUser != null) {
            String uid = currentUser.getUid(); // Use the UID as document ID

            // Firestore instance
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Create Address and Geo objects
            appUsers.Address address = new appUsers.Address(address1, address2, city, province, "Canada", postalCode);
            appUsers.Geo geo = new appUsers.Geo(0.0, 0.0);

            // Create user object
            appUsers user = new appUsers(uid, name, email, role, address, geo);

            // Save to Firestore
            db.collection("Users").document(uid)
                    .set(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SignUp.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                        if ("professional".equals(role)) {
                            startActivity(new Intent(SignUp.this, ServiceProviderHome.class));
                        } else {
                            startActivity(new Intent(SignUp.this, CustomerHome.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUp.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

    }




}