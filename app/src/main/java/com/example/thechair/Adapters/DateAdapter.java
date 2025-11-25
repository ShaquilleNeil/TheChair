// Shaq’s Notes:
// This adapter displays a horizontal list of dates for booking selection.
// Each item shows:
// - the short name of the weekday (Mon, Tue, etc.)
// - the numeric day of the month
//
// When a user taps a date, the adapter:
// - updates the selected position,
// - notifies the listener (used by the booking screen to load availability),
// - refreshes the UI so the selected date gets visually highlighted.
//
// Uses LocalDate (API 26+), so checks are in place for SDK compatibility.

package com.example.thechair.Adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    private final List<LocalDate> dates;          // list of dates to display
    private int selectedPosition;                 // currently selected item
    private final OnDateClickListener onDateClickListener;

    public DateAdapter(List<LocalDate> dates, OnDateClickListener onDateClickListener) {
        this.dates = dates;
        this.onDateClickListener = onDateClickListener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate one date item card
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);

        return new DateViewHolder((ViewGroup) view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {

        LocalDate date = dates.get(position);

        // -------------------- DAY OF WEEK NAME (Mon, Tue...) --------------------
        String dayOfWeek = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dayOfWeek = date.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault());
        }
        holder.txtDayOfWeek.setText(dayOfWeek);

        // -------------------- DAY NUMBER (1–31) --------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.txtDayNumber.setText(String.valueOf(date.getDayOfMonth()));
        }

        // Highlight if this item is selected
        holder.itemView.setSelected(position == selectedPosition);

        // -------------------- CLICK HANDLER --------------------
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;              // update selected date
            onDateClickListener.onDateClick(date);    // notify parent screen
            notifyDataSetChanged();                   // refresh all items
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    // Listener interface for parent activity/fragment
    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    // -------------------- VIEW HOLDER --------------------
    static class DateViewHolder extends RecyclerView.ViewHolder {

        TextView txtDayOfWeek, txtDayNumber;

        public DateViewHolder(@NonNull ViewGroup parent) {
            super(parent);

            txtDayOfWeek = parent.findViewById(R.id.txtDayOfWeek);
            txtDayNumber = parent.findViewById(R.id.txtDayNumber);
        }
    }
}
