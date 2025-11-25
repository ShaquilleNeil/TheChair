// Shaq’s Notes:
// This adapter displays reviews for a professional. Each review item includes:
// - the reviewer's name,
// - the text comment,
// - the numeric star rating.
//
// Reviews are stored as Maps (from Firestore) rather than a typed model,
// so getOrDefault() is used to safely read fields that may or may not exist.
// update() replaces the list and refreshes the UI.

package com.example.thechair.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.util.List;
import java.util.Map;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    private List<Map<String, Object>> list;   // raw reviews from Firestore

    public ReviewsAdapter(List<Map<String, Object>> list) {
        this.list = list;
    }

    // Replace list contents and refresh UI
    public void update(List<Map<String, Object>> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    // -------------------- VIEW HOLDER --------------------
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, comment;
        RatingBar rating;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.reviewName);
            comment = itemView.findViewById(R.id.reviewComment);
            rating = itemView.findViewById(R.id.reviewRating);
        }
    }

    @NonNull
    @Override
    public ReviewsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate one review item card
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {

        // Get the review map for this position
        Map<String, Object> review = list.get(pos);

        // -------------------- BIND REVIEW FIELDS --------------------
        h.name.setText((String) review.getOrDefault("customerName", "User"));
        h.comment.setText((String) review.getOrDefault("comment", ""));

        // Convert Double → float for RatingBar
        h.rating.setRating(((Double) review.getOrDefault("rating", 0.0)).floatValue());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
