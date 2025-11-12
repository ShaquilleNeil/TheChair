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

    public interface SearchCallback {
        void onResults(List<Map<String, Object>> results);
    }

    private static final OkHttpClient client = new OkHttpClient();

    public static void searchProfessionals(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onResults(new ArrayList<>()));
            return;
        }

        new Thread(() -> {
            try {
                // ✅ 1. Build JSON payload
                JSONObject json = new JSONObject();
                json.put("searchValue", query.trim());
                json.put("limit", 20);

                JSONArray fields = new JSONArray();
                fields.put("name");
                fields.put("services.name");
                fields.put("city");
                fields.put("tags");
                fields.put("profession");
                fields.put("profilepic");
                fields.put("role");
                json.put("fields", fields);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("https://us-central1-thechair-ed8af.cloudfunctions.net/ext-firestore-search-extension-searchCollectionHttp/v2/Users")
                        .post(body)
                        .build();


                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        android.util.Log.e("SearchRepository", "❌ HTTP " + response.code());
                        new Handler(Looper.getMainLooper()).post(() -> callback.onResults(new ArrayList<>()));
                        return;
                    }

                    String responseBody = response.body().string();
                    android.util.Log.d("SearchRepository", "🔥 Response: " + responseBody);

                    List<Map<String, Object>> results = new ArrayList<>();

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray dataArray = jsonObject.optJSONArray("data");

                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);
                                if ("professional".equalsIgnoreCase(obj.optString("role"))) {

                                    Map<String, Object> map = new java.util.HashMap<>();
                                    map.put("id", obj.optString("id"));
                                    map.put("name", obj.optString("name"));
                                    map.put("city", obj.optString("city"));
                                    map.put("profession", obj.optString("profession"));
                                    map.put("profilepic", obj.optString("profilepic"));
                                    map.put("role", obj.optString("role"));

                                    JSONArray tagsArray = obj.optJSONArray("tags");
                                    if (tagsArray != null) {
                                        List<String> tags = new ArrayList<>();
                                        for (int j = 0; j < tagsArray.length(); j++) {
                                            tags.add(tagsArray.getString(j));
                                        }
                                        map.put("tags", tags);
                                    }


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
                                                    android.util.Log.e("SearchRepository", "⚠️ Firestore fetch failed for " + id + ": " + err.getMessage())
                                            );

                                    results.add(map);
                                }
                            }
                        }

                        android.util.Log.d("SearchRepository", "✅ Parsed professionals: " + results.size());
                        new Handler(Looper.getMainLooper()).post(() -> callback.onResults(results));

                    } catch (Exception parseError) {
                        android.util.Log.e("SearchRepository", "❌ JSON Parse Error: " + parseError.getMessage());
                        new Handler(Looper.getMainLooper()).post(() -> callback.onResults(new ArrayList<>()));
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("SearchRepository", "❌ Exception: " + e.getMessage());
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResults(new ArrayList<>()));
            }
        }).start();
    }
}
