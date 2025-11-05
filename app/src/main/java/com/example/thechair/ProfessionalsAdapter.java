package com.example.thechair;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProfessionalsAdapter extends RecyclerView.Adapter<ProfessionalsAdapter.ViewHolder> {

    private Context mContext;
    private List<appUsers> mProfessionals;

    public ProfessionalsAdapter(Context context, List<appUsers> professionals) {
        mContext = context;
        mProfessionals = professionals;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.prof_info, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        appUsers professional = mProfessionals.get(position);

        holder.tvprovidername.setText(professional.getName());
//        holder.ivprovider.setImageResource(professional.getProfilepic());

    }

    @Override
    public int getItemCount() {
        return mProfessionals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivprovider;
        TextView tvprovidername;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivprovider = itemView.findViewById(R.id.ivprovider);
            tvprovidername = itemView.findViewById(R.id.tvprovidername);
        }
    }
}
