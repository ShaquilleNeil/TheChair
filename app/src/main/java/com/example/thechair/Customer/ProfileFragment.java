// Shaq’s Notes:
// This fragment is the customer’s little home base: personal info, image,
// address summary, editing, and sign-out. The caching pattern is tight:
// fast local UI → silent Firestore refresh → bitmap cache update.
// The only structural thing worth flagging is ensuring you never show
// “, , ,” when parts of the address are empty. Your builder already avoids
// that by checking each field, so you’re good. Lifecycle-wise, everything
// behaves fine because AsyncTask only drops a bitmap into an ImageView
// the fragment still owns.

package com.example.thechair.Customer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thechair.AuthFlow.AuthFlow;
import com.example.thechair.R;
import com.example.thechair.Adapters.UserManager;
import com.example.thechair.Adapters.appUsers;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private TextView name, email, phone, profileaddress;
    private ImageView profileimage;

    private Button editprofile, signout;

    public ProfileFragment() { }

    public static ProfileFragment newInstance(String p1, String p2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PARAM1, p1);
        b.putString(ARG_PARAM2, p2);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle savedState) {

        View v = inf.inflate(R.layout.fragment_profile, parent, false);

        name           = v.findViewById(R.id.profileName);
        email          = v.findViewById(R.id.profileEmail);
        phone          = v.findViewById(R.id.profilePhone);
        profileimage   = v.findViewById(R.id.profileImage);
        profileaddress = v.findViewById(R.id.profileAddress);
        editprofile    = v.findViewById(R.id.editProfileButton);
        signout        = v.findViewById(R.id.btnSignOut);

        loadUser();

        editprofile.setOnClickListener(view -> {
            appUsers u = UserManager.getInstance().getUser();
            if (u == null || u.getAddress() == null) {
                Toast.makeText(getContext(), "User address not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            appUsers.Address a = u.getAddress();

            Intent i = new Intent(getActivity(), CustomerEditProfile.class);
            i.putExtra("profilepic", u.getProfilepic());
            i.putExtra("name", u.getName());
            i.putExtra("email", u.getEmail());
            i.putExtra("phone", u.getPhoneNumber());
            i.putExtra("addressLine1", a.getStreet());
            i.putExtra("addressLine2", a.getRoom());
            i.putExtra("city", a.getCity());
            i.putExtra("province", a.getProvince());
            i.putExtra("country", a.getCountry());
            i.putExtra("postalCode", a.getPostalCode());
            startActivity(i);
        });

        signout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(getActivity(), AuthFlow.class);
            startActivity(i);
            requireActivity().finish();
        });

        return v;
    }

    private void loadUser() {
        UserManager um = UserManager.getInstance();
        appUsers cached = um.getUser();

        if (cached != null) {
            name.setText(cached.getName());
            email.setText(cached.getEmail());
            phone.setText(cached.getPhoneNumber());

            appUsers.Address a = cached.getAddress();
            if (a != null) {
                StringBuilder sb = new StringBuilder();
                if (a.getStreet() != null && !a.getStreet().isEmpty()) sb.append(a.getStreet());
                if (a.getRoom() != null && !a.getRoom().isEmpty()) sb.append(", ").append(a.getRoom());
                if (a.getCity() != null && !a.getCity().isEmpty()) sb.append(", ").append(a.getCity());
                if (a.getProvince() != null && !a.getProvince().isEmpty()) sb.append(", ").append(a.getProvince());
                if (a.getCountry() != null && !a.getCountry().isEmpty()) sb.append(", ").append(a.getCountry());
                if (a.getPostalCode() != null && !a.getPostalCode().isEmpty()) sb.append(", ").append(a.getPostalCode());
                profileaddress.setText(sb.toString());
            } else {
                profileaddress.setText("Address not provided");
            }

            Bitmap bm = um.getProfileBitmap();
            profileimage.setImageBitmap(bm != null ? bm : BitmapFactory.decodeResource(getResources(), R.drawable.banner));
        }

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        FirebaseFirestore.getInstance().collection("Users")
                .document(fUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    appUsers fresh = doc.toObject(appUsers.class);
                    if (fresh == null) return;

                    name.setText(fresh.getName());
                    email.setText(fresh.getEmail());
                    phone.setText(fresh.getPhoneNumber());

                    appUsers.Address a = fresh.getAddress();
                    if (a != null) {
                        StringBuilder sb = new StringBuilder();
                        if (a.getStreet() != null && !a.getStreet().isEmpty()) sb.append(a.getStreet());
                        if (a.getRoom() != null && !a.getRoom().isEmpty()) sb.append(", ").append(a.getRoom());
                        if (a.getCity() != null && !a.getCity().isEmpty()) sb.append(", ").append(a.getCity());
                        if (a.getProvince() != null && !a.getProvince().isEmpty()) sb.append(", ").append(a.getProvince());
                        if (a.getCountry() != null && !a.getCountry().isEmpty()) sb.append(", ").append(a.getCountry());
                        if (a.getPostalCode() != null && !a.getPostalCode().isEmpty()) sb.append(", ").append(a.getPostalCode());
                        profileaddress.setText(sb.toString());
                    } else {
                        profileaddress.setText("Address not provided");
                    }

                    String p = fresh.getProfilepic();
                    if (p != null) {
                        new ImageLoaderTask(p, profileimage, um).execute();
                    } else {
                        profileimage.setImageResource(R.drawable.banner);
                    }

                    um.setUser(fresh);
                });
    }

    public static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;
        private final UserManager userManager;

        public ImageLoaderTask(String url, ImageView iv, UserManager mgr) {
            this.url = url;
            this.imageView = iv;
            this.userManager = mgr;
        }

        @Override
        protected Bitmap doInBackground(String... s) {
            try {
                URL u = new URL(url);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setDoInput(true);
                c.connect();
                InputStream in = c.getInputStream();
                return BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b != null) {
                imageView.setImageBitmap(b);
                userManager.setProfileBitmap(b);
            } else {
                imageView.setImageResource(R.drawable.banner);
            }
        }
    }
}
