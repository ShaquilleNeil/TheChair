// Shaq’s Notes:
// This adapter displays a grid of images—used for professional portfolios.
// Each item loads an image URL using Glide and shows it in a square grid cell.
// When the user taps an image, the adapter calls a listener so the parent
// fragment/activity can open a full-screen viewer or perform another action.
//
// Very simple: bind URL → show image → handle click.

package com.example.thechair.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thechair.R;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.MyViewHolder> {

    private final Context mContext;
    private final List<String> mUploads; // list of image URLs

    // Click listener interface
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String imageUrl);
    }

    // Parent sets this to handle full-screen viewing or actions
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Constructor
    public GalleryAdapter(Context context, List<String> uploads) {
        mContext = context;
        mUploads = uploads;
    }

    @NonNull
    @Override
    public GalleryAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflate each grid cell layout
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_grid_image, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryAdapter.MyViewHolder holder, int position) {

        String imageUrl = mUploads.get(position);

        // -------------------- LOAD IMAGE USING GLIDE --------------------
        Glide.with(mContext)
                .load(imageUrl)
                .placeholder(R.drawable.image_placeholder_bg) // shown while loading
                .error(R.drawable.ic_broken_image)             // shown if image fails
                .centerCrop()
                .into(holder.workImage);

        // -------------------- CLICK HANDLER --------------------
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(imageUrl);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUploads.size();
    }

    // -------------------- VIEW HOLDER --------------------
    public static class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView workImage;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            workImage = itemView.findViewById(R.id.workImage);
        }
    }
}
