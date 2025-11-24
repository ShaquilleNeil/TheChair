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

    private final List<String> slots;
    private final OnTimeSlotClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION; // highlight support

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String time = slots.get(position);
        holder.tvTimeSlot.setText(time);


        holder.itemView.setBackgroundResource(
                selectedPosition == position ?
                        R.drawable.timeslot_selected_background :
                        R.drawable.timeslot_background
        );

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            listener.onTimeSelected(time);
        });
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeSlot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
        }
    }
}
