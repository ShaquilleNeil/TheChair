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
    private Context mContext;
    private List<String> mUploads;

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String imageUrl);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    public GalleryAdapter(Context context, List<String> uploads) {
        mContext = context;
        mUploads = uploads;
    }




    @NonNull
    @Override
    public GalleryAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_grid_image, parent, false);


        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryAdapter.MyViewHolder holder, int position) {
        String imageUrl = mUploads.get(position);


        Glide.with(mContext)
                .load(imageUrl)
                .placeholder(R.drawable.image_placeholder_bg) // fallback background
                .error(R.drawable.ic_broken_image)             // optional: show if failed
                .centerCrop()
                .into(holder.workImage);

        holder.itemView.setOnClickListener( v -> {
            if (listener != null) {
               listener.onItemClick(imageUrl);
            }
        });


    }

    @Override
    public int getItemCount() {
        return mUploads.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

       ImageView workImage;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            workImage = itemView.findViewById(R.id.workImage);
        }
    }




}
