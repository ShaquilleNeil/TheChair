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

        if(convertView == null) {
            convertView = View.inflate(context, R.layout.customer_edit_profile_activity, null);
        }

        appUsers appusers = appUsersList.get(position);

        ImageView imageView = convertView.findViewById(R.id.profileImage);
        ImageButton btnChangePhoto = convertView.findViewById(R.id.btnChangePhoto);
        EditText inputName = convertView.findViewById(R.id.profileName);
        EditText inputEmail = convertView.findViewById(R.id.profileEmail);
        EditText inputPhone = convertView.findViewById(R.id.profilePhone);
        EditText inputAddress1 = convertView.findViewById(R.id.profileAddress);
        EditText inputAddress2 = convertView.findViewById(R.id.profileAddress);
        EditText inputCity = convertView.findViewById(R.id.profileAddress);
        EditText inputProvince = convertView.findViewById(R.id.profileAddress);
        EditText inputPostalCode = convertView.findViewById(R.id.profileAddress);
       Button editProfile = convertView.findViewById(R.id.editProfileButton);


        inputName.setText(appusers.getName());
        inputEmail.setText(appusers.getEmail());
        inputPhone.setText(appusers.getPhoneNumber());
        inputAddress1.setText(appusers.getAddress().getStreet());
        inputAddress2.setText(appusers.getAddress().getRoom());
        inputCity.setText(appusers.getAddress().getCity());
        inputProvince.setText(appusers.getAddress().getProvince());
        inputPostalCode.setText(appusers.getAddress().getPostalCode());


        editProfile.setOnClickListener( v -> {

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
