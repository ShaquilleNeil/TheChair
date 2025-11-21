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

    private List<Map<String, Object>> list;

    public ReviewsAdapter(List<Map<String, Object>> list) {
        this.list = list;
    }

    public void update(List<Map<String, Object>> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

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
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Map<String, Object> review = list.get(pos);

        h.name.setText((String) review.getOrDefault("customerName", "User"));
        h.comment.setText((String) review.getOrDefault("comment", ""));
        h.rating.setRating(((Double) review.getOrDefault("rating", 0.0)).floatValue());
    }

    @Override
    public int getItemCount() { return list.size(); }
}
