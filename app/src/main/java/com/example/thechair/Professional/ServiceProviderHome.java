/** ------------------------------------------------------------
 * Shaq’s Notes:
 *
 *  - This is the main **home container** for professionals.
 *    Similar structure to CustomerHome but with fewer tabs.
 *
 *  - Responsibilities:
 *        • Applies edge-to-edge layout + handles status bar insets.
 *        • Hosts a FrameLayout (appMainViewpro) that swaps fragments.
 *        • Default screen = ProHomeFragment (appointments + dashboard).
 *        • Bottom nav lets the stylist switch between:
 *              - Home (ProHomeFragment)
 *              - Profile (ProProfileFragment)
 *
 *  - Behavior:
 *        • No back stack for tab switching → replacing fragments directly.
 *        • Relies on professional_service_provider_profile_activity.xml
 *          which defines the FrameLayout + bottom navigation bar.
 *
 *  - Both fragments (Home + Profile) load real-time data from Firestore:
 *        • ProHomeFragment → appointments + name + picture cache
 *        • ProProfileFragment → portfolio + editable profile
 *
 *  - This activity acts as the “root” after login for stylists.
 * ------------------------------------------------------------- */

package com.example.thechair.Professional;

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

public class ServiceProviderHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.professional_service_provider_profile_activity);

        FrameLayout container = findViewById(R.id.appMainViewpro);

        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.appMainViewpro, new ProHomeFragment())
                .commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavViewpro);
        bottomNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        bottomNavigationView.setOnItemSelectedListener(item -> {

            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home_pro) {
                fragment = new ProHomeFragment();
            } else if (id == R.id.nav_profile_pro) {
                fragment = new ProProfileFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.appMainViewpro, fragment)
                        .commit();
            }

            return true;
        });
    }
}
