package com.example.thechair.Customer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thechair.Adapters.appUsers;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CustomerEditProfile extends AppCompatActivity {

    EditText inputName, inputEmail, inputPhone, inputAddressline1, inputAddressline2, inputCity, inputProvince, inputPostalCode;
    Button btnSaveChanges, btnCancel;

ImageButton btnChangephoto;
    ImageView editProfileImage;
    FirebaseFirestore databaseUsers;
    StorageReference storageUsers;
    ArrayList<appUsers> usersList;
    Uri imageUri;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.customer_edit_profile_activity);

        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputAddressline1 = findViewById(R.id.inputAddressLine1);
        inputAddressline2 = findViewById(R.id.inputAddressLine2);
        inputCity = findViewById(R.id.inputCity);
        inputProvince = findViewById(R.id.inputProvince);
        inputPostalCode = findViewById(R.id.inputPostalCode);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCancel = findViewById(R.id.btnCancel);
        editProfileImage = findViewById(R.id.editProfileImage);
        btnChangephoto = findViewById(R.id.btnChangePhoto);

        usersList = new ArrayList<>();



        Intent intent = getIntent();

        String profilePicUrl = intent.getStringExtra("profilepic");

        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            new ImageLoaderTask(profilePicUrl, editProfileImage).execute();
        } else {
            editProfileImage.setImageResource(R.drawable.banner);
        }


        databaseUsers = FirebaseFirestore.getInstance();
        storageUsers = FirebaseStorage.getInstance().getReference("profileImages");


        inputName.setText(intent.getStringExtra("name"));
        inputEmail.setText(intent.getStringExtra("email"));
        inputPhone.setText(intent.getStringExtra("phone"));
        inputAddressline1.setText(intent.getStringExtra("addressLine1"));
        inputAddressline2.setText(intent.getStringExtra("addressLine2"));
        inputCity.setText(intent.getStringExtra("city"));
        inputProvince.setText(intent.getStringExtra("province"));
        inputPostalCode.setText(intent.getStringExtra("postalCode"));


        btnChangephoto.setOnClickListener(v -> {
            selectImageLauncher.launch("image/*");
        });

        btnSaveChanges.setOnClickListener(v -> {
            String name = inputName.getText().toString();
            String email = inputEmail.getText().toString();
            String phone = inputPhone.getText().toString();
            String street = inputAddressline1.getText().toString();
            String room = inputAddressline2.getText().toString();
            String city = inputCity.getText().toString();
            String province = inputProvince.getText().toString();
            String postalCode = inputPostalCode.getText().toString();


            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = user.getUid();

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("email", email);
            updates.put("phoneNumber", phone);

            Map<String, Object> address = new HashMap<>();
            address.put("street", street);
            address.put("room", room);
            address.put("city", city);
            address.put("province", province);
            address.put("postalCode", postalCode);
            updates.put("address", address);


            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("Users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());


        });

        btnCancel.setOnClickListener(v -> {
            finish();
        });


    }


    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;

        public ImageLoaderTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.banner);
            }
        }
    }

    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    editProfileImage.setImageURI(uri);
                    uploadProfileImageAndUpdateUser(uri);
                }
            });


    private void uploadProfileImageAndUpdateUser(Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        StorageReference imageRef = storageUsers.child(uid + ".jpg");

        if (imageUri != null) {

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Update Firestore with new image URL
                                databaseUsers.collection("Users").document(uid)
                                        .update("profilepic", uri.toString())
                                        .addOnSuccessListener(aVoid ->
                                                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to update Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }


}