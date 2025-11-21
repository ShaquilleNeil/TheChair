package com.example.thechair.Adapters;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

public class DirectionsHelper {

    public static void openExternalGoogleMaps(Context context, LatLng destination, String label) {

        // Opens Google Maps with a pin and ALL routing modes available.
        Uri gmmIntentUri = Uri.parse(
                "geo:0,0?q="
                        + destination.latitude + "," + destination.longitude
                        + "(" + label + ")"
        );

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            context.startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {

            // Fallback: open browser version (also shows all modes)
            Uri webUri = Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query="
                            + destination.latitude + "," + destination.longitude
            );
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            context.startActivity(webIntent);
        }
    }
}
