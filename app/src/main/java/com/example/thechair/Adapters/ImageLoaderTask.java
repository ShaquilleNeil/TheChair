package com.example.thechair.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

    private final String url;
    private final ImageView imageView;
    private final UserManager userManager;

    // ✅ THIS is the constructor your fragments expect
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

            // Only store cache if provided
            if (userManager != null) {
                userManager.setProfileBitmap(bitmap);
            }
        }
    }
}
