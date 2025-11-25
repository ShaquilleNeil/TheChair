// Shaq’s Notes:
// This is the whole “show pros on the map” machine—location grabbing, custom pin
// rendering, geocoding fallback, radius filtering, bottom sheet filters, search,
// category matching, and dynamic marker visibility. It’s your Google-Maps-powered
// talent radar.
//
// The structure is sane: lifecycle forwarding, permission handling, lazy geocoding,
// marker metadata, and bitmap-icon generation. Nothing here is wasted—every method
// contributes to tying Firestore pros into map pins that breathe and filter in real time.

package com.example.thechair.Customer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
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

    // Search input
    private EditText searchBar;
    private String currentQuery = "";

    // All markers we manage
    private final List<ProMarker> proMarkers = new ArrayList<>();

    // Permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted && googleMap != null) enableLocationAndZoom();
            });

    public NearbyFragment() {}

    public static NearbyFragment newInstance(String p1, String p2) {
        NearbyFragment f = new NearbyFragment();
        Bundle args = new Bundle();
        args.putString("param1", p1);
        args.putString("param2", p2);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle s) {

        View v = inf.inflate(R.layout.fragment_nearby, parent, false);

        // Map
        mapView = v.findViewById(R.id.mapView);
        mapView.onCreate(s);
        mapView.getMapAsync(this);

        // Search reveal control
        searchBar = v.findViewById(R.id.serviceSearchBar);
        ImageView searchFab = v.findViewById(R.id.searchFab);
        LinearLayout searchContainer = v.findViewById(R.id.searchContainer);

        if (searchFab != null && searchContainer != null && searchBar != null) {
            searchFab.setOnClickListener(btn -> {
                searchFab.setVisibility(View.GONE);
                searchContainer.setVisibility(View.VISIBLE);
                searchBar.requestFocus();
            });

            searchBar.setOnFocusChangeListener((vv, focus) -> {
                if (!focus && searchBar.getText().toString().trim().isEmpty()) {
                    searchContainer.setVisibility(View.GONE);
                    searchFab.setVisibility(View.VISIBLE);
                }
            });

            v.setOnTouchListener((xx, yy) -> {
                if (searchBar.hasFocus()) searchBar.clearFocus();
                return false;
            });

            setupSearchFiltering();
        }

        // Filter sheet
        MaterialCardView filterSheet = v.findViewById(R.id.filterSheet);
        if (filterSheet != null) {
            BottomSheetBehavior<MaterialCardView> sheet =
                    BottomSheetBehavior.from(filterSheet);

            ImageView btnOpen = v.findViewById(R.id.btnOpenFilters);
            Button btnClose = v.findViewById(R.id.btnCloseSheet);

            radiusSeekBar = v.findViewById(R.id.radiusSeekBar);
            radiusLabel = v.findViewById(R.id.radiusLabel);
            switchShowAll = v.findViewById(R.id.switchShowAll);
            nearbyListButton = v.findViewById(R.id.btnNearbyList);

            sheet.setHideable(true);
            sheet.setState(BottomSheetBehavior.STATE_HIDDEN);

            if (btnOpen != null)
                btnOpen.setOnClickListener(xx -> sheet.setState(BottomSheetBehavior.STATE_EXPANDED));

            if (btnClose != null)
                btnClose.setOnClickListener(xx -> sheet.setState(BottomSheetBehavior.STATE_HIDDEN));

            setupRadiusControls();
            setupShowAllToggle();
            setupNearbyListButton();
        }

        return v;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap gm) {
        googleMap = gm;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setPadding(0,0,0,187);

        // Fine location
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        // Marker click → bottom sheet
        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() == null) return false;
            markerPopup(marker.getTag().toString(), marker.getTitle(), marker.getPosition());
            return true;
        });

        // Keep filtering consistent
        googleMap.setOnCameraIdleListener(this::updateMarkerVisibility);

        // Info windows (custom inner layout)
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override public View getInfoWindow(@NonNull Marker m) { return null; }

            @Override public View getInfoContents(@NonNull Marker m) {
                View w = LayoutInflater.from(getContext())
                        .inflate(R.layout.info_window_professional, null);

                ImageView img = w.findViewById(R.id.infoImage);
                TextView name = w.findViewById(R.id.infoName);
                TextView prof = w.findViewById(R.id.infoProfession);

                ProMarker found = null;
                for (ProMarker p : proMarkers)
                    if (p.id.equals(m.getTag())) { found = p; break; }

                if (found != null) {
                    img.setImageBitmap(found.profilebitmap);
                    name.setText(found.name);
                    prof.setText(found.profession);
                }
                return w;
            }
        });

        enableLocationAndZoom();
        professionalsGeo();
    }

    private int getNavHeight() {
        int res = getResources().getIdentifier("navigation_bar_height",
                "dimen", "android");
        return res > 0 ? getResources().getDimensionPixelSize(res) : 180;
    }

    // Map lifecycle
    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause()  { if (mapView != null) mapView.onPause();  super.onPause(); }
    @Override public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }

    // Location enable + camera
    private void enableLocationAndZoom() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        LocationManager lm = (LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

        try { googleMap.setMyLocationEnabled(true); } catch (Exception ignored) {}

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener(loc -> {
            if (loc != null) {
                lastKnownLocation = loc;
                LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14f));
                updateRadiusCircle();
                updateDistancesAndVisibility();
            }
        });
    }

    private void updateRadiusCircle() {
        if (googleMap == null || lastKnownLocation == null) return;

        LatLng c = new LatLng(
                lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude()
        );

        double m = currentRadiusKm * 1000;

        if (radiusCircle == null) {
            radiusCircle = googleMap.addCircle(
                    new CircleOptions()
                            .center(c)
                            .radius(m)
                            .strokeWidth(2f)
                            .strokeColor(0x55FFFFFF)
                            .fillColor(0x2200FFFF)
            );
        } else {
            radiusCircle.setCenter(c);
            radiusCircle.setRadius(m);
        }
    }

    private double distanceBetweenKm(LatLng a, LatLng b) {
        float[] out = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out);
        return out[0] / 1000.0;
    }

    private void updateDistancesAndVisibility() {
        if (lastKnownLocation == null) return;

        LatLng me = new LatLng(lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude());

        for (ProMarker pm : proMarkers)
            pm.distanceKm = distanceBetweenKm(me, pm.position);

        updateMarkerVisibility();
    }

    private void setupSearchFiltering() {
        if (searchBar == null) return;

        searchBar.addTextChangedListener(
                new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence c, int s, int c1, int c2) {}
                    @Override public void onTextChanged(CharSequence c, int s, int b, int c2) {
                        currentQuery = c.toString();
                        updateMarkerVisibility();
                    }
                    @Override public void afterTextChanged(android.text.Editable e) {}
                }
        );
    }

    private void setupRadiusControls() {
        if (radiusSeekBar == null || radiusLabel == null) return;

        radiusSeekBar.setMax(50);
        radiusSeekBar.setProgress((int) DEFAULT_RADIUS_KM);
        updateRadiusLabel();

        radiusSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                        currentRadiusKm = Math.max(1, p);
                        updateRadiusLabel();
                        updateRadiusCircle();
                        updateDistancesAndVisibility();
                    }
                    @Override public void onStartTrackingTouch(SeekBar sb) {}
                    @Override public void onStopTrackingTouch(SeekBar sb) {}
                }
        );
    }

    private void updateRadiusLabel() {
        if (radiusLabel != null)
            radiusLabel.setText("Within " + (int) currentRadiusKm + " km");
    }

    private void setupShowAllToggle() {
        if (switchShowAll == null) return;

        switchShowAll.setChecked(false);
        switchShowAll.setOnCheckedChangeListener((btn, checked) -> {
            useRadiusFilter = !checked;
            updateMarkerVisibility();
        });
    }

    private void setupNearbyListButton() {
        if (nearbyListButton == null) return;

        nearbyListButton.setOnClickListener(xx ->
                showNearbyListDialog()
        );
    }

    private void showNearbyListDialog() {
        if (proMarkers.isEmpty()) {
            Toast.makeText(requireContext(), "No professionals loaded yet",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ProMarker pm : proMarkers) {
            if (!shouldShowMarker(pm)) continue;

            sb.append(pm.name)
                    .append(" – ").append(pm.profession)
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

            String n = pm.name == null ? "" : pm.name.toLowerCase();
            String p = pm.profession == null ? "" : pm.profession.toLowerCase();

            boolean matchName = n.contains(q);
            boolean matchProfession = p.contains(q);

            boolean matchService = false;
            for (String s : pm.serviceNames)
                if (s.contains(q)) { matchService = true; break; }

            boolean matchTag = false;
            for (String t : pm.tags)
                if (t.contains(q)) { matchTag = true; break; }

            boolean finalMatch = q.isEmpty()
                    || matchName
                    || matchProfession
                    || matchService
                    || matchTag;

            boolean within = true;
            if (useRadiusFilter && pm.distanceKm >= 0)
                within = pm.distanceKm <= currentRadiusKm;

            pm.marker.setVisible(finalMatch && within);
        }
    }

    private boolean shouldShowMarker(ProMarker pm) {
        boolean within = !useRadiusFilter || pm.distanceKm <= currentRadiusKm;
        boolean match = currentQuery.trim().isEmpty()
                || pm.name.toLowerCase().contains(currentQuery.toLowerCase())
                || pm.profession.toLowerCase().contains(currentQuery.toLowerCase());
        return match && within;
    }

    // Geocoding via Google API
    private void geocodeWithGoogleAPI(String fullAddress, Consumer<LatLng> cb) {

        OkHttpClient client = new OkHttpClient();

        String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                + URLEncoder.encode(fullAddress, StandardCharsets.UTF_8)
                + "&key=" + BuildConfig.MAPS_API_KEY;

        Request req = new Request.Builder().url(url).build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                Log.e("GEOCODER_API", "Fail: " + e.getMessage());
            }

            @Override public void onResponse(@NonNull Call c, @NonNull Response r)
                    throws IOException {

                if (!r.isSuccessful()) {
                    Log.e("GEOCODER_API", "HTTP err: " + r.code());
                    return;
                }

                String body = r.body().string();
                Log.d("GEOCODER_RAW", body);

                try {
                    JSONObject json = new JSONObject(body);
                    String status = json.optString("status");

                    if (!"OK".equals(status)) {
                        Log.e("GEOCODER_API", "API Err: " + status);
                        return;
                    }

                    JSONArray results = json.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject loc = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location");

                        LatLng ll = new LatLng(
                                loc.getDouble("lat"),
                                loc.getDouble("lng")
                        );

                        new Handler(Looper.getMainLooper())
                                .post(() -> cb.accept(ll));
                    }
                } catch (Exception e) {
                    Log.e("GEOCODER_API", "JSON Err: " + e.getMessage());
                }
            }
        });
    }

    // Fetch all users → drop professionals onto the map (with geocoding fallback)
    private void professionalsGeo() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty() || !isAdded()) return;

                    for (var doc : query.getDocuments()) {

                        if (!"professional".equals(doc.getString("role"))) continue;

                        GeoPoint geo = doc.getGeoPoint("geo");

                        boolean badGeo =
                                geo == null
                                        || Math.abs(geo.getLatitude()) < 0.0001
                                        || Math.abs(geo.getLongitude()) < 0.0001;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> addr =
                                (Map<String, Object>) doc.get("address");

                        // Skip — missing location info
                        if (addr == null) {
                            if (!badGeo && geo != null)
                                addMarkerForDoc(doc, new LatLng(
                                        geo.getLatitude(),
                                        geo.getLongitude()
                                ));
                            continue;
                        }

                        String street = (String) addr.get("street");
                        String apt    = (String) addr.get("room");
                        String city   = (String) addr.get("city");
                        String state  = (String) addr.get("province");
                        String postal = (String) addr.get("postalCode");
                        String country= (String) addr.get("country");

                        if (street == null || city == null || state == null || postal == null) {
                            if (!badGeo && geo != null)
                                addMarkerForDoc(doc, new LatLng(
                                        geo.getLatitude(),
                                        geo.getLongitude()
                                ));
                            continue;
                        }

                        // Normalize address bits
                        street = street
                                .replace("Ave.", "Avenue")
                                .replace("Ave",  "Avenue")
                                .replace("ave.", "Avenue")
                                .replace("ave",  "Avenue");

                        if (postal.length() == 6)
                            postal = postal.substring(0,3)+" "+postal.substring(3);

                        if (country == null || country.isEmpty())
                            country = "Canada";

                        String full = street
                                + (apt != null && !apt.isEmpty() ? " "+apt : "")
                                + ", " + city
                                + ", " + state + " " + postal
                                + ", " + country;

                        if (badGeo) {
                            geocodeWithGoogleAPI(full, ll -> {
                                if (!isAdded() || googleMap == null) return;

                                db.collection("Users")
                                        .document(doc.getId())
                                        .update("geo", new GeoPoint(ll.latitude, ll.longitude));

                                addMarkerForDoc(doc, ll);
                            });
                        } else {
                            LatLng ll = new LatLng(
                                    geo.getLatitude(),
                                    geo.getLongitude()
                            );
                            addMarkerForDoc(doc, ll);
                        }
                    }
                });
    }

    private void addMarkerForDoc(com.google.firebase.firestore.DocumentSnapshot doc,
                                 LatLng pos) {

        if (!isAdded() || googleMap == null) return;

        String name = doc.getString("name");
        String prof = doc.getString("profession");
        String pic  = doc.getString("profilepic");
        String id   = doc.getId();

        // Build services list lowercased
        List<String> serviceNamesTmp = new ArrayList<>();
        List<Map<String,Object>> srvList = (List<Map<String,Object>>) doc.get("services");
        if (srvList != null)
            for (Map<String,Object> s : srvList) {
                String nm = (String) s.get("name");
                if (nm != null) serviceNamesTmp.add(nm.toLowerCase());
            }
        final List<String> serviceNames = serviceNamesTmp;

        // Build tags list lowercased
        List<String> tagsTmp = new ArrayList<>();
        List<String> rawTags = (List<String>) doc.get("tags");
        if (rawTags != null)
            for (String t : rawTags)
                if (t != null) tagsTmp.add(t.toLowerCase());

        final List<String> tags = tagsTmp;

        // Load bitmap + generate pin
        Glide.with(requireActivity())
                .asBitmap()
                .load(pic)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bm,
                                                @Nullable Transition<? super Bitmap> tr) {

                        if (!isAdded() || googleMap == null) return;

                        Bitmap finalMarker = createMarkerBitmap(bm);

                        Marker m = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(pos)
                                        .title(name)
                                        .snippet(prof)
                                        .icon(BitmapDescriptorFactory.fromBitmap(finalMarker))
                                        .anchor(0.5f, 1f)
                        );

                        if (m != null) {
                            m.setTag(id);

                            double dist = -1;
                            if (lastKnownLocation != null) {
                                LatLng me = new LatLng(
                                        lastKnownLocation.getLatitude(),
                                        lastKnownLocation.getLongitude()
                                );
                                dist = distanceBetweenKm(me, pos);
                            }

                            proMarkers.add(new ProMarker(
                                    m, id, name, prof, pos, dist,
                                    serviceNames, tags, bm
                            ));
                        }

                        updateMarkerVisibility();
                    }

                    @Override public void onLoadCleared(@Nullable Drawable d) {}
                });
    }

    // Pixel helper
    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem()
                .getDisplayMetrics().density);
    }

    private void openPublicProfile(String proId) {
        Fragment f = new PublicProfileFragment();
        Bundle b = new Bundle();
        b.putString("professionalId", proId);
        f.setArguments(b);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.appMainView, f)
                .addToBackStack(null)
                .commit();
    }

    // Oval crop
    private Bitmap circleCrop(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        int inset = (int) (size * 0.08f);

        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        float cx = size / 2f;
        float cy = size / 2f;
        float rx = size / 2f - inset;
        float ry = (size / 2f - inset) * 0.88f;

        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, p);

        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect srcRect = new Rect(0, 0, src.getWidth(), src.getHeight());
        Rect dstRect = new Rect(inset, inset, size - inset, size - inset);

        c.drawBitmap(src, srcRect, dstRect, p);
        return out;
    }

    // Create final pin bitmap
    private Bitmap createMarkerBitmap(Bitmap head) {
        int w = 120;
        int h = 150;

        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);

        Drawable pin = getResources().getDrawable(R.drawable.map_pin);
        pin.setBounds(0, 0, w, h);
        pin.draw(c);

        Bitmap face = circleCrop(head);

        int size = 68;
        int left = (w - size) / 2;
        int top = 24;

        c.drawBitmap(
                Bitmap.createScaledBitmap(face, size, size, true),
                left, top, null
        );

        return bm;
    }

    private void markerPopup(String id, String name, LatLng pos) {
        BottomSheetDialog d =
                new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);

        View v = getLayoutInflater().inflate(R.layout.marker_menu, null);
        d.setContentView(v);

        TextView nameTV = v.findViewById(R.id.proName);
        TextView professionTV = v.findViewById(R.id.proProfession);
        ImageView proImage = v.findViewById(R.id.proImage);
        Button bProfile = v.findViewById(R.id.btnViewProfile);
        Button bDirections = v.findViewById(R.id.btnDirections);

        nameTV.setText(name);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .document(id)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    String profession = doc.getString("profession");
                    String pic = doc.getString("profilepic");

                    // Set profession
                    professionTV.setText(profession);

                    // Set image
                    Glide.with(requireContext())
                            .load(pic)
                            .placeholder(R.drawable.ic_person)
                            .into(proImage);
                });

        bProfile.setOnClickListener(xx -> {
            d.dismiss();
            openPublicProfile(id);
        });

        bDirections.setOnClickListener(xx -> {
            d.dismiss();
            DirectionsHelper.openExternalGoogleMaps(requireContext(), pos, name);
        });

        d.show();
    }

    // Model for markers we manage
    public static class ProMarker {
        public Marker marker;
        public String id;
        public String name;
        public String profession;
        public LatLng position;
        public double distanceKm;

        public List<String> serviceNames;
        public List<String> tags;
        public Bitmap profilebitmap;

        public ProMarker(Marker m, String id, String nm, String prof,
                         LatLng pos, double dist,
                         List<String> serviceNames,
                         List<String> tags, Bitmap profileBitmap) {

            this.marker = m;
            this.id = id;
            this.name = nm;
            this.profession = prof;
            this.position = pos;
            this.distanceKm = dist;

            this.serviceNames = serviceNames != null ? serviceNames : new ArrayList<>();
            this.tags = tags != null ? tags : new ArrayList<>();

            this.profilebitmap = profileBitmap;
        }
    }
}
