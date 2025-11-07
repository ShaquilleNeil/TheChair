package com.example.thechair.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.util.List;


public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<HairAppointment> appointments;

    public AppointmentAdapter(List<HairAppointment> appointments) {
        this.appointments = appointments;
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
        HairAppointment a = appointments.get(position);

        // Bind text values
        holder.txtTime.setText(a.getTime());
        holder.txtClientName.setText("Client: " + a.getClientName());
        holder.txtService.setText(a.getServiceType());
        holder.txtNotes.setText("Notes: " + a.getNotes());

        // "Mark as done" button logic
        holder.btnDone.setOnClickListener(v -> {
            a.setCompleted(true);
            Toast.makeText(v.getContext(), "Marked as done!", Toast.LENGTH_SHORT).show();
            holder.btnDone.setEnabled(false);
            holder.btnDone.setText("Completed");
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView txtService, txtClientName, txtTime, txtNotes;
        Button btnDone;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtService = itemView.findViewById(R.id.txtService);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtNotes = itemView.findViewById(R.id.txtNotes);
            btnDone = itemView.findViewById(R.id.btnDone);
        }
    }
}
