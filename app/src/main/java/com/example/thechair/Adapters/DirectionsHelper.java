package com.example.thechair.Adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

public class DirectionsHelper {

    public static void openExternalGoogleMaps(Context context, LatLng destination, String label) {

        // Ensure label is safe
        if (label == null || label.trim().isEmpty()) {
            label = "Destination";
        }

        // Encode label for spaces & special characters
        String encodedLabel = Uri.encode(label);

        Uri gmmIntentUri = Uri.parse(
                "geo:0,0?q="
                        + destination.latitude + "," + destination.longitude
                        + "(" + encodedLabel + ")"
        );

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            context.startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {

            // Fallback to browser
            Uri webUri = Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query="
                            + destination.latitude + "," + destination.longitude
                            + "&query_place_id=" + encodedLabel
            );
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            context.startActivity(webIntent);
        }
    }
}
