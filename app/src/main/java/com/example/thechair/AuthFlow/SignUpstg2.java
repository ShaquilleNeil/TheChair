package com.example.thechair.AuthFlow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thechair.R;
import com.example.thechair.Adapters.appUsers;

public class SignUpstg2 extends AppCompatActivity {

    EditText editTextAddress1, editTextAddress2, editTextCity, editTextProvince, editTextPostalCode, editTextPhoneNumber;
    Button buttonNext3, btn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.authflow_sign_upstg2_activity);

        editTextAddress1 = findViewById(R.id.editTextAddress1);
        editTextAddress2 = findViewById(R.id.editTextAddress2);

        editTextCity = findViewById(R.id.editTextCity);

        editTextProvince = findViewById(R.id.editTextProvince);

        editTextPostalCode = findViewById(R.id.editTextPostalCode);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);

        buttonNext3 = findViewById(R.id.buttonnext);

        //receive information from step 1
        appUsers user = (appUsers) getIntent().getSerializableExtra("user");
        String pass = getIntent().getStringExtra("password");

        buttonNext3.setOnClickListener(v -> {
            String address1 = editTextAddress1.getText().toString();
            String address2 = editTextAddress2.getText().toString();
            String city = editTextCity.getText().toString();
            String province = editTextProvince.getText().toString();
            String postalCode = editTextPostalCode.getText().toString();
            String phoneNumber = editTextPhoneNumber.getText().toString();

            if (address1.isEmpty() || city.isEmpty() || province.isEmpty() || postalCode.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(SignUpstg2.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            //update user object
            appUsers.Address address = new appUsers.Address();
            address.setStreet(address1);
            address.setRoom(address2);
            address.setCity(city);
            address.setProvince(province);
            address.setPostalCode(postalCode);
            user.setAddress(address);
            user.setPhoneNumber(phoneNumber);

            Intent intent = new Intent(SignUpstg2.this, SignUpstg3.class);
            intent.putExtra("user", user);
            intent.putExtra("password", pass);
            startActivity(intent);


        });



    }
}