package com.example.thechair.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.util.List;
import java.util.Map;

public class ProServiceAdapter extends RecyclerView.Adapter<ProServiceAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> servicesList;

    // Clicking the service (normal tap)
    private OnServiceClickListener serviceClickListener;

    // Deleting the service (delete button)
    public interface OnDeleteClickListener {
        void onDeleteClick(int position, Map<String,Object> service);
    }
    private OnDeleteClickListener deleteListener;

    public void setOnServiceClickListener(OnServiceClickListener listener) {
        this.serviceClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public interface OnServiceClickListener {
        void onServiceClick(Map<String, Object> service);
    }

    public ProServiceAdapter(Context context, List<Map<String, Object>> servicesList) {
        this.context = context;
        this.servicesList = servicesList;
    }

    @NonNull
    @Override
    public ProServiceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_pro_service, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProServiceAdapter.ViewHolder holder, int position) {

        Map<String, Object> service = servicesList.get(position);

        holder.serviceName.setText((String) service.get("name"));
        holder.servicePrice.setText("$" + service.get("price"));
        holder.serviceDuration.setText(service.get("duration") + " mins");

        // Normal click → open booking/details
        holder.itemView.setOnClickListener(v -> {
            if (serviceClickListener != null) {
                serviceClickListener.onServiceClick(service);
            }
        });

        // DELETE button
        holder.btnDeleteService.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(position, service);
            }
        });
    }

    @Override
    public int getItemCount() {
        return servicesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView serviceName, servicePrice, serviceDuration;
        Button btnDeleteService;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            serviceName = itemView.findViewById(R.id.serviceName);
            servicePrice = itemView.findViewById(R.id.servicePrice);
            serviceDuration = itemView.findViewById(R.id.serviceDuration);
            btnDeleteService = itemView.findViewById(R.id.btnDeleteService);
        }
    }
}
