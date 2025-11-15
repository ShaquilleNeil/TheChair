package com.example.thechair.Customer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thechair.AuthFlow.AuthFlow;
import com.example.thechair.R;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView name, email, phone,profileaddress;
    private ImageView profileimage;

    private Button editprofile, signout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);



        name = view.findViewById(R.id.profileName);
        email = view.findViewById(R.id.profileEmail);
        phone = view.findViewById(R.id.profilePhone);
        profileimage = view.findViewById(R.id.profileImage);
        profileaddress = view.findViewById(R.id.profileAddress);
        editprofile = view.findViewById(R.id.editProfileButton);
        signout = view.findViewById(R.id.btnSignOut);








        loadUser();


        editprofile.setOnClickListener(v -> {
            appUsers user = UserManager.getInstance().getUser();
            if (user != null && user.getAddress() != null) {
                appUsers.Address addr = user.getAddress();

                Intent intent = new Intent(getActivity(), CustomerEditProfile.class);
                intent.putExtra("profilepic", user.getProfilepic());
                intent.putExtra("name", user.getName());
                intent.putExtra("email", user.getEmail());
                intent.putExtra("phone", user.getPhoneNumber());
                intent.putExtra("addressLine1", addr.getStreet());
                intent.putExtra("addressLine2", addr.getRoom());
                intent.putExtra("city", addr.getCity());
                intent.putExtra("province", addr.getProvince());
                intent.putExtra("country", addr.getCountry());
                intent.putExtra("postalCode", addr.getPostalCode());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "User address not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });

        signout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), AuthFlow.class);
            startActivity(intent);
            getActivity().finish();
        });


        return view;
    }


    private void loadUser() {
        UserManager userManager = UserManager.getInstance();
        appUsers cachedUser = userManager.getUser();

        if (cachedUser != null) {
            name.setText(cachedUser.getName());
            email.setText(cachedUser.getEmail());
            phone.setText(cachedUser.getPhoneNumber());

            // Build full address
            appUsers.Address address = cachedUser.getAddress();
            if (address != null) {
                StringBuilder fullAddress = new StringBuilder();
                if (address.getStreet() != null && !address.getStreet().isEmpty()) fullAddress.append(address.getStreet());
                if (address.getRoom() != null && !address.getRoom().isEmpty()) fullAddress.append(", ").append(address.getRoom());
                if (address.getCity() != null && !address.getCity().isEmpty()) fullAddress.append(", ").append(address.getCity());
                if (address.getProvince() != null && !address.getProvince().isEmpty()) fullAddress.append(", ").append(address.getProvince());
                if (address.getCountry() != null && !address.getCountry().isEmpty()) fullAddress.append(", ").append(address.getCountry());
                if (address.getPostalCode() != null && !address.getPostalCode().isEmpty()) fullAddress.append(", ").append(address.getPostalCode());

                profileaddress.setText(fullAddress.toString());
            } else {
                profileaddress.setText("Address not provided");
            }

            // Show cached image immediately if available
            Bitmap cachedBitmap = userManager.getProfileBitmap();
            if (cachedBitmap != null) {
                profileimage.setImageBitmap(cachedBitmap);
            } else {
                profileimage.setImageResource(R.drawable.banner);
            }
        }

        // Fetch latest from Firebase
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            appUsers firebaseUserData = documentSnapshot.toObject(appUsers.class);
                            if (firebaseUserData != null) {
                                name.setText(firebaseUserData.getName());
                                email.setText(firebaseUserData.getEmail());
                                phone.setText(firebaseUserData.getPhoneNumber());

                                // Update address again with latest data
                                appUsers.Address address = firebaseUserData.getAddress();
                                if (address != null) {
                                    StringBuilder fullAddress = new StringBuilder();
                                    if (address.getStreet() != null && !address.getStreet().isEmpty()) fullAddress.append(address.getStreet());
                                    if (address.getRoom() != null && !address.getRoom().isEmpty()) fullAddress.append(", ").append(address.getRoom());
                                    if (address.getCity() != null && !address.getCity().isEmpty()) fullAddress.append(", ").append(address.getCity());
                                    if (address.getProvince() != null && !address.getProvince().isEmpty()) fullAddress.append(", ").append(address.getProvince());
                                    if (address.getCountry() != null && !address.getCountry().isEmpty()) fullAddress.append(", ").append(address.getCountry());
                                    if (address.getPostalCode() != null && !address.getPostalCode().isEmpty()) fullAddress.append(", ").append(address.getPostalCode());

                                    profileaddress.setText(fullAddress.toString());
                                } else {
                                    profileaddress.setText("Address not provided");
                                }

                                String profilePic = firebaseUserData.getProfilepic();
                                if (profilePic != null) {
                                    new ProfileFragment.ImageLoaderTask(profilePic, profileimage, userManager).execute();
                                } else {
                                    profileimage.setImageResource(R.drawable.banner);
                                }

                                userManager.setUser(firebaseUserData);
                            }
                        }
                    });
        }
    }



    public static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;
        private final UserManager userManager;

        public ImageLoaderTask(String url, ImageView imageView, UserManager userManager) {
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
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                userManager.setProfileBitmap(bitmap); // cache the bitmap
            } else {
                imageView.setImageResource(R.drawable.banner);
            }
        }
    }
}