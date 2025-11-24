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
    private final List<LocalDate> dates;
    private int selectedPosition;
    private final OnDateClickListener onDateClickListener;

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date, parent, false);

        return new DateViewHolder((ViewGroup) view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        LocalDate date = dates.get(position);

        String dayOfWeek = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        }
        holder.txtDayOfWeek.setText(dayOfWeek);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.txtDayNumber.setText(String.valueOf(date.getDayOfMonth()));
        }

        holder.itemView.setSelected((position == selectedPosition));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedPosition = position;
                onDateClickListener.onDateClick(date);
                notifyDataSetChanged();
            }
        });




    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    public DateAdapter(List<LocalDate> dates, OnDateClickListener onDateClickListener) {
        this.dates = dates;
        this.onDateClickListener = onDateClickListener;
    }









    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView txtDayOfWeek, txtDayNumber;

        public DateViewHolder(@NonNull ViewGroup parent) {
            super(parent);
            txtDayOfWeek = parent.findViewById(R.id.txtDayOfWeek);
            txtDayNumber = parent.findViewById(R.id.txtDayNumber);

        }
    }

}
