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

    private Context mContext;
    private List<appUsers> mProfessionals;
    private UserManager userManager;


    public ProfessionalsAdapter(Context context, List<appUsers> professionals) {
        mContext = context;
        mProfessionals = professionals;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.prof_info_adapter_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        appUsers professional = mProfessionals.get(position);

        holder.tvprovidername.setText(professional.getName());

        String imageUrl = professional.getProfilepic();
        if (imageUrl != null) {
            new ImageLoaderTask(imageUrl, holder.ivprovider, userManager.getInstance()).execute();
        } else {
            holder.ivprovider.setImageResource(R.drawable.banner);
        }

    }

    @Override
    public int getItemCount() {
        return mProfessionals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivprovider;
        TextView tvprovidername;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivprovider = itemView.findViewById(R.id.ivprovider);
            tvprovidername = itemView.findViewById(R.id.tvprovidername);
        }
    }




    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;
        private final UserManager userManager;

        public ImageLoaderTask(String url, ImageView imageView, UserManager userManager) {
            this.url = url;
            this.imageView = imageView;
            this.userManager = userManager;
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
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                userManager.setProfileBitmap(bitmap); // cache the bitmap
            } else {
                imageView.setImageResource(R.drawable.banner);
            }
        }
    }




}
