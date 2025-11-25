// Shaq’s Notes:
// This ArrayAdapter is used to populate the customer profile update screen.
// It loads the user’s details (name, email, phone, full address) into editable
// input fields. When the user taps “Edit Profile,” it launches the
// CustomerEditProfile activity and passes all current values via Intent extras.
//
// Important notes:
// - All EditText fields map to the appUsers object’s nested Address data.
// - The adapter inflates customer_edit_profile_activity as the row layout.
// - Image changing button exists but isn’t wired here.
// - Several EditTexts incorrectly share the SAME ID (profileAddress).
//   They should each have separate IDs. Right now the same field is referenced
//   multiple times (address1, address2, city, province, postalCode) which is
//   likely a bug in your XML.

package com.example.thechair.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.thechair.Customer.CustomerEditProfile;
import com.example.thechair.R;

import java.util.List;

public class ProfileupdateAdapter extends ArrayAdapter<appUsers> {

    private final Context context;
    private final List<appUsers> appUsersList;

    public ProfileupdateAdapter(Context context, List<appUsers> appUsersList) {
        super(context, R.layout.customer_edit_profile_activity, appUsersList);
        this.context = context;
        this.appUsersList = appUsersList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, android.view.ViewGroup parent) {

        // Inflate the layout if needed
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.customer_edit_profile_activity, null);
        }

        // Get the user object for this row
        appUsers appusers = appUsersList.get(position);

        // ------------- UI REFERENCES -------------

        ImageView imageView = convertView.findViewById(R.id.profileImage);
        ImageButton btnChangePhoto = convertView.findViewById(R.id.btnChangePhoto);

        EditText inputName = convertView.findViewById(R.id.profileName);
        EditText inputEmail = convertView.findViewById(R.id.profileEmail);
        EditText inputPhone = convertView.findViewById(R.id.profilePhone);

        // ⚠️ These should NOT all share profileAddress in real XML.
        // They must be separate fields. This is a warning.
        EditText inputAddress1 = convertView.findViewById(R.id.profileAddress);
        EditText inputAddress2 = convertView.findViewById(R.id.profileAddress);
        EditText inputCity = convertView.findViewById(R.id.profileAddress);
        EditText inputProvince = convertView.findViewById(R.id.profileAddress);
        EditText inputPostalCode = convertView.findViewById(R.id.profileAddress);

        Button editProfile = convertView.findViewById(R.id.editProfileButton);

        // ------------- POPULATE THE EDIT FIELDS -------------

        inputName.setText(appusers.getName());
        inputEmail.setText(appusers.getEmail());
        inputPhone.setText(appusers.getPhoneNumber());

        inputAddress1.setText(appusers.getAddress().getStreet());
        inputAddress2.setText(appusers.getAddress().getRoom());
        inputCity.setText(appusers.getAddress().getCity());
        inputProvince.setText(appusers.getAddress().getProvince());
        inputPostalCode.setText(appusers.getAddress().getPostalCode());

        // ------------- HANDLE EDIT PROFILE BUTTON -------------

        editProfile.setOnClickListener(v -> {

            // Launch edit profile activity with all fields as extras
            Intent intent = new Intent(context, CustomerEditProfile.class);

            intent.putExtra("name", inputName.getText().toString());
            intent.putExtra("email", inputEmail.getText().toString());
            intent.putExtra("phone", inputPhone.getText().toString());

            intent.putExtra("address1", inputAddress1.getText().toString());
            intent.putExtra("address2", inputAddress2.getText().toString());
            intent.putExtra("city", inputCity.getText().toString());
            intent.putExtra("province", inputProvince.getText().toString());
            intent.putExtra("postalCode", inputPostalCode.getText().toString());

            context.startActivity(intent);
        });

        return convertView;
    }
}
