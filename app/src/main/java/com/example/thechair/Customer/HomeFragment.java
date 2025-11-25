// Shaq’s Notes:
// This fragment is the customer’s home screen—your dashboard of beauty mayhem.
// It pulls your profile, showcases categories (braids, locs, weave, haircut),
// displays a rotating banner fetched from Firebase Storage, and loads a curated,
// daily-shuffled list of professionals.
//
// Under the hood: cached user data, background image loading, Firestore querying,
// banner rotation using a Handler loop, and fragment navigation for category taps.
// It's a kitchen-sink fragment, but it's organized and predictable.

package com.example.thechair.Customer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.thechair.Adapters.ProfessionalsAdapter;
import com.example.thechair.Professional.PublicProfileFragment;
import com.example.thechair.R;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView username, tvprovidername, weaveText, locsText, haircutText, braidsText;
    private ImageView profileimage, braids, weave, locs, haircut, banner;

    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;

    private ProfessionalsAdapter adapter;
    private List<appUsers> professionals = new ArrayList<>();

    private final List<String> bannerUrls = new ArrayList<>();
    private List<String> bannerTitles = new ArrayList<>();
    private int rotationInterval = 4000;

    private int currentIndex = 0;
    private Runnable bannerRunnable;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public HomeFragment() {}

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

        // Horizontal list of pros
        recyclerView = view.findViewById(R.id.serviceProvidersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        adapter = new ProfessionalsAdapter(getContext(), professionals);
        recyclerView.setAdapter(adapter);

        adapter.setOnProfessionalClickListener(pro -> {
            PublicProfileFragment fragment = new PublicProfileFragment();
            Bundle args = new Bundle();
            args.putString("professionalId", pro.getId());
            fragment.setArguments(args);

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.appMainView, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Bind UI
        username = view.findViewById(R.id.username);
        profileimage = view.findViewById(R.id.profileImage);
        haircutText = view.findViewById(R.id.haircutText);
        weaveText = view.findViewById(R.id.weaveText);
        locsText = view.findViewById(R.id.locsText);
        braidsText = view.findViewById(R.id.braidsText);
        banner = view.findViewById(R.id.banner);

        haircut = view.findViewById(R.id.haircut);
        weave = view.findViewById(R.id.weave);
        locs = view.findViewById(R.id.locs);
        braids = view.findViewById(R.id.braids);

        // Category shortcuts → open SearchFragment prefilled
        locs.setOnClickListener(v -> openSearch(locsText.getText().toString()));
        weave.setOnClickListener(v -> openSearch(weaveText.getText().toString()));
        haircut.setOnClickListener(v -> openSearch(haircutText.getText().toString()));
        braids.setOnClickListener(v -> openSearch(braidsText.getText().toString()));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadUser();
        loadProfessionals();
        loadbanners();

        profileimage.setOnClickListener(v -> {
            ProfileFragment profileFragment = new ProfileFragment();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.appMainView, profileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    // Load and cache logged-in user
    private void loadUser() {
        UserManager userManager = UserManager.getInstance();
        appUsers cachedUser = userManager.getUser();

        if (cachedUser != null) {
            username.setText(cachedUser.getName());

            Bitmap cachedBitmap = userManager.getProfileBitmap();
            if (cachedBitmap != null) profileimage.setImageBitmap(cachedBitmap);
            else profileimage.setImageResource(R.drawable.banner);
        }

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
                                } else profileimage.setImageResource(R.drawable.banner);

                                userManager.setUser(firebaseUserData);
                            }
                        }
                    });
        }
    }

    // Make randomness stable for the day
    private Random getDailyRandom() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        return new Random(today.hashCode());
    }

    // Load first five professionals (random order per day)
    private void loadProfessionals() {
        db.collection("Users")
                .whereEqualTo("role", "professional")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    professionals.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        appUsers user = document.toObject(appUsers.class);
                        professionals.add(user);
                    }

                    Collections.shuffle(professionals, getDailyRandom());

                    if (professionals.size() > 5)
                        professionals = professionals.subList(0, 5);

                    adapter.updateList(professionals);
                });
    }

    // Background bitmap fetch
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
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setDoInput(true);
                connection.connect();

                try (InputStream input = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(input);
                }
            } catch (Exception ignored) {}
            return null;
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

    private void openSearch(String search) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString("search", search);
        fragment.setArguments(args);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.appMainView, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Load banner config from Firestore
    private void loadbanners() {
        db.collection("appConfig").document("HomePage")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    bannerTitles = (List<String>) doc.get("bannerTitles");
                    if (bannerTitles == null) bannerTitles = new ArrayList<>();

                    rotationInterval = doc.getLong("rotationalInterval").intValue();

                    List<String> gsPaths = (List<String>) doc.get("banners");
                    if (gsPaths == null || gsPaths.isEmpty()) return;

                    bannerUrls.clear();

                    for (String gsPath : gsPaths) {
                        getDownloadUrl(gsPath);
                    }
                })
                .addOnFailureListener(e -> Log.e("BANNERS", "Error: " + e));
    }

    // Start rotation once URLs are loaded
    private void startBannerRotation() {
        if (bannerUrls.isEmpty()) return;

        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                Glide.with(requireContext())
                        .load(bannerUrls.get(currentIndex))
                        .into(banner);

                currentIndex = (currentIndex + 1) % bannerUrls.size();

                bannerHandler.postDelayed(this, rotationInterval);
            }
        };

        bannerHandler.post(bannerRunnable);
    }

    // Convert gs:// paths to HTTPS URLs
    private void getDownloadUrl(String gsPath) {
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(gsPath);

        ref.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    bannerUrls.add(uri.toString());

                    if (bannerUrls.size() == bannerTitles.size() || bannerTitles.isEmpty()) {
                        startBannerRotation();
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("BANNERS", "Failed: " + gsPath + " → " + e.getMessage())
                );
    }
}
