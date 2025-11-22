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
import android.os.Bundle;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.thechair.Adapters.DirectionsHelper;
import com.example.thechair.Professional.PublicProfileFragment;
import com.example.thechair.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class NearbyFragment extends Fragment implements OnMapReadyCallback {

    private static final float DEFAULT_RADIUS_KM = 1f;

    private MapView mapView;
    private GoogleMap googleMap;
    private final List<ProMarker> proMarkers = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;

    private Circle radiusCircle;
    private float currentRadiusKm = DEFAULT_RADIUS_KM;
    private boolean useRadiusFilter = true;

    // Bottom sheet controls
    private SeekBar radiusSeekBar;
    private TextView radiusLabel;
    private SwitchCompat switchShowAll;
    private Button nearbyListButton;

    // Search
    private EditText searchBar;
    private String currentQuery = "";

    // Permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted && googleMap != null) enableLocationAndZoom();
            });


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // MAP
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // SEARCH FAB
        searchBar = view.findViewById(R.id.serviceSearchBar);
        ImageView searchFab = view.findViewById(R.id.searchFab);
        LinearLayout searchContainer = view.findViewById(R.id.searchContainer);

        searchFab.setOnClickListener(v -> {
            searchFab.setVisibility(View.GONE);
            searchContainer.setVisibility(View.VISIBLE);
            searchBar.requestFocus();
        });

        searchBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && searchBar.getText().toString().trim().isEmpty()) {
                searchContainer.setVisibility(View.GONE);
                searchFab.setVisibility(View.VISIBLE);
            }
        });

        setupSearchFiltering();

        // FILTER SHEET SETUP
        MaterialCardView filterSheet = view.findViewById(R.id.filterSheet);
        BottomSheetBehavior<MaterialCardView> sheetBehavior =
                BottomSheetBehavior.from(filterSheet);

        ImageView btnOpenFilters = view.findViewById(R.id.btnOpenFilters);
        btnOpenFilters.setOnClickListener(v ->
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));

        Button btnCloseSheet = view.findViewById(R.id.btnCloseSheet);
        btnCloseSheet.setOnClickListener(v ->
                sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));

        // Bottom sheet controls
        radiusSeekBar = view.findViewById(R.id.radiusSeekBar);
        radiusLabel = view.findViewById(R.id.radiusLabel);
        switchShowAll = view.findViewById(R.id.switchShowAll);
        nearbyListButton = view.findViewById(R.id.btnNearbyList);

        // Start hidden
        sheetBehavior.setHideable(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        setupRadiusControls();
        setupShowAllToggle();
        setupNearbyListButton();

        return view;
    }

    // MAP READY
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() == null) return false;

            markerPopup(
                    marker.getTag().toString(),
                    marker.getTitle(),
                    marker.getPosition()
            );
            return true;
        });

        enableLocationAndZoom();
        professionalsGeo();
    }

    private void enableLocationAndZoom() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        try {
            googleMap.setMyLocationEnabled(true);
        } catch (Exception ignored) {}

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                lastKnownLocation = location;
                LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 14));

                updateRadiusCircle();
            }
        });
    }

    // SEARCH FILTER
    private void setupSearchFiltering() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                updateMarkerVisibility();
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // RADIUS CONTROLS
    private void setupRadiusControls() {
        radiusSeekBar.setMax(50);
        radiusSeekBar.setProgress(1);
        updateRadiusLabel();

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                currentRadiusKm = Math.max(1, progress);
                updateRadiusLabel();
                updateRadiusCircle();
                updateDistancesAndVisibility();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void updateRadiusLabel() {
        radiusLabel.setText("Within " + (int) currentRadiusKm + " km");
    }

    // TOGGLE "SHOW ALL"
    private void setupShowAllToggle() {
        switchShowAll.setChecked(false);
        switchShowAll.setOnCheckedChangeListener((b, checked) -> {
            useRadiusFilter = !checked;
            updateMarkerVisibility();
        });
    }

    // POPUP LIST
    private void setupNearbyListButton() {
        nearbyListButton.setOnClickListener(v -> showNearbyListDialog());
    }

    private void showNearbyListDialog() {
        if (proMarkers.isEmpty()) {
            Toast.makeText(requireContext(), "No professionals loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ProMarker pm : proMarkers) {
            if (!shouldShowMarker(pm)) continue;

            sb.append(pm.name)
                    .append(" – ")
                    .append(pm.profession)
                    .append(String.format(" (%.1f km)", pm.distanceKm))
                    .append("\n");
        }

        if (sb.length() == 0) sb.append("No professionals in this radius.");

        new AlertDialog.Builder(requireContext())
                .setTitle("Nearby Professionals")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // RADIUS CIRCLE
    private void updateRadiusCircle() {
        if (googleMap == null || lastKnownLocation == null) return;

        LatLng center = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        double meters = currentRadiusKm * 1000;

        if (radiusCircle == null) {
            radiusCircle = googleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(meters)
                    .strokeWidth(2f)
                    .strokeColor(0x55FFFFFF)
                    .fillColor(0x2200FFFF));
        } else {
            radiusCircle.setCenter(center);
            radiusCircle.setRadius(meters);
        }
    }

    private double distanceBetweenKm(LatLng a, LatLng b) {
        float[] result = new float[1];
        Location.distanceBetween(a.latitude, a.longitude,
                b.latitude, b.longitude, result);
        return result[0] / 1000.0;
    }

    private void updateDistancesAndVisibility() {
        if (lastKnownLocation == null) return;

        LatLng user = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        for (ProMarker pm : proMarkers) {
            pm.distanceKm = distanceBetweenKm(user, pm.position);
        }
        updateMarkerVisibility();
    }

    // FETCH PROS
    private void professionalsGeo() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").get().addOnSuccessListener(query -> {
            for (var doc : query) {
                if (!"professional".equals(doc.getString("role"))) continue;

                GeoPoint geo = doc.getGeoPoint("geo");
                if (geo == null) continue;

                LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude());
                String name = doc.getString("name");
                String profession = doc.getString("profession");
                String url = doc.getString("profilepic");
                String id = doc.getId();

                Glide.with(requireActivity())
                        .asBitmap()
                        .load(url)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap bmp,
                                                        @Nullable Transition<? super Bitmap> t) {

                                Marker m = googleMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title(name)
                                        .icon(BitmapDescriptorFactory.fromBitmap(
                                                createBitmapFromView(getMarkerView(name, profession, bmp))
                                        ))
                                        .anchor(0.5f, 1f)
                                );

                                if (m != null) {
                                    m.setTag(id);

                                    double dist = -1;
                                    if (lastKnownLocation != null) {
                                        LatLng user = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                        dist = distanceBetweenKm(user, pos);
                                    }

                                    proMarkers.add(new ProMarker(m, id, name, profession, pos, dist));
                                }
                                updateMarkerVisibility();
                            }

                            @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            }
        });
    }

    private void updateMarkerVisibility() {
        String q = currentQuery.toLowerCase().trim();

        for (ProMarker pm : proMarkers) {

            boolean matches = q.isEmpty() ||
                    pm.name.toLowerCase().contains(q) ||
                    pm.profession.toLowerCase().contains(q);

            boolean within = true;
            if (useRadiusFilter && pm.distanceKm >= 0) {
                within = pm.distanceKm <= currentRadiusKm;
            }

            pm.marker.setVisible(matches && within);
        }
    }

    private boolean shouldShowMarker(ProMarker pm) {
        boolean within = !useRadiusFilter || pm.distanceKm <= currentRadiusKm;
        boolean matches = currentQuery.trim().isEmpty()
                || pm.name.toLowerCase().contains(currentQuery.toLowerCase())
                || pm.profession.toLowerCase().contains(currentQuery.toLowerCase());
        return matches && within;
    }

    private void markerPopup(String id, String name, LatLng pos) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.marker_menu, null);
        dialog.setContentView(v);

        ((TextView) v.findViewById(R.id.proName)).setText(name);

        v.findViewById(R.id.btnViewProfile).setOnClickListener(x -> {
            dialog.dismiss();
            openPublicProfile(id);
        });

        v.findViewById(R.id.btnDirections).setOnClickListener(x -> {
            dialog.dismiss();
            DirectionsHelper.openExternalGoogleMaps(requireContext(), pos, name);
        });

        dialog.show();
    }

    private void openPublicProfile(String id) {
        Fragment fragment = new PublicProfileFragment();
        Bundle args = new Bundle();
        args.putString("professionalId", id);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.appMainView, fragment)
                .addToBackStack(null)
                .commit();
    }

    private View getMarkerView(String name, String profession, Bitmap bmp) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.marker_professional, null);

        ((ImageView) v.findViewById(R.id.markerImage)).setImageBitmap(bmp);
        ((TextView) v.findViewById(R.id.markerName)).setText(name);
        ((TextView) v.findViewById(R.id.markerProfession)).setText(profession);

        return v;
    }

    private Bitmap createBitmapFromView(View v) {
        v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

        Bitmap b = Bitmap.createBitmap(
                v.getMeasuredWidth(),
                v.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }

    public static class ProMarker {
        public Marker marker;
        public String id;
        public String name;
        public String profession;
        public LatLng position;
        public double distanceKm;

        public ProMarker(Marker m, String id, String nm, String prof,
                         LatLng pos, double dist) {
            this.marker = m;
            this.id = id;
            this.name = nm;
            this.profession = prof;
            this.position = pos;
            this.distanceKm = dist;
        }
    }
}
