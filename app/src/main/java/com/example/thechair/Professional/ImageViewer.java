package com.example.thechair.Professional;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.thechair.R;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageViewer extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_viewer);

        PhotoView photoView = findViewById(R.id.photoView);
        String url = getIntent().getStringExtra("imageUrl");

        Glide.with(this).load(url).into(photoView);

        photoView.setOnClickListener(v -> finish());




    }
}