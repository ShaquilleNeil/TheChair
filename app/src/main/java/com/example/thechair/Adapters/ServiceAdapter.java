// Shaq’s Notes:
// This adapter displays a list of services offered by a professional.
// Each service item shows:
// - service name,
// - service price,
// - service duration.
//
// Every service comes from a Firestore Map<String,Object>, not a typed model,
// because services in appUsers are stored as raw objects in the Firestore user doc.
//
// When the user taps on a service, a callback returns the full service Map so
// the parent screen can open a booking flow or show more details.

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

    private final Context context;
    private final List<Map<String, Object>> servicesList; // each service = Firestore map

    // Click listener for selecting a service
    private OnServiceClickListener listener;

    public interface OnServiceClickListener {
        void onServiceClick(Map<String, Object> service);
    }

    public void setOnServiceClickListener(OnServiceClickListener listener) {
        this.listener = listener;
    }

    // Constructor
    public ServiceAdapter(Context context, List<Map<String, Object>> servicesList) {
        this.context = context;
        this.servicesList = servicesList;
    }

    @NonNull
    @Override
    public ServiceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflate one service row item
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_service, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceAdapter.ViewHolder holder, int position) {

        Map<String, Object> service = servicesList.get(position);

        // Extract fields safely from raw Map objects
        String name = (String) service.get("name");
        Object price = service.get("price");           // might be Long or Double
        Object duration = service.get("duration");     // same item type issue
        String description = (String) service.get("description");

        // Bind values to UI labels
        holder.serviceName.setText(name != null ? name : "Service");

        holder.servicePrice.setText(
                price != null ? "$" + price.toString() : ""
        );

        holder.serviceDuration.setText(
                duration != null ? duration.toString() + " mins" : ""
        );

        // Click event → parent handles booking or details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onServiceClick(service);
        });
    }

    @Override
    public int getItemCount() {
        return servicesList.size();
    }

    // -------------------- VIEW HOLDER --------------------
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView serviceName, servicePrice, serviceDuration, serviceDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            serviceName = itemView.findViewById(R.id.serviceName);
            servicePrice = itemView.findViewById(R.id.servicePrice);
            serviceDuration = itemView.findViewById(R.id.serviceDuration);

            // Note: serviceDescription exists in layout but isn't used here
        }
    }
}
