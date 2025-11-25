// Shaq’s Notes:
// This class is the search engine for professionals in The Chair.
// It talks to a Firebase Firestore Search Extension (HTTP endpoint), sends a
// JSON body with the search query and fields, and returns a list of matching
// professionals as Maps.
//
// Flow:
// 1. If query is empty → immediately return empty list on main thread.
// 2. Otherwise, launch a background thread.
// 3. Build JSON payload with searchValue, limit, and fields to search.
// 4. Send POST request with OkHttp to the Cloud Function URL.
// 5. Parse the JSON response, but only keep documents where role == "professional".
// 6. For each professional:
//      - extract id, name, city, profession, profilepic, role, and tags,
//      - then do an extra Firestore get() to fetch the full "services" field,
//        adding it into the same map when it arrives.
// 7. Post the list of results back to the main thread via callback.
//
// This class is repository-style: UI calls searchProfessionals() and gets
// cleaned data back via SearchCallback, without worrying about HTTP/JSON.

package com.example.thechair.Adapters;

import android.os.Handler;
import android.os.Looper;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchRepository {

    // Callback interface so the UI can receive results asynchronously
    public interface SearchCallback {
        void onResults(List<Map<String, Object>> results);
    }

    // Shared OkHttpClient instance
    private static final OkHttpClient client = new OkHttpClient();

    // Main entry point for performing search
    public static void searchProfessionals(String query, SearchCallback callback) {

        // If the query is empty or null, immediately return an empty list
        if (query == null || query.trim().isEmpty()) {
            new Handler(Looper.getMainLooper()).post(
                    () -> callback.onResults(new ArrayList<>())
            );
            return;
        }

        // Run network logic on a background thread
        new Thread(() -> {
            try {
                // 1) Build JSON payload for the search request
                JSONObject json = new JSONObject();
                json.put("searchValue", query.trim());
                json.put("limit", 20);

                // Fields to search in the extension
                JSONArray fields = new JSONArray();
                fields.put("name");
                fields.put("services.name");
                fields.put("city");
                fields.put("tags");
                fields.put("profession");
                fields.put("profilepic");
                fields.put("role");
                json.put("fields", fields);

                // Request body as JSON
                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                // HTTP POST to Firestore Search Extension endpoint
                Request request = new Request.Builder()
                        .url("https://us-central1-thechair-ed8af.cloudfunctions.net/ext-firestore-search-extension-searchCollectionHttp/v2/Users")
                        .post(body)
                        .build();

                // Execute the request synchronously on this background thread
                try (Response response = client.newCall(request).execute()) {

                    if (!response.isSuccessful()) {
                        android.util.Log.e("SearchRepository", "❌ HTTP " + response.code());

                        // Return empty list on main thread on failure
                        new Handler(Looper.getMainLooper()).post(
                                () -> callback.onResults(new ArrayList<>())
                        );
                        return;
                    }

                    // Raw JSON response from the extension
                    String responseBody = response.body().string();
                    android.util.Log.d("SearchRepository", "🔥 Response: " + responseBody);

                    List<Map<String, Object>> results = new ArrayList<>();

                    try {
                        // 2) Parse JSON response
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray dataArray = jsonObject.optJSONArray("data");

                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);

                                // Only keep documents where role == "professional"
                                if ("professional".equalsIgnoreCase(obj.optString("role"))) {

                                    Map<String, Object> map = new java.util.HashMap<>();
                                    map.put("id", obj.optString("id"));
                                    map.put("name", obj.optString("name"));
                                    map.put("city", obj.optString("city"));
                                    map.put("profession", obj.optString("profession"));
                                    map.put("profilepic", obj.optString("profilepic"));
                                    map.put("role", obj.optString("role"));

                                    // Optional tags array → convert JSON array to List<String>
                                    JSONArray tagsArray = obj.optJSONArray("tags");
                                    if (tagsArray != null) {
                                        List<String> tags = new ArrayList<>();
                                        for (int j = 0; j < tagsArray.length(); j++) {
                                            tags.add(tagsArray.getString(j));
                                        }
                                        map.put("tags", tags);
                                    }

                                    // 3) Enrich result with full "services" from Firestore doc
                                    String id = obj.optString("id");
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("Users")
                                            .document(id)
                                            .get()
                                            .addOnSuccessListener(snapshot -> {
                                                if (snapshot.exists()) {
                                                    Object realServices = snapshot.get("services");
                                                    if (realServices != null) {
                                                        map.put("services", realServices);
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(err ->
                                                    android.util.Log.e(
                                                            "SearchRepository",
                                                            "⚠️ Firestore fetch failed for " + id +
                                                                    ": " + err.getMessage()
                                                    )
                                            );

                                    // Add to local results list
                                    results.add(map);
                                }
                            }
                        }

                        android.util.Log.d("SearchRepository",
                                "✅ Parsed professionals: " + results.size());

                        // 4) Return results to callback on main thread
                        new Handler(Looper.getMainLooper()).post(
                                () -> callback.onResults(results)
                        );

                    } catch (Exception parseError) {
                        android.util.Log.e("SearchRepository",
                                "❌ JSON Parse Error: " + parseError.getMessage());

                        new Handler(Looper.getMainLooper()).post(
                                () -> callback.onResults(new ArrayList<>())
                        );
                    }
                }
            } catch (Exception e) {
                // Catch-all for network/JSON exceptions
                android.util.Log.e("SearchRepository", "❌ Exception: " + e.getMessage());
                e.printStackTrace();

                new Handler(Looper.getMainLooper()).post(
                        () -> callback.onResults(new ArrayList<>())
                );
            }
        }).start();
    }
}
