package com.example.thechair.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.util.List;
import java.util.Map;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> servicesList;
    private OnServiceClickListener listener;

    public interface OnServiceClickListener {
        void onServiceClick(Map<String, Object> service);
    }

    public void setOnServiceClickListener(OnServiceClickListener listener) {
        this.listener = listener;
    }

    public ServiceAdapter(Context context, List<Map<String, Object>> servicesList) {
        this.context = context;
        this.servicesList = servicesList;
    }

    @NonNull
    @Override
    public ServiceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_service, parent, false);
        return new ServiceAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceAdapter.ViewHolder holder, int position) {
        Map<String, Object> service = servicesList.get(position);

        String name = (String) service.get("name");
        Object price = service.get("price");
        Object duration = service.get("duration");
        String description = (String) service.get("description");

        holder.serviceName.setText(name != null ? name : "Service");
        holder.servicePrice.setText(price != null ? "$" + price : "");
        holder.serviceDuration.setText(duration != null ? duration + " mins" : "");
        holder.serviceDescription.setText(description != null ? description : "");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onServiceClick(service);
        });
    }

    @Override
    public int getItemCount() {
        return servicesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView serviceName, servicePrice, serviceDuration, serviceDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceName = itemView.findViewById(R.id.serviceName);
            servicePrice = itemView.findViewById(R.id.servicePrice);
            serviceDuration = itemView.findViewById(R.id.serviceDuration);
            serviceDescription = itemView.findViewById(R.id.serviceDescription);
        }
    }
}
