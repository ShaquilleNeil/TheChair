// Shaq’s Notes:
// This is the customer’s command deck. The bottom navigation is the traffic cop,
// swapping fragments into appMainView when each tab is tapped. The activity also
// listens for a special “openTab” flag coming from ConfirmBooking to jump straight
// back to Home.
//
// Window inset handling keeps content from hiding under the status bar.
// Everything else is clean fragment transactions with zero retained state,
// which keeps navigation predictable.

package com.example.thechair.Customer;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.thechair.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class CustomerHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.customer_profile_activity);

        // Container for fragment swapping
        FrameLayout container = findViewById(R.id.appMainView);

        // Push UI down from the status bar so nothing hides behind it
        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);

        // Always show label text
        bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        // If coming from ConfirmBooking with "openTab", go straight to Home
        String openTab = getIntent().getStringExtra("openTab");
        if ("home".equals(openTab)) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appMainView, new HomeFragment())
                    .commit();
        } else {
            // Default = Home tab
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appMainView, new HomeFragment())
                    .commit();
        }

        // Switch fragments when bottom nav is tapped
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) fragment = new HomeFragment();
            else if (id == R.id.nav_search) fragment = new SearchFragment();
            else if (id == R.id.nav_bookings) fragment = new BookingsFragment();
            else if (id == R.id.nav_profile) fragment = new ProfileFragment();
            else if (id == R.id.nav_map) fragment = new NearbyFragment();

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.appMainView, fragment)
                        .commit();
            }

            return true;
        });
    }
}
