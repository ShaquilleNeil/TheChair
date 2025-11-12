package com.example.thechair.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private List<Map<String, Object>> data;
    private OnProfessionalClickListener listener;

    // 🔹 Interface for click events
    public interface OnProfessionalClickListener {
        void onProfessionalClick(Map<String, Object> professional);
    }

    public void setOnProfessionalClickListener(OnProfessionalClickListener listener) {
        this.listener = listener;
    }

    public SearchAdapter(List<Map<String, Object>> data) {
        this.data = data;
    }

    public void updateData(List<Map<String, Object>> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, int position) {
        Map<String, Object> item = data.get(position);

        String name = (String) item.get("name");
        String profilePic = (String) item.get("profilepic");
        String profession = (String) item.get("profession");

        holder.proName.setText(name != null ? name : "Unknown");
        holder.proProfession.setText(profession != null ? profession : "");

        if (profilePic != null && !profilePic.isEmpty()) {
            new ImageLoaderTask(profilePic, holder.profileImage).execute();
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_person);
        }

        // 🔹 Handle click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProfessionalClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView proName, proProfession;
        ImageView profileImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            proName = itemView.findViewById(R.id.proName);
            proProfession = itemView.findViewById(R.id.proProfession);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }

    // 🔹 Simplified ImageLoaderTask
    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;

        public ImageLoaderTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                try (InputStream input = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(input);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_person);
            }
        }
    }
}
