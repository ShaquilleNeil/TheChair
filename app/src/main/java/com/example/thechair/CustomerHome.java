package com.example.thechair;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_profile);

        //loading the fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.appMainView, new HomeFragment()).commit();


        //create listener for bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        bottomNav.setOnItemSelectedListener(item ->{
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_search) {
                fragment = new SearchFragment();
            } else if (id == R.id.nav_bookings) {
                fragment = new BookingsFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.appMainView, fragment)
                        .commit();
            }


            return true;

        });



    }
}