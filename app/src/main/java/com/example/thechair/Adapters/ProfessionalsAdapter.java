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

import com.example.thechair.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ProfessionalsAdapter extends RecyclerView.Adapter<ProfessionalsAdapter.ViewHolder> {

    private final Context mContext;
    private List<appUsers> mProfessionals;

    // CLICK LISTENER
    public interface ProfessionalClickListener {
        void onProfessionalClick(appUsers professional);
    }

    private ProfessionalClickListener clickListener;

    public void setOnProfessionalClickListener(ProfessionalClickListener listener) {
        this.clickListener = listener;
    }

    public ProfessionalsAdapter(Context context, List<appUsers> professionals) {
        this.mContext = context;
        this.mProfessionals = professionals;
    }

    // ---- NEW: Proper updateList method ----
    public void updateList(List<appUsers> newList) {
        this.mProfessionals = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfessionalsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.prof_info_adapter_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfessionalsAdapter.ViewHolder holder, int position) {
        appUsers professional = mProfessionals.get(position);

        holder.tvprovidername.setText(professional.getName());

        String imageUrl = professional.getProfilepic();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            new ImageLoaderTask(imageUrl, holder.ivprovider).execute();
        } else {
            holder.ivprovider.setImageResource(R.drawable.banner);
        }
    }

    @Override
    public int getItemCount() {
        return mProfessionals.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivprovider;
        TextView tvprovidername;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivprovider = itemView.findViewById(R.id.ivprovider);
            tvprovidername = itemView.findViewById(R.id.tvprovidername);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onProfessionalClick(mProfessionals.get(pos));
                }
            });
        }
    }

    // ----------------- IMAGE LOADER -----------------

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
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.banner);
            }
        }
    }
}
