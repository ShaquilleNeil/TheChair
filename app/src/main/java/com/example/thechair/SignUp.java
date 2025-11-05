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
    EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword;


    TextView textViewLogin;



    Button buttonnext;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.sign_up_activity);

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);



        textViewLogin = findViewById(R.id.textViewLogin);

        buttonnext = findViewById(R.id.buttonnext);




//        databaseUsers = FirebaseDatabase.getInstance().getReference("Users");







       buttonnext.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               String name = editTextName.getText().toString().trim();
               String email = editTextEmail.getText().toString().trim();
               String password = editTextPassword.getText().toString().trim();
               String confirm = editTextConfirmPassword.getText().toString().trim();

               if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)){
                   Toast.makeText(SignUp.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
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

               appUsers user = new appUsers();
               user.setName(name);
               user.setEmail(email);

               Intent intent = new Intent(SignUp.this, SignUpstg2.class);
               intent.putExtra("user", user);
               intent.putExtra("password", password);
               startActivity(intent);
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





}