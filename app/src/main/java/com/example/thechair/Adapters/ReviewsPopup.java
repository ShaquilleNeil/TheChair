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

    private final RecyclerView recycler;
    private final ReviewsAdapter adapter;

    public ReviewsPopup(Context context, String professionalId) {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup_reviews);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCancelable(true);

        recycler = findViewById(R.id.recyclerReviews);
        recycler.setLayoutManager(new LinearLayoutManager(context));
        adapter = new ReviewsAdapter(new ArrayList<>());
        recycler.setAdapter(adapter);

        loadReviews(professionalId);
    }

    private void loadReviews(String professionalId) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(professionalId)
                .collection("ratings")
                .get()
                .addOnSuccessListener(query -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (var doc : query.getDocuments()) {
                        list.add(doc.getData());
                    }
                    adapter.update(list);
                });
    }
}
