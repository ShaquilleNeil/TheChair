package com.example.thechair.Customer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.thechair.Professional.PublicProfileFragment;
import com.example.thechair.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.thechair.BuildConfig;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NearbyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NearbyFragment extends Fragment implements OnMapReadyCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private MapView mapView;
   private GoogleMap googleMap;

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted && googleMap != null) {
                    enableLocationAndZoom();
                }
            });



    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public NearbyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NearbyFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NearbyFragment newInstance(String param1, String param2) {
        NearbyFragment fragment = new NearbyFragment();
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
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());


        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);





        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        googleMap.setOnMarkerClickListener(marker -> {

            String proId = marker.getTag().toString();

            openPublicProfile(proId);

            return true; // consume the click event
        });


        enableLocationAndZoom();

        professionalsGeo();

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


        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 16f));
            }
        });

    }

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
            public void onFailure(Call call, IOException e) {
                Log.e("GEOCODER_API", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("GEOCODER_API", "HTTP error: " + response.code());
                    return;
                }

                String body = response.body().string();

                // 🔥 CRITICAL: Log the raw response
                Log.e("GEOCODER_RAW", body);

                try {
                    JSONObject json = new JSONObject(body);

                    // 🔥 API status
                    String status = json.optString("status");
                    Log.e("GEOCODER_STATUS", "Status = " + status);

                    if (!"OK".equals(status)) {
                        Log.e("GEOCODER_API", "API Error: " + status);
                        return;
                    }

                    JSONArray results = json.getJSONArray("results");
                    Log.e("GEOCODER_RESULTS", "Results length = " + results.length());

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


    private void professionalsGeo() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) return;

                    for (var doc : query.getDocuments()) {

                        if (!"professional".equals(doc.getString("role"))) continue;

                        GeoPoint geo = doc.getGeoPoint("geo");

                        boolean needsGeocoding =
                                geo == null ||
                                        Math.abs(geo.getLatitude()) < 0.0001 ||
                                        Math.abs(geo.getLongitude()) < 0.0001;

                        // --- Read nested address ---
                        Map<String, Object> addressMap = (Map<String, Object>) doc.get("address");

                        if (addressMap == null) {
                            Log.e("ADDRESS", "User " + doc.getId() + " has no address map.");
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
                            postal = postal.substring(0,3) + " " + postal.substring(3);
                        }

                        String fullAddress = street
                                + (apt != null && !apt.isEmpty() ? " " + apt : "")
                                + ", " + city
                                + ", " + state + " " + postal
                                + ", " + (country != null ? country : "Canada");

                        if (needsGeocoding) {
                            Log.d("GEOCODE_REQUEST", "Geocoding: " + fullAddress);

                            geocodeWithGoogleAPI(fullAddress, latLng -> {

                                Log.d("GEOCODE_RESPONSE", doc.getString("name") +
                                        " → " + latLng.latitude + ", " + latLng.longitude);

                                db.collection("Users")
                                        .document(doc.getId())
                                        .update("geo", new GeoPoint(latLng.latitude, latLng.longitude));

                                String name = doc.getString("name");
                                String profession = doc.getString("profession");
                                String profileUrl = doc.getString("profilepic");

                                Glide.with(requireActivity())
                                        .asBitmap()
                                        .load(profileUrl)
                                        .into(new CustomTarget<Bitmap>() {
                                            @Override
                                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                                if (!isAdded() || getActivity() == null) return;

                                                View markerView = getMarkerView(name, profession, resource);
                                                Bitmap markerBitmap = createBitmapFromView(markerView);

                                                Marker m = googleMap.addMarker(
                                                        new MarkerOptions()
                                                                .position(latLng)
                                                                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                                                                .anchor(0.5f, 1.0f)
                                                );

                                                if (m != null) {
                                                    m.setTag(doc.getId());  // attach professional's Firestore ID
                                                }

                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                                        });

                            });

                        } else {
                            LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude());

                            String name = doc.getString("name");
                            String profession = doc.getString("profession");
                            String profileUrl = doc.getString("profilepic");

                            Glide.with(requireActivity())
                                    .asBitmap()
                                    .load(profileUrl)
                                    .into(new CustomTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                            if (!isAdded() || getActivity() == null) return;
                                            View markerView = getMarkerView(name, profession, resource);
                                            Bitmap markerBitmap = createBitmapFromView(markerView);

                                            Marker m = googleMap.addMarker(
                                                    new MarkerOptions()
                                                            .position(pos)
                                                            .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                                                            .anchor(0.5f, 1.0f)
                                            );

                                            if (m != null) {
                                                m.setTag(doc.getId());  // attach professional's Firestore ID
                                            }

                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                                    });

                        }
                    }
                });
    }
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








}