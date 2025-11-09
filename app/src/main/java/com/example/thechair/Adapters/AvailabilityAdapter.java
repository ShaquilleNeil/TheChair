package com.example.thechair.Adapters;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.util.List;
import java.util.Locale;

public class AvailabilityAdapter extends RecyclerView.Adapter<AvailabilityAdapter.ViewHolder> {

    private final Context context;
    private final List<Availability> days;

    public AvailabilityAdapter(Context context, List<Availability> days) {
        this.context = context;
        this.days = days;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_workday, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Availability day = days.get(position);

        holder.txtDayName.setText(capitalize(day.getDay()));
        holder.switchAvailable.setChecked(day.isAvailable());
        holder.txtStartTime.setText(day.getStart());
        holder.txtEndTime.setText(day.getEnd());

        // Disable time fields when unavailable
        holder.txtStartTime.setEnabled(day.isAvailable());
        holder.txtEndTime.setEnabled(day.isAvailable());
        holder.txtStartTime.setAlpha(day.isAvailable() ? 1f : 0.4f);
        holder.txtEndTime.setAlpha(day.isAvailable() ? 1f : 0.4f);

        // Switch toggles availability
        holder.switchAvailable.setOnCheckedChangeListener((button, checked) -> {
            day.setAvailable(checked);
            holder.txtStartTime.setEnabled(checked);
            holder.txtEndTime.setEnabled(checked);
            holder.txtStartTime.setAlpha(checked ? 1f : 0.4f);
            holder.txtEndTime.setAlpha(checked ? 1f : 0.4f);

            if (!checked) {
                day.setStart("--");
                day.setEnd("--");
                holder.txtStartTime.setText("--");
                holder.txtEndTime.setText("--");
            } else {
                if (day.getStart() == null || day.getStart().equals("--"))
                    day.setStart("09:00");
                if (day.getEnd() == null || day.getEnd().equals("--"))
                    day.setEnd("17:00");
                holder.txtStartTime.setText(day.getStart());
                holder.txtEndTime.setText(day.getEnd());
            }
        });

        // Time pickers
        holder.txtStartTime.setOnClickListener(v -> showTimePicker(holder.txtStartTime, day, true));
        holder.txtEndTime.setOnClickListener(v -> showTimePicker(holder.txtEndTime, day, false));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private void showTimePicker(TextView textView, Availability day, boolean isStart) {
        int hour = 9, minute = 0;
        try {
            if (!textView.getText().toString().equals("--")) {
                String[] parts = textView.getText().toString().split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}

        TimePickerDialog dialog = new TimePickerDialog(
                context,
                (view, h, m) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                    textView.setText(time);
                    if (isStart) day.setStart(time);
                    else day.setEnd(time);
                },
                hour, minute, true
        );
        dialog.show();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDayName, txtStartTime, txtEndTime;
        Switch switchAvailable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDayName = itemView.findViewById(R.id.txtDayName);
            txtStartTime = itemView.findViewById(R.id.txtStartTime);
            txtEndTime = itemView.findViewById(R.id.txtEndTime);
            switchAvailable = itemView.findViewById(R.id.switchAvailable);
        }
    }
}
