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

        FrameLayout container = findViewById(R.id.appMainView);

        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        // If directed from ConfirmBooking, open specific tab
        String openTab = getIntent().getStringExtra("openTab");
        if ("home".equals(openTab)) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appMainView, new HomeFragment())
                    .commit();
        } else {
            // Default first tab
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appMainView, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) fragment = new HomeFragment();
            else if (id == R.id.nav_search) fragment = new SearchFragment();
            else if (id == R.id.nav_bookings) fragment = new BookingsFragment();
            else if (id == R.id.nav_profile) fragment = new ProfileFragment();

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.appMainView, fragment)
                        .commit();
            }

            return true;
        });
    }

}