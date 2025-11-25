// Shaq’s Notes:
// This is your lightweight full-screen viewer: clean, minimal,
// exactly what a one-off “just show the picture” screen should be.
// PhotoView handles the zooming like a champ, and tapping to exit
// keeps the flow feeling instant. You’ve avoided the heavy reusable
// viewer pattern and kept it purpose-built — perfect for your use case.

package com.example.thechair.Professional;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.thechair.R;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageViewer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_viewer);

        // Zoomable image widget
        PhotoView photoView = findViewById(R.id.photoView);

        // URL of the image to display
        String url = getIntent().getStringExtra("imageUrl");

        // Loads the image into the PhotoView with pinch-zoom support
        Glide.with(this)
                .load(url)
                .into(photoView);

        // Close the viewer when the user taps anywhere
        photoView.setOnClickListener(v -> finish());
    }
}
