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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
import com.example.thechair.BuildConfig;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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

    private static final float DEFAULT_RADIUS_KM = 1f;

    private MapView mapView;
    private GoogleMap googleMap;

    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;

    // Radius bubble
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

    // All loaded professional markers
    private final List<ProMarker> proMarkers = new ArrayList<>();

    // Permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted && googleMap != null) {
                    enableLocationAndZoom();
                }
            });

    public NearbyFragment() { }

    public static NearbyFragment newInstance(String param1, String param2) {
        NearbyFragment fragment = new NearbyFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        // MAP
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // SEARCH FAB + CONTAINER
        searchBar = view.findViewById(R.id.serviceSearchBar);
        ImageView searchFab = view.findViewById(R.id.searchFab);
        LinearLayout searchContainer = view.findViewById(R.id.searchContainer);

        if (searchFab != null && searchContainer != null && searchBar != null) {
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

            view.setOnTouchListener((v, event) -> {
                if(searchBar.hasFocus()) {
                    searchBar.clearFocus();

                }
                return false;
            });

            setupSearchFiltering();
        }

        // FILTER BOTTOM SHEET
        MaterialCardView filterSheet = view.findViewById(R.id.filterSheet);
        if (filterSheet != null) {
            BottomSheetBehavior<MaterialCardView> sheetBehavior =
                    BottomSheetBehavior.from(filterSheet);

            ImageView btnOpenFilters = view.findViewById(R.id.btnOpenFilters);
            Button btnCloseSheet = view.findViewById(R.id.btnCloseSheet);

            radiusSeekBar = view.findViewById(R.id.radiusSeekBar);
            radiusLabel = view.findViewById(R.id.radiusLabel);
            switchShowAll = view.findViewById(R.id.switchShowAll);
            nearbyListButton = view.findViewById(R.id.btnNearbyList);

            // Start hidden
            sheetBehavior.setHideable(true);
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            if (btnOpenFilters != null) {
                btnOpenFilters.setOnClickListener(v ->
                        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));
            }

            if (btnCloseSheet != null) {
                btnCloseSheet.setOnClickListener(v ->
                        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
            }

            setupRadiusControls();
            setupShowAllToggle();
            setupNearbyListButton();
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Zoom controls on the side + basic UI
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setPadding(0,0,0,187);

        // Location permission check
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() == null) return false;

            String proId = marker.getTag().toString();
            String proName = marker.getTitle();
            LatLng position = marker.getPosition();

            markerPopup(proId, proName, position);
            return true; // consume event
        });

        enableLocationAndZoom();
        professionalsGeo();
    }


    private int getNavHeight() {
        int res = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return res > 0 ? getResources().getDimensionPixelSize(res) : 180;
    }

    // =====================
    //   LIFECYCLE
    // =====================
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

    // =====================
    //   LOCATION + RADIUS
    // =====================
    private void enableLocationAndZoom() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGpsOn) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
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
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 14f));

                updateRadiusCircle();
                updateDistancesAndVisibility();
            }
        });
    }

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

    // =====================
    //   SEARCH + FILTER UI
    // =====================
    private void setupSearchFiltering() {
        if (searchBar == null) return;

        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                updateMarkerVisibility();
            }

            @Override public void afterTextChanged(android.text.Editable s) { }
        });
    }

    private void setupRadiusControls() {
        if (radiusSeekBar == null || radiusLabel == null) return;

        radiusSeekBar.setMax(50);
        radiusSeekBar.setProgress((int) DEFAULT_RADIUS_KM);
        updateRadiusLabel();

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                currentRadiusKm = Math.max(1, progress);
                updateRadiusLabel();
                updateRadiusCircle();
                updateDistancesAndVisibility();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
    }

    private void updateRadiusLabel() {
        if (radiusLabel != null) {
            radiusLabel.setText("Within " + (int) currentRadiusKm + " km");
        }
    }

    private void setupShowAllToggle() {
        if (switchShowAll == null) return;

        switchShowAll.setChecked(false);
        switchShowAll.setOnCheckedChangeListener((b, checked) -> {
            // If "Show all" is ON → ignore radius filter
            useRadiusFilter = !checked;
            updateMarkerVisibility();
        });
    }

    private void setupNearbyListButton() {
        if (nearbyListButton == null) return;

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
                    .append(pm.distanceKm >= 0
                            ? String.format(" (%.1f km)", pm.distanceKm)
                            : "")
                    .append("\n");
        }

        if (sb.length() == 0) sb.append("No professionals in this radius.");

        new AlertDialog.Builder(requireContext())
                .setTitle("Nearby Professionals")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateMarkerVisibility() {
        String q = currentQuery == null ? "" : currentQuery.toLowerCase().trim();

        for (ProMarker pm : proMarkers) {

            String nameSafe = pm.name == null ? "" : pm.name.toLowerCase();
            String professionSafe = pm.profession == null ? "" : pm.profession.toLowerCase();

            boolean matchesName = nameSafe.contains(q);
            boolean matchesProfession = professionSafe.contains(q);

            // Services — already stored lowercase
            boolean matchesService = false;
            for (String s : pm.serviceNames) {
                if (s.contains(q)) {
                    matchesService = true;
                    break;
                }
            }

            // Tags — already stored lowercase
            boolean matchesTag = false;
            for (String t : pm.tags) {
                if (t.contains(q)) {
                    matchesTag = true;
                    break;
                }
            }

            // FINAL match result:
            boolean matches = q.isEmpty()
                    || matchesName
                    || matchesProfession
                    || matchesService
                    || matchesTag;

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

    // =====================
    //   GEOCODING (GOOGLE API)
    // =====================
    private void geocodeWithGoogleAPI(String fullAddress, Consumer<LatLng> callback) {

        OkHttpClient client = new OkHttpClient();

        String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                + URLEncoder.encode(fullAddress, StandardCharsets.UTF_8)
                + "&key=" + BuildConfig.MAPS_API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GEOCODER_API", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("GEOCODER_API", "HTTP error: " + response.code());
                    return;
                }

                String body = response.body().string();
                Log.d("GEOCODER_RAW", body);

                try {
                    JSONObject json = new JSONObject(body);
                    String status = json.optString("status");
                    Log.d("GEOCODER_STATUS", "Status = " + status);

                    if (!"OK".equals(status)) {
                        Log.e("GEOCODER_API", "API Error: " + status);
                        return;
                    }

                    JSONArray results = json.getJSONArray("results");
                    Log.d("GEOCODER_RESULTS", "Results length = " + results.length());

                    if (results.length() > 0) {
                        JSONObject location = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location");

                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");

                        LatLng latLng = new LatLng(lat, lng);

                        new Handler(Looper.getMainLooper())
                                .post(() -> callback.accept(latLng));
                    } else {
                        Log.e("GEOCODER_API", "No results for: " + fullAddress);
                    }

                } catch (Exception e) {
                    Log.e("GEOCODER_API", "JSON parse error: " + e.getMessage());
                }
            }
        });
    }

    // =====================
    //   FETCH PROFESSIONALS + AUTO-GEOCODE
    // =====================
    private void professionalsGeo() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) return;
                    if (!isAdded()) return;

                    for (var doc : query.getDocuments()) {

                        if (!"professional".equals(doc.getString("role"))) continue;

                        GeoPoint geo = doc.getGeoPoint("geo");

                        boolean needsGeocoding =
                                geo == null
                                        || Math.abs(geo.getLatitude()) < 0.0001
                                        || Math.abs(geo.getLongitude()) < 0.0001;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> addressMap = (Map<String, Object>) doc.get("address");

                        if (addressMap == null) {
                            Log.e("ADDRESS", "User " + doc.getId() + " has no address map.");
                            // if we do have valid geo somehow, still show them
                            if (!needsGeocoding && geo != null) {
                                LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude());
                                addMarkerForDoc(doc, pos);
                            }
                            continue;
                        }

                        String street  = (String) addressMap.get("street");
                        String apt     = (String) addressMap.get("room");
                        String city    = (String) addressMap.get("city");
                        String state   = (String) addressMap.get("province");
                        String postal  = (String) addressMap.get("postalCode");
                        String country = (String) addressMap.get("country");

                        if (street == null || city == null || state == null || postal == null) {
                            Log.e("ADDRESS", "Skipping user " + doc.getId() + " — missing address parts");
                            if (!needsGeocoding && geo != null) {
                                LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude());
                                addMarkerForDoc(doc, pos);
                            }
                            continue;
                        }

                        // Normalize street name
                        street = street
                                .replace("Ave.", "Avenue")
                                .replace("Ave", "Avenue")
                                .replace("ave.", "Avenue")
                                .replace("ave", "Avenue");

                        // Normalize postal code
                        if (postal.length() == 6) {
                            postal = postal.substring(0, 3) + " " + postal.substring(3);
                        }

                        if (country == null || country.trim().isEmpty()) {
                            country = "Canada";
                        }

                        String fullAddress = street
                                + (apt != null && !apt.isEmpty() ? " " + apt : "")
                                + ", " + city
                                + ", " + state + " " + postal
                                + ", " + country;

                        if (needsGeocoding) {
                            Log.d("GEOCODE_REQUEST", "Geocoding: " + fullAddress);

                            geocodeWithGoogleAPI(fullAddress, latLng -> {
                                if (!isAdded() || googleMap == null) return;

                                Log.d("GEOCODE_RESPONSE", doc.getString("name") +
                                        " → " + latLng.latitude + ", " + latLng.longitude);

                                db.collection("Users")
                                        .document(doc.getId())
                                        .update("geo", new GeoPoint(latLng.latitude, latLng.longitude));

                                addMarkerForDoc(doc, latLng);
                            });

                        } else {
                            LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude());
                            addMarkerForDoc(doc, pos);
                        }
                    }
                });
    }

    private void addMarkerForDoc(com.google.firebase.firestore.DocumentSnapshot doc, LatLng pos) {
        if (!isAdded() || googleMap == null) return;

        String name = doc.getString("name");
        String profession = doc.getString("profession");
        String profileUrl = doc.getString("profilepic");
        String id = doc.getId();

        // ----------------------------------------------------
        //  SERVICES  (build → freeze into final)
        // ----------------------------------------------------
        List<String> serviceNamesTmp = new ArrayList<>();
        List<Map<String, Object>> servicesList =
                (List<Map<String, Object>>) doc.get("services");

        if (servicesList != null) {
            for (Map<String, Object> srv : servicesList) {
                String s = (String) srv.get("name");
                if (s != null) serviceNamesTmp.add(s.toLowerCase());
            }
        }

        final List<String> serviceNames = serviceNamesTmp;   // FINAL ✔


        // ----------------------------------------------------
        //  TAGS  (normalize → freeze into final)
        // ----------------------------------------------------
        List<String> tagsTmp = new ArrayList<>();
        List<String> rawTags = (List<String>) doc.get("tags");

        if (rawTags != null) {
            for (String t : rawTags) {
                if (t != null) tagsTmp.add(t.toLowerCase());
            }
        }

        final List<String> tags = tagsTmp;   // FINAL ✔


        // ----------------------------------------------------
        //  GLIDE CALLBACK
        // ----------------------------------------------------
        Glide.with(requireActivity())
                .asBitmap()
                .load(profileUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {

                        if (!isAdded() || googleMap == null) return;

                        View markerView = getMarkerView(name, profession, resource);
                        Bitmap markerBitmap = createBitmapFromView(markerView);

                        Marker m = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(pos)
                                        .title(name)
                                        .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                                        .anchor(0.5f, 1.0f)
                        );

                        if (m != null) {
                            m.setTag(id);

                            double dist = -1;
                            if (lastKnownLocation != null) {
                                LatLng user = new LatLng(
                                        lastKnownLocation.getLatitude(),
                                        lastKnownLocation.getLongitude()
                                );
                                dist = distanceBetweenKm(user, pos);
                            }

                            // Now safe because tags + serviceNames are FINAL
                            proMarkers.add(new ProMarker(
                                    m, id, name, profession, pos, dist,
                                    serviceNames, tags
                            ));
                        }

                        updateMarkerVisibility();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }


    // =====================
    //   UI Helpers
    // =====================
    private View getMarkerView(String name, String profession, Bitmap profileBitmap) {
        View markerView = LayoutInflater.from(getContext())
                .inflate(R.layout.marker_professional, null);

        ImageView image = markerView.findViewById(R.id.markerImage);
        TextView tvName = markerView.findViewById(R.id.markerName);
        TextView tvProfession = markerView.findViewById(R.id.markerProfession);

        image.setImageBitmap(profileBitmap);
        tvName.setText(name);
        tvProfession.setText(profession);

        return markerView;
    }

    private Bitmap createBitmapFromView(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
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

    private void markerPopup(String proId, String proName, LatLng position) {
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);

        View view = getLayoutInflater().inflate(R.layout.marker_menu, null);
        dialog.setContentView(view);

        TextView proNameTextView = view.findViewById(R.id.proName);
        Button btnViewProfile = view.findViewById(R.id.btnViewProfile);
        Button btnDirections = view.findViewById(R.id.btnDirections);

        proNameTextView.setText(proName);

        btnViewProfile.setOnClickListener(v -> {
            dialog.dismiss();
            openPublicProfile(proId);
        });

        btnDirections.setOnClickListener(v -> {
            dialog.dismiss();
            DirectionsHelper.openExternalGoogleMaps(requireContext(), position, proName);
        });

        dialog.show();
    }

    // =====================
    //   MODEL
    // =====================
    public static class ProMarker {
        public Marker marker;
        public String id;
        public String name;
        public String profession;
        public LatLng position;
        public double distanceKm;

        public List<String> serviceNames;   // NEW
        public List<String> tags;           // NEW

        public ProMarker(Marker m, String id, String nm, String prof,
                         LatLng pos, double dist,
                         List<String> serviceNames,
                         List<String> tags) {

            this.marker = m;
            this.id = id;
            this.name = nm;
            this.profession = prof;
            this.position = pos;
            this.distanceKm = dist;
            this.serviceNames = serviceNames != null ? serviceNames : new ArrayList<>();
            this.tags = tags != null ? tags : new ArrayList<>();
        }
    }

}
