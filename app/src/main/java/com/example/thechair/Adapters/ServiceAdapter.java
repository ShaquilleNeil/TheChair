package com.example.thechair.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thechair.R;

import java.util.ArrayList;



public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    Context context;
    ArrayList<Services> servicesList;

    public ServiceAdapter(Context context, ArrayList<Services>servicesList) {
        this.context = context;
        this.servicesList = servicesList;

    }

    @NonNull
    @Override
    public ServiceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.services_cards, parent, false);


        return new ServiceAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceAdapter.ViewHolder holder, int position) {
        //assign values to views
        Services services = servicesList.get(position);
        holder.serviceName.setText(services.getName());
        holder.servicePrice.setText("$" + services.getPrice());
        holder.serviceDuration.setText(String.valueOf(services.getDuration()) + " mins");


    }

    @Override
    public int getItemCount() {
        return servicesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView serviceName, servicePrice, serviceDuration;


        //grab names from the services_cards.xml
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceName = itemView.findViewById(R.id.serviceName);
            servicePrice = itemView.findViewById(R.id.servicePrice);
            serviceDuration = itemView.findViewById(R.id.serviceDuration);
        }
    }
}
