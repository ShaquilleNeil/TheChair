// Shaq’s Notes:
// This class creates a custom popup dialog that displays all reviews for a
// professional. It works like a mini full-screen card:
//
// - Transparent background so the popup floats over the screen
// - Contains a RecyclerView with ReviewsAdapter
// - Automatically fetches the professional’s “ratings” subcollection
//   and populates the list
//
// The parent screen only needs to create -> show this dialog.

package com.example.thechair.Adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReviewsPopup extends Dialog {

    private final RecyclerView recycler;   // displays the review list
    private final ReviewsAdapter adapter;  // binds review data to UI

    public ReviewsPopup(Context context, String professionalId) {
        super(context);

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Load popup layout
        setContentView(R.layout.popup_reviews);

        // Transparent rounded background
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setCancelable(true); // tapping outside dismisses

        // Setup RecyclerView
        recycler = findViewById(R.id.recyclerReviews);
        recycler.setLayoutManager(new LinearLayoutManager(context));

        // Empty list until Firestore loads
        adapter = new ReviewsAdapter(new ArrayList<>());
        recycler.setAdapter(adapter);

        // Fetch ratings from Firestore
        loadReviews(professionalId);
    }

    // -------------------- LOAD REVIEWS FROM FIRESTORE --------------------
    private void loadReviews(String professionalId) {

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(professionalId)
                .collection("ratings")           // each doc = one review
                .get()
                .addOnSuccessListener(query -> {

                    List<Map<String, Object>> list = new ArrayList<>();

                    // Convert each Firestore document to a Map
                    for (var doc : query.getDocuments()) {
                        list.add(doc.getData());
                    }

                    // Update adapter with reviews
                    adapter.update(list);
                });
    }
}
