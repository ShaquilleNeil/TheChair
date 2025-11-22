package com.example.thechair.Customer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.thechair.Adapters.DirectionsHelper;
import com.example.thechair.Professional.PublicProfileFragment;
import com.example.thechair.R;
import com.example.thechair.BuildConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class NearbyFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private final List<ProMarker> proMarkers = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted && googleMap != null) enableLocationAndZoom();
            });

    private EditText searchBar;

    public NearbyFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // 🔍 Attach search bar
        searchBar = view.findViewById(R.id.serviceSearchBar);
        ImageView searchFab = view.findViewById(R.id.searchFab);
        LinearLayout searchContainer = view.findViewById(R.id.searchContainer);
        EditText searchBar = view.findViewById(R.id.serviceSearchBar);

// Expand on tap
        searchFab.setOnClickListener(v -> {
            searchFab.setVisibility(View.GONE);
            searchContainer.setVisibility(View.VISIBLE);
            searchBar.requestFocus();
        });

// Collapse when empty + back pressed
        searchBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && searchBar.getText().toString().trim().isEmpty()) {
                searchContainer.setVisibility(View.GONE);
                searchFab.setVisibility(View.VISIBLE);
            }
        });

        setupSearchFiltering();

        return view;
    }


    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);


        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        googleMap.setOnMarkerClickListener(marker -> {
            String proId = marker.getTag().toString();
            String proName = marker.getTitle();
            LatLng pos = marker.getPosition();
            markerPopup(proId, proName, pos);
            return true;
        });

        enableLocationAndZoom();
        professionalsGeo();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }



    /* ============================================================
       SEARCH BAR — FILTER MARKERS BY TAG
       ============================================================ */
    private void setupSearchFiltering() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMarkers(s.toString());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMarkers(String query) {
        query = query.toLowerCase().trim();

        for (ProMarker pm : proMarkers) {
            boolean match = false;

            for (String tag : pm.tags) {
                if (tag.toLowerCase().contains(query)) {
                    match = true;
                    break;
                }
            }

            pm.marker.setVisible(match);
        }
    }


    /* ============================================================
       LOCATION ENABLE
       ============================================================ */
    private void enableLocationAndZoom() {

        boolean fine = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        boolean coarse = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return; // This is fine because the launcher will call this again
        }

        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 16f));
            } else {
                Log.e("LOCATION", "Location returned null");
            }
        });
    }




    /* ============================================================
       FIRESTORE FETCH + MARKERS
       ============================================================ */
    private void professionalsGeo() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .get()
                .addOnSuccessListener(query -> {

                    for (var doc : query.getDocuments()) {

                        if (!"professional".equals(doc.getString("role"))) continue;

                        GeoPoint geo = doc.getGeoPoint("geo");

                        // 🔥  GET TAGS
                        List<String> rawTags = (List<String>) doc.get("tags");
                        if (rawTags == null) rawTags = new ArrayList<>();

                        final List<String> tags = new ArrayList<>(rawTags); // ← make final copy


                        if (geo == null) continue;

                        LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude());
                        String name = doc.getString("name");
                        String profession = doc.getString("profession");
                        String profileUrl = doc.getString("profilepic");

                        Glide.with(requireActivity())
                                .asBitmap()
                                .load(profileUrl)
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                                        Marker m = googleMap.addMarker(
                                                new MarkerOptions()
                                                        .position(pos)
                                                        .title(name)
                                                        .icon(BitmapDescriptorFactory.fromBitmap(
                                                                createBitmapFromView(getMarkerView(name, profession, bitmap))
                                                        ))
                                                        .anchor(0.5f, 1f)
                                        );

                                        if (m != null) {
                                            m.setTag(doc.getId());
                                            proMarkers.add(new ProMarker(m, tags)); // ⭐ Save tags
                                        }
                                    }

                                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                                });
                    }
                });
    }


    /* ============================================================
       MARKER POPUP
       ============================================================ */
    private void markerPopup(String proId, String proName, LatLng position) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.marker_menu, null);
        dialog.setContentView(view);

        TextView name = view.findViewById(R.id.proName);
        Button btnProfile = view.findViewById(R.id.btnViewProfile);
        Button btnDirections = view.findViewById(R.id.btnDirections);

        name.setText(proName);

        btnProfile.setOnClickListener(v -> {
            dialog.dismiss();
            openPublicProfile(proId);
        });

        btnDirections.setOnClickListener(v -> {
            dialog.dismiss();
            DirectionsHelper.openExternalGoogleMaps(requireContext(), position, proName);
        });

        dialog.show();
    }


    private void openPublicProfile(String proId) {
        Fragment fragment = new PublicProfileFragment();
        Bundle args = new Bundle();
        args.putString("professionalId", proId);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.appMainView, fragment)
                .addToBackStack(null)
                .commit();
    }


    /* ============================================================
       MARKER VIEW UTILITIES
       ============================================================ */
    private View getMarkerView(String name, String profession, Bitmap img) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.marker_professional, null);

        ImageView pfp = view.findViewById(R.id.markerImage);
        TextView tvName = view.findViewById(R.id.markerName);
        TextView tvProfession = view.findViewById(R.id.markerProfession);

        pfp.setImageBitmap(img);
        tvName.setText(name);
        tvProfession.setText(profession);

        return view;
    }

    private Bitmap createBitmapFromView(View view) {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }


    /* ============================================================
       PRO MARKER MODEL
       ============================================================ */
    public static class ProMarker {
        public Marker marker;
        public List<String> tags;

        public ProMarker(Marker marker, List<String> tags) {
            this.marker = marker;
            this.tags = tags;
        }
    }



}
