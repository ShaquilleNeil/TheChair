package com.example.thechair;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogIn extends AppCompatActivity {

    EditText editTextEmailLogin, editTextPasswordLogin;

    TextView textViewForgotPassword, textViewSignUp;

    Button buttonLogIn;

    FirebaseAuth mAuth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.log_in_activity);

        editTextEmailLogin = findViewById(R.id.editTextEmailLogin);
        editTextPasswordLogin = findViewById(R.id.editTextPasswordLogin);

        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        textViewSignUp = findViewById(R.id.textViewSignUp);

        buttonLogIn = findViewById(R.id.buttonLogin);

        mAuth = FirebaseAuth.getInstance();


        textViewSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogIn.this, SignUp.class);
                startActivity(intent);
            }
        });


        buttonLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

    }

    private void loginUser(){
        String email = editTextEmailLogin.getText().toString().trim();
        String password = editTextPasswordLogin.getText().toString().trim();

        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful())  {
                    //get user to check if it's their first time logging in
                    FirebaseUser user = mAuth.getCurrentUser();

                    if(user != null){
                        long creationTimestamp = user.getMetadata().getCreationTimestamp();
                        long lastSignInTimestamp = user.getMetadata().getLastSignInTimestamp();

                        //checking if the user is new
//                        if(creationTimestamp == lastSignInTimestamp){
//                            Toast.makeText(LogIn.this, "Welcome", Toast.LENGTH_SHORT).show();
//
//
//                        }else{
//                            Toast.makeText(LogIn.this, "Welcome", Toast.LENGTH_SHORT).show();
//                            startActivity(new Intent(LogIn.this, Home.class));
//
//                        }
                        finish();


                    }


                }else {
                    Toast.makeText(LogIn.this, "Something went wrong",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}