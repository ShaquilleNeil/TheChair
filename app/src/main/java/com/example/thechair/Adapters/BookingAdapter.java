package com.example.thechair.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thechair.R;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {
    private Context context;
    private List<Booking> bookingList;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }






    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cus_bookings_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.tvServiceName.setText(booking.getServiceName());
        holder.tvProName.setText(booking.getProfessionalName());
        holder.tvDate.setText(booking.getSelectedDate());
        holder.tvTime.setText(booking.getServiceTime());


        if (booking.getProPic() != null && !booking.getProPic().isEmpty()) {
            Glide.with(context)
                    .load(booking.getProPic())
                    .placeholder(R.drawable.ic_person)
                    .into(holder.imgProPic);
        } else {
            holder.imgProPic.setImageResource(R.drawable.ic_person);
        }

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
        return bookingList.size();
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
