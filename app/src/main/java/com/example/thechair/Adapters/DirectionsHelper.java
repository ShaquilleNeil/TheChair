// Shaq’s Notes:
// This helper launches Google Maps to give the user turn-by-turn directions.
// It creates a geo URI that opens the Google Maps app directly. If the app
// isn't installed, it falls back to opening Maps in a web browser.
//
// The label (place name) is encoded to avoid issues with spaces/special chars.
// LatLng is passed in from Google Maps SDK for Android.

package com.example.thechair.Adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

public class DirectionsHelper {

    // Opens Google Maps to navigate to the given coordinates
    public static void openExternalGoogleMaps(Context context, LatLng destination, String label) {

        // Use default label if none provided
        if (label == null || label.trim().isEmpty()) {
            label = "Destination";
        }

        // Encode text so spaces & symbols don't break the URI
        String encodedLabel = Uri.encode(label);

        // Primary URI that launches the Google Maps app
        Uri gmmIntentUri = Uri.parse(
                "geo:0,0?q="
                        + destination.latitude + "," + destination.longitude
                        + "(" + encodedLabel + ")"
        );

        // Build intent for Google Maps app
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps"); // ensures app open

        try {
            // Try launching Google Maps app
            context.startActivity(mapIntent);

        } catch (ActivityNotFoundException e) {

            // Fallback: open Google Maps in a browser
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
