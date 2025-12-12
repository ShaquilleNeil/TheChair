// Shaq’s Notes:
// This adapter displays a list of bookings for a *customer*. Each item shows:
// - the service name,
// - the professional's name,
// - date + time,
// - the professional’s profile image,
// - and the booking status badge.
//
// When a user taps a booking, the adapter opens BookingDetailsFragment and
// passes all booking details via a Bundle. Glide is used to load the pro’s
// profile image. Status dynamically changes the background badge.

package com.example.thechair.Adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.thechair.Customer.BookingDetailsFragment;
import com.example.thechair.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final Context context;
    private final List<Booking> bookingList;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the card layout for a single booking
        View view = LayoutInflater.from(context).inflate(R.layout.cus_bookings_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Booking booking = bookingList.get(position);

        // Bind main booking details to text fields
        holder.tvServiceName.setText(booking.getServiceName());
//        holder.tvProName.setText(booking.getProfessionalName());
        holder.tvDate.setText(booking.getSelectedDate());
        holder.tvTime.setText(booking.getServiceTime());


        String proId = booking.getProfessionalId();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(proId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    String liveName = doc.getString("name");
                    String livePic  = doc.getString("profilepic");

                    holder.tvProName.setText(liveName);

                    Glide.with(context)
                            .load(livePic)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_person)
                            .into(holder.imgProPic);

                });
        // -------------------- CLICK → OPEN BOOKING DETAILS SCREEN --------------------
        holder.itemView.setOnClickListener(v -> {

            // Always get the updated position in case items shift
            Booking b = bookingList.get(holder.getAdapterPosition());

            Fragment fragment = new BookingDetailsFragment();

            // Pass all booking details to the details fragment
            Bundle bundle = new Bundle();
            bundle.putString("bookingId", b.getBookingId());
            bundle.putString("professionalId", b.getProfessionalId());
            bundle.putString("professionalName", b.getProfessionalName());
            bundle.putString("profilePic", b.getProPic());

            // Service info
            bundle.putString("serviceName", b.getServiceName());
            bundle.putString("selectedDate", b.getSelectedDate());
            bundle.putString("serviceTime", b.getServiceTime());
            bundle.putString("endTime", b.getEndTime());



            // Numeric values
            bundle.putInt("serviceDuration", b.getServiceDuration());
            bundle.putInt("servicePrice", b.getServicePrice());

            // Status
            bundle.putString("status", b.getStatus());

            // -------------------- ADDRESS --------------------



            fragment.setArguments(bundle);

            // Replace the current screen with BookingDetailsFragment
            ((AppCompatActivity) context).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.appMainView, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // -------------------- LOAD PROFILE IMAGE USING GLIDE --------------------
//        if (booking.getProPic() != null && !booking.getProPic().isEmpty()) {
//            Glide.with(context)
//                    .load(booking.getProPic())
//                    .placeholder(R.drawable.ic_person)
//                    .into(holder.imgProPic);
//        } else {
//            holder.imgProPic.setImageResource(R.drawable.ic_person);
//        }

        // -------------------- STATUS BADGE --------------------
        String status = booking.getStatus();
        holder.tvStatus.setText(status);

        switch (status.toLowerCase()) {
            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                break;

            case "confirmed":
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_confirmed);
                break;

            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_confirmed);
                break;

            case "cancelled":
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_cancelled);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size(); // number of bookings to display
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProPic;
        TextView tvServiceName, tvProName, tvDate, tvTime, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProPic = itemView.findViewById(R.id.imgProPic);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvProName = itemView.findViewById(R.id.tvProName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
