package com.example.thechair.Professional;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.Adapters.GalleryAdapter;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.example.thechair.AuthFlow.AuthFlow;
import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProProfileFragment extends Fragment {

    private Button btnSignout, btnworkUpload, btnEditProfile;
    private ImageView profileimage;
    private TextView name, profession;

    private Uri imageUri;
    private RecyclerView galleryRecyclerView;
    private GalleryAdapter galleryAdapter;
    private final List<String> portfolioImages = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                getActivity();
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Refresh profile after editing
                    loadUser();
                    loadPortfolio();
                }
            });


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pro_profile, container, false);

        btnSignout = view.findViewById(R.id.btn_sign_out);
        profileimage = view.findViewById(R.id.profileImage);
        name = view.findViewById(R.id.username);
        profession = view.findViewById(R.id.userProfession);
        btnworkUpload = view.findViewById(R.id.btnworkUpload);
        galleryRecyclerView = view.findViewById(R.id.galleryRecyclerView);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);


        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        galleryAdapter = new GalleryAdapter(requireContext(), portfolioImages);
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        galleryRecyclerView.setAdapter(galleryAdapter);

        // Load user + images
        loadUser();
        loadPortfolio();

        btnworkUpload.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        btnEditProfile.setOnClickListener(v -> {
                editProfileLauncher.launch(new Intent(getActivity(), ProEditProfile.class));


        });

        btnSignout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), AuthFlow.class));
            requireActivity().finish();
        });

        return view;
    }

    // ---------- FIRESTORE: LOAD USER ----------
    private void loadUser() {
        UserManager userManager = UserManager.getInstance();
        appUsers cachedUser = userManager.getUser();

        if (cachedUser != null) {
            name.setText(cachedUser.getName());
            profession.setText(cachedUser.getProfession());
            Bitmap cachedBitmap = userManager.getProfileBitmap();
            if (cachedBitmap != null) {
                profileimage.setImageBitmap(cachedBitmap);
            } else {
                profileimage.setImageResource(R.drawable.banner);
            }
        }

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            db.collection("Users").document(firebaseUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            appUsers data = doc.toObject(appUsers.class);
                            if (data != null) {
                                name.setText(data.getName());
                                profession.setText(data.getProfession());
                                if (data.getProfilepic() != null) {
                                    new ImageLoaderTask(data.getProfilepic(), profileimage, userManager).execute();
                                }
                                userManager.setUser(data);
                            }
                        }
                    });
        }
    }

    // ---------- FIRESTORE: LOAD PORTFOLIO IMAGES ----------
    private void loadPortfolio() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        db.collection("Users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> images = (List<String>) doc.get("portfolioImages");
                        Log.d("FirestoreCheck", "Fetched portfolioImages: " + images);
                        if (images != null) {
                            portfolioImages.clear();
                            portfolioImages.addAll(images);
                            Log.d("AdapterCheck", "Adapter updated with " + portfolioImages.size() + " images");
                            galleryAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading portfolio", e));
    }

    // ---------- FIREBASE STORAGE: UPLOAD IMAGE ----------
    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference("portfolioImages/" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    Log.d("Upload", "✅ Uploaded image: " + url);

                    saveImageUrlToFirestore(url);
                    portfolioImages.add(url);
                    galleryAdapter.notifyItemInserted(portfolioImages.size() - 1);
                }))
                .addOnFailureListener(e -> Log.e("Upload", "Upload failed: " + e.getMessage()));
    }

    // ---------- FIRESTORE: SAVE IMAGE URL ----------
    private void saveImageUrlToFirestore(String url) {
        String userId = auth.getCurrentUser().getUid();

        db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> existing = (List<String>) documentSnapshot.get("portfolioImages");
                        if (existing == null) existing = new ArrayList<>();
                        existing.add(url);
                        Map<String, Object> update = new HashMap<>();
                        update.put("portfolioImages", existing);
                        db.collection("Users").document(userId).set(update, SetOptions.merge());
                    }
                });
    }

    // ---------- IMAGE PICKER ----------
    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    uploadImageToFirebase(uri);
                }
            });

    // ---------- PROFILE IMAGE LOADER ----------
    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;
        private final UserManager userManager;

        ImageLoaderTask(String url, ImageView imageView, UserManager userManager) {
            this.url = url;
            this.imageView = imageView;
            this.userManager = userManager;
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
                userManager.setProfileBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.banner);
            }
        }
    }
}
