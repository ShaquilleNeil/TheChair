// Shaq’s Notes:
// This adapter displays a list of professionals (providers/barbers/stylists).
// Each item shows:
// - the professional’s name,
// - their profile image,
// - and triggers a click callback when tapped.
//
// The adapter supports dynamic updates through updateList(). Profile images are
// loaded using a lightweight AsyncTask (a simplified alternative to Glide).
// When a professional is clicked, the parent fragment/activity receives the
// selected appUsers object via the ProfessionalClickListener.

package com.example.thechair.Adapters;

import android.content.Context;
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

public class ProfessionalsAdapter extends RecyclerView.Adapter<ProfessionalsAdapter.ViewHolder> {

    private final Context mContext;
    private List<appUsers> mProfessionals;

    // ---------- CLICK LISTENER: Parent handles what happens on click ----------
    public interface ProfessionalClickListener {
        void onProfessionalClick(appUsers professional);
    }

    private ProfessionalClickListener clickListener;

    public void setOnProfessionalClickListener(ProfessionalClickListener listener) {
        this.clickListener = listener;
    }

    // Constructor
    public ProfessionalsAdapter(Context context, List<appUsers> professionals) {
        this.mContext = context;
        this.mProfessionals = professionals;
    }

    // Replace the current list and refresh UI
    public void updateList(List<appUsers> newList) {
        this.mProfessionals = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfessionalsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate provider row layout
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.prof_info_adapter_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfessionalsAdapter.ViewHolder holder, int position) {

        appUsers professional = mProfessionals.get(position);

        // Name display
        holder.tvprovidername.setText(professional.getName());

        // Profile picture loading
        String imageUrl = professional.getProfilepic();

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.banner)
                .error(R.drawable.banner)
                .into(holder.ivprovider);

    }

    @Override
    public int getItemCount() {
        return mProfessionals.size();
    }

    // -------------------- VIEW HOLDER --------------------
    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivprovider;
        TextView tvprovidername;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivprovider = itemView.findViewById(R.id.ivprovider);
            tvprovidername = itemView.findViewById(R.id.tvprovidername);

            // Handle click → send selected professional object to parent
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onProfessionalClick(mProfessionals.get(pos));
                }
            });
        }
    }

    // -------------------- IMAGE LOADER (AsyncTask) --------------------
    // Downloads an image in the background and sets it into the ImageView.

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
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);

            } catch (Exception e) {
                e.printStackTrace();
                return null; // fail → null
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.banner); // fallback image
            }
        }
    }
}
