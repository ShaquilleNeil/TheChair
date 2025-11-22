package com.example.thechair.Professional;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProEditProfile extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 101;

    private ImageView editProfileImage;
    private ImageButton btnChangePhoto;
    private EditText inputName, inputProfession, inputPhone, inputEmail,
            inputAddressLine1, inputAddressLine2, inputCity,
            inputProvince, inputPostalCode;

    private Button btnSaveChanges, btnCancel;

    private Uri selectedImageUri = null;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pro_edit_profile);

        initViews();
        loadUserData();

        btnChangePhoto.setOnClickListener(v -> openGallery());
        btnSaveChanges.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        editProfileImage = findViewById(R.id.editProfileImage);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        inputName = findViewById(R.id.inputName);
        inputProfession = findViewById(R.id.inputProfession);
        inputPhone = findViewById(R.id.inputPhone);
        inputEmail = findViewById(R.id.inputEmail);

        inputAddressLine1 = findViewById(R.id.inputAddressLine1);
        inputAddressLine2 = findViewById(R.id.inputAddressLine2);
        inputCity = findViewById(R.id.inputCity);
        inputProvince = findViewById(R.id.inputProvince);
        inputPostalCode = findViewById(R.id.inputPostalCode);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCancel = findViewById(R.id.btnCancel);
    }

    // -----------------------------
    // LOAD DATA FROM FIRESTORE
    // -----------------------------
    private void loadUserData() {
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    fillFields(doc);

                    String photoUrl = doc.getString("profilepic");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(editProfileImage);

                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void fillFields(DocumentSnapshot doc) {
        inputName.setText(doc.getString("name"));
        inputProfession.setText(doc.getString("profession"));
        inputPhone.setText(doc.getString("phoneNumber"));
        inputEmail.setText(doc.getString("email"));

        // Address
        Map<String, Object> address = (Map<String, Object>) doc.get("address");
        if (address != null) {
            inputAddressLine1.setText((String) address.get("street"));
            inputAddressLine2.setText((String) address.get("room"));
            inputCity.setText((String) address.get("city"));
            inputProvince.setText((String) address.get("province"));
            inputPostalCode.setText((String) address.get("postalCode"));
        }
    }

    // -----------------------------
    // PICK PHOTO
    // -----------------------------
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                editProfileImage.setImageBitmap(bmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // -----------------------------
    // SAVE PROFILE
    // -----------------------------
    private void saveProfile() {

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", inputName.getText().toString());
        userMap.put("profession", inputProfession.getText().toString());
        userMap.put("phoneNumber", inputPhone.getText().toString());

        // Address
        Map<String, Object> address = new HashMap<>();
        address.put("street", inputAddressLine1.getText().toString());
        address.put("room", inputAddressLine2.getText().toString());
        address.put("city", inputCity.getText().toString());
        address.put("province", inputProvince.getText().toString());
        address.put("postalCode", inputPostalCode.getText().toString());
        userMap.put("address", address);

        // Full address for geocoding
        String fullAddress =
                inputAddressLine1.getText().toString() + ", " +
                        inputCity.getText().toString() + ", " +
                        inputProvince.getText().toString() + ", " +
                        inputPostalCode.getText().toString();

        // ========================================
        // ⭐ RUN GEOCODER ON BACKGROUND THREAD
        // ========================================
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(ProEditProfile.this);
                List<android.location.Address> results =
                        geocoder.getFromLocationName(fullAddress, 1);

                if (results != null && !results.isEmpty()) {
                    double lat = results.get(0).getLatitude();
                    double lng = results.get(0).getLongitude();
                    userMap.put("geo", new com.google.firebase.firestore.GeoPoint(lat, lng));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Back to main thread to update Firestore
            runOnUiThread(() -> {
                if (selectedImageUri != null) {
                    uploadImageAndSave(userMap);
                } else {
                    updateFirestore(userMap);
                }
            });
        }).start();
    }



    // -----------------------------
    // UPLOAD IMAGE TO FIREBASE
    // -----------------------------
    private void uploadImageAndSave(Map<String, Object> userMap) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("profileImages/" + uid + ".jpg");

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    userMap.put("profilepic", uri.toString());
                                    updateFirestore(userMap);
                                }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show());
    }

    private void updateFirestore(Map<String, Object> userMap) {
        db.collection("Users").document(uid)
                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent();
                    i.putExtra("updated", true);
                    setResult(Activity.RESULT_OK, i);
                    finish();


                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show());
    }

}
