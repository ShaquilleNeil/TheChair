// Shaq’s Notes:
// This AsyncTask manually downloads an image from a URL on a background thread.
// When done, it sets the Bitmap in an ImageView and optionally caches it inside
// UserManager (used for remembering profile pictures in memory).
//
// Glide or Coil is usually preferred, but this class exists for custom caching.
// The constructor must include (url, imageView, userManager) because fragments
// are calling it exactly with those parameters.

package com.example.thechair.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

    private final String url;             // image URL to download
    private final ImageView imageView;    // target ImageView
    private final UserManager userManager; // optional cache manager

    // Required constructor: fragments depend on this signature
    public ImageLoaderTask(String url, ImageView imageView, UserManager userManager) {
        this.url = url;
        this.imageView = imageView;
        this.userManager = userManager;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        try {
            // Open connection to the image URL
            URL urlConnection = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
            connection.setDoInput(true);
            connection.connect();

            // Download stream → decode into bitmap
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // return null if download failed
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {

        // Only update UI if bitmap successfully loaded
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);

            // Cache profile image if UserManager provided
            if (userManager != null) {
                userManager.setProfileBitmap(bitmap);
            }
        }
    }
}
