package com.example.thechair.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.thechair.R;

import java.util.List;
import java.util.Map;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    //Shaq's notes: This list holds all the professionals returned from Firestore Search.
    // Each item is a Map<String, Object> representing one user's document.
    private List<Map<String, Object>> data;

    //Shaq's notes: The constructor initializes the adapter with data (initially empty).
    public SearchAdapter(List<Map<String, Object>> data) {
        this.data = data;
    }

    //Shaq's notes: Called whenever new search results arrive from the repository.
    // It updates the RecyclerView by replacing the old list and refreshing the UI.
    public void updateData(List<Map<String, Object>> newData) {
        this.data = newData;
        android.util.Log.d("SearchAdapter", "🧾 Adapter updated with " + newData.size() + " results");
        notifyDataSetChanged();
    }

    //Shaq's notes: Called by RecyclerView to create a new ViewHolder object
    // when there are no existing ones that can be reused.
    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout file "item_search_result.xml" for each result card.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    //Shaq's notes: This binds data from Firestore (the "Map" object) to the UI components.
    @Override
    public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, int position) {

        Map<String, Object> item = data.get(position);

        String name = (String) item.get("name");
        String profilePic = (String) item.get("profilepic");

        holder.proName.setText(name != null ? name : "");

        // 🔹 Handle the "services" field gracefully (paste this part)
        Object servicesObj = item.get("services");

        if (servicesObj instanceof List) {
            // Case 1: Firestore returns an array of services
            List<?> servicesList = (List<?>) servicesObj;
            StringBuilder builder = new StringBuilder();

            for (Object serviceObj : servicesList) {
                if (serviceObj instanceof Map) {
                    Map<?, ?> serviceMap = (Map<?, ?>) serviceObj;
                    String serviceName = (String) serviceMap.get("name");
                    if (serviceName != null && !serviceName.isEmpty()) {
                        if (builder.length() > 0) builder.append(" • ");
                        builder.append(serviceName);
                    }
                }
            }

            holder.proServices.setText(
                    builder.length() > 0 ? builder.toString() : "No services listed"
            );

        } else if (servicesObj instanceof Map) {
            // Case 2: Firestore returns a single object
            Map<?, ?> serviceMap = (Map<?, ?>) servicesObj;
            String serviceName = (String) serviceMap.get("name");

            holder.proServices.setText(
                    serviceName != null && !serviceName.isEmpty()
                            ? serviceName
                            : "No services listed"
            );

        } else {
            // Case 3: No services field or invalid type
            holder.proServices.setText("No services listed");
        }


    }

    //Shaq's notes: Return the total number of items currently displayed in the list.
    @Override
    public int getItemCount() {
        return data.size();
    }

    //Shaq's notes: ViewHolder pattern — holds references to all UI components in a single card.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView proName, proServices;
        ImageView profileImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            //Shaq's notes: Connect each UI component with its view in item_search_result.xml.
            proName = itemView.findViewById(R.id.proName);
            proServices = itemView.findViewById(R.id.proServices);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
}
