// Shaq's Notes:
// This adapter takes a list of Booking objects and displays them in the
// professional’s appointment list. It updates UI elements based on booking
// status (pending / accepted / completed / rejected) and also pushes status
// updates back to Firestore in both the global "bookings" collection and the
// professional's own subtcollection under "Users/{proId}/bookings". Everything
// shown in each card — buttons, colors, visibility — reacts to that status.

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

    private final List<Booking> bookings; // Stores all appointments to be displayed

    public AppointmentAdapter(List<Booking> bookings) {
        this.bookings = bookings;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Shaq's Notes: Inflate one appointment card layout.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        // Shaq's Notes: Grab the booking for this specific card.
        Booking b = bookings.get(position);

        // Bind the appointment details to UI
        holder.txtTime.setText(b.serviceTime + " - " + b.endTime);
        holder.txtClientName.setText("Client: " + b.customerName);
        holder.txtService.setText("Service: " + b.serviceName);
        holder.txtStatus.setText("Status: " + b.status);

        // Firebase references used for status updates
        String proId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Shaq's Notes:
        // Each status determines color AND which action buttons appear.
        switch (b.status) {
            case "pending":
                holder.txtStatus.setTextColor(Color.parseColor("#FACC15")); // yellow
                holder.pendingButtons.setVisibility(View.VISIBLE); // Accept / Reject visible
                holder.btnDone.setVisibility(View.GONE);
                break;

            case "accepted":
                holder.txtStatus.setTextColor(Color.parseColor("#3B82F6")); // blue
                holder.pendingButtons.setVisibility(View.GONE); // Only "Done" visible
                holder.btnDone.setVisibility(View.VISIBLE);
                break;

            case "completed":
                holder.txtStatus.setTextColor(Color.parseColor("#16A34A")); // green
                holder.pendingButtons.setVisibility(View.GONE);
                holder.btnDone.setVisibility(View.GONE);
                break;

            default:
                // rejected, cancelled, anything else
                holder.txtStatus.setTextColor(Color.parseColor("#DC2626")); // red
                holder.pendingButtons.setVisibility(View.GONE);
                holder.btnDone.setVisibility(View.GONE);
                break;
        }

        // Shaq's Notes:
        // Every button press updates Firestore AND immediately refreshes the UI card.

        // Accept booking
        holder.btnAccept.setOnClickListener(v -> {
            updateStatus(db, proId, b, "accepted");
            b.status = "accepted"; // Update local object
            notifyItemChanged(position); // Refresh the card visually
            Toast.makeText(v.getContext(), "Booking accepted", Toast.LENGTH_SHORT).show();
        });

        // Reject booking
        holder.btnReject.setOnClickListener(v -> {
            updateStatus(db, proId, b, "rejected");
            b.status = "rejected";
            notifyItemChanged(position);
            Toast.makeText(v.getContext(), "Booking rejected", Toast.LENGTH_SHORT).show();
        });

        // Mark as completed
        holder.btnDone.setOnClickListener(v -> {
            updateStatus(db, proId, b, "completed");
            b.status = "completed";
            notifyItemChanged(position);
            Toast.makeText(v.getContext(), "Marked as completed", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size(); // total appointments in the list
    }

    // Shaq's Notes:
    // This method pushes the new status to *two* Firestore locations.
    private void updateStatus(FirebaseFirestore db, String proId, Booking b, String status) {
        // Global booking reference
        db.collection("bookings")
                .document(b.bookingId)
                .update("status", status);

        // Professional’s local sub-collection reference
        db.collection("Users")
                .document(proId)
                .collection("bookings")
                .document(b.bookingId)
                .update("status", status);
    }

    // ViewHolder: Holds references to all UI elements in one card.
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
