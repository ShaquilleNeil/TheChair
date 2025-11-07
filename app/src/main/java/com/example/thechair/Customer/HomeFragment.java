package com.example.thechair.Customer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thechair.Adapters.ProfessionalsAdapter;
import com.example.thechair.R;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private FirebaseFirestore db;
    private TextView username, tvprovidername;
    private ImageView profileimage;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private ProfessionalsAdapter adapter;
    private List<appUsers> professionals = new ArrayList<>();

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.serviceProvidersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        adapter = new ProfessionalsAdapter(getContext(), professionals);
        recyclerView.setAdapter(adapter);



        username = view.findViewById(R.id.username);
        profileimage = view.findViewById(R.id.profileImage);



        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();



        loadUser();
        loadProfessionals();


        profileimage.setOnClickListener(v -> {
            ProfileFragment profileFragment = new ProfileFragment();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.appMainView, profileFragment)
                    .addToBackStack(null) // optional, allows back navigation
                    .commit();
        });


        // Inflate the layout for this fragment
        return view;
    }


    private void loadUser() {
        UserManager userManager = UserManager.getInstance();
        appUsers cachedUser = userManager.getUser();

        if (cachedUser != null) {
            username.setText(cachedUser.getName());

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
                                username.setText(firebaseUserData.getName());

                                String profilePic = firebaseUserData.getProfilepic();
                                if (profilePic != null) {
                                    new ImageLoaderTask(profilePic, profileimage, userManager).execute();
                                } else {
                                    profileimage.setImageResource(R.drawable.banner);
                                }

                                userManager.setUser(firebaseUserData);
                            }
                        }
                    });
        }
    }



    private void loadProfessionals() {
        db.collection("Users").whereEqualTo("role", "professional").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    professionals.clear(); // clear old data before adding new

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            appUsers user = document.toObject(appUsers.class);
                            professionals.add(user);
                        }
                        adapter.notifyDataSetChanged(); // refresh RecyclerView
                    } else {
                        Log.d("TAG", "No professionals found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG", "Error loading professionals", e);
                    Toast.makeText(getContext(), "Error loading professionals", Toast.LENGTH_SHORT).show();
                });
    }

    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
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