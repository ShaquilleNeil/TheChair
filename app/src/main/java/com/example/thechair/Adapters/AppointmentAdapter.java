package com.example.thechair.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Booking> bookings;

    public AppointmentAdapter(List<Booking> bookings) {
        this.bookings = bookings;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Booking b = bookings.get(position);

        holder.txtTime.setText(b.serviceTime + " - " + b.endTime);
        holder.txtClientName.setText("Client: " + b.customerName);
        holder.txtService.setText("Service: " + b.serviceName);
        holder.txtStatus.setText("Status: " + b.status);

        String proId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ---------------- COLOR STATUS ----------------
        switch (b.status) {
            case "pending":
                holder.txtStatus.setTextColor(Color.parseColor("#FACC15")); // Yellow
                holder.pendingButtons.setVisibility(View.VISIBLE);
                holder.btnDone.setVisibility(View.GONE);
                break;

            case "accepted":
                holder.txtStatus.setTextColor(Color.parseColor("#3B82F6")); // Blue
                holder.pendingButtons.setVisibility(View.GONE);
                holder.btnDone.setVisibility(View.VISIBLE);
                break;

            case "completed":
                holder.txtStatus.setTextColor(Color.parseColor("#16A34A")); // Green
                holder.pendingButtons.setVisibility(View.GONE);
                holder.btnDone.setVisibility(View.GONE);
                break;

            default: // rejected / cancelled
                holder.txtStatus.setTextColor(Color.parseColor("#DC2626")); // Red
                holder.pendingButtons.setVisibility(View.GONE);
                holder.btnDone.setVisibility(View.GONE);
                break;
        }

        // ---------------- BUTTON ACTIONS ----------------

        holder.btnAccept.setOnClickListener(v -> {
            updateStatus(db, proId, b, "accepted");
            b.status = "accepted";
            notifyItemChanged(position);
            Toast.makeText(v.getContext(), "Booking accepted", Toast.LENGTH_SHORT).show();
        });

        holder.btnReject.setOnClickListener(v -> {
            updateStatus(db, proId, b, "rejected");
            b.status = "rejected";
            notifyItemChanged(position);
            Toast.makeText(v.getContext(), "Booking rejected", Toast.LENGTH_SHORT).show();
        });

        holder.btnDone.setOnClickListener(v -> {
            updateStatus(db, proId, b, "completed");
            b.status = "completed";
            notifyItemChanged(position);
            Toast.makeText(v.getContext(), "Marked as completed", Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public int getItemCount() {
        return bookings.size();
    }


    // 🔥 Update the booking status in both Firestore locations
    private void updateStatus(FirebaseFirestore db, String proId, Booking b, String status) {
        db.collection("bookings")
                .document(b.bookingId)
                .update("status", status);

        db.collection("Users")
                .document(proId)
                .collection("bookings")
                .document(b.bookingId)
                .update("status", status);
    }


    // -------------------- VIEW HOLDER --------------------
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView txtService, txtClientName, txtTime, txtStatus;
        Button btnDone, btnAccept, btnReject;
        LinearLayout pendingButtons;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);

            txtService = itemView.findViewById(R.id.txtService);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtStatus = itemView.findViewById(R.id.txtStatus);

            btnDone = itemView.findViewById(R.id.btnDone);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);

            pendingButtons = itemView.findViewById(R.id.pendingButtons);
        }
    }
}
