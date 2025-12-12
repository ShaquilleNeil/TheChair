// Shaq’s Notes:
// This adapter powers the search results list. Each row shows:
// - the professional’s name,
// - their profession label,
// - their profile image,
// - and triggers a click event returning the whole Firestore Map.
//
// The adapter uses Maps instead of typed models because search results often
// come from Firestore queries without strict modeling. Images are loaded using
// a lightweight AsyncTask. updateData() refreshes results dynamically.

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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.thechair.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private List<Map<String, Object>> data;                     // search results
    private OnProfessionalClickListener listener;

    // Click callback for parent fragment/activity
    public interface OnProfessionalClickListener {
        void onProfessionalClick(Map<String, Object> professional);
    }

    public void setOnProfessionalClickListener(OnProfessionalClickListener listener) {
        this.listener = listener;
    }

    public SearchAdapter(List<Map<String, Object>> data) {
        this.data = data;
    }

    // Replace dataset and update UI
    public void updateData(List<Map<String, Object>> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflate each search result card
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, int position) {

        Map<String, Object> item = data.get(position);

        // -------------------- TEXT FIELDS --------------------
        String name = (String) item.get("name");
        String profilePic = (String) item.get("profilepic");
        String profession = (String) item.get("profession");

        holder.proName.setText(name != null ? name : "Unknown");
        holder.proProfession.setText(profession != null ? profession : "");

        // -------------------- PROFILE IMAGE --------------------
        Glide.with(holder.itemView.getContext())
                .load(profilePic)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // memory + disk cache
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(holder.profileImage);

        // -------------------- CLICK HANDLER --------------------
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProfessionalClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // -------------------- VIEW HOLDER --------------------
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

    // -------------------- IMAGE LOADER (AsyncTask) --------------------
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
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(url).openConnection();

                connection.connect();

                try (InputStream input = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(input);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null; // fallback handled in onPostExecute
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
