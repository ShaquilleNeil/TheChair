// Shaq’s Notes:
// This adapter displays a list of time slots for booking.
// Each row shows one time (e.g., "2:00 PM") and allows the user to choose it.
// The selected slot gets a special background for visual highlighting.
//
// Key points:
// - The parent fragment passes a listener to receive the selected time.
// - selectedPosition tracks which slot is currently chosen.
// - notifyDataSetChanged() refreshes all rows so only one stays highlighted.

package com.example.thechair.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {

    private final List<String> slots;                   // list of available times
    private final OnTimeSlotClickListener listener;     // callback for selection
    private int selectedPosition = RecyclerView.NO_POSITION; // track selected time

    // Listener interface for parent component
    public interface OnTimeSlotClickListener {
        void onTimeSelected(String time);
    }

    public TimeSlotAdapter(List<String> slots, OnTimeSlotClickListener listener) {
        this.slots = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflate a single time slot row layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String time = slots.get(position);

        // Display the time text
        holder.tvTimeSlot.setText(time);

        // Highlight selected slot visually
        holder.itemView.setBackgroundResource(
                selectedPosition == position
                        ? R.drawable.timeslot_selected_background
                        : R.drawable.timeslot_background
        );

        // Handle clicking a slot
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;   // update selected state
            notifyDataSetChanged();        // refresh all items
            listener.onTimeSelected(time); // notify parent
        });
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    // -------------------- VIEW HOLDER --------------------
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTimeSlot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
        }
    }
}
