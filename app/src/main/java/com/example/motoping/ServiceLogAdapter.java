package com.example.motoping;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ServiceLogAdapter extends RecyclerView.Adapter<ServiceLogAdapter.LogViewHolder> {

    private List<ServiceLog> logList;
    private String vehicleId;

    public ServiceLogAdapter(List<ServiceLog> logList, String vehicleId) {
        this.logList = logList;
        this.vehicleId = vehicleId;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        ServiceLog log = logList.get(position);
        holder.textLogTitle.setText(log.getTitle());
        holder.textLogDate.setText(log.getDate());

        if (log.getCost() != null && !log.getCost().isEmpty()) {
            holder.textLogCost.setText("₹" + log.getCost());
            holder.textLogCost.setVisibility(View.VISIBLE);
        } else {
            holder.textLogCost.setVisibility(View.GONE);
        }

        if (log.getNotes() != null && !log.getNotes().isEmpty()) {
            holder.textLogNotes.setText(log.getNotes());
            holder.textLogNotes.setVisibility(View.VISIBLE);
        } else {
            holder.textLogNotes.setVisibility(View.GONE);
        }

        // --- NEW: Click to Edit ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AddServiceLogActivity.class);
            intent.putExtra("VEHICLE_ID", vehicleId);
            intent.putExtra("LOG_ID", log.getId());
            intent.putExtra("TITLE", log.getTitle());
            intent.putExtra("DATE", log.getDate());
            intent.putExtra("COST", log.getCost());
            intent.putExtra("NOTES", log.getNotes());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView textLogTitle, textLogDate, textLogCost, textLogNotes;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            textLogTitle = itemView.findViewById(R.id.textLogTitle);
            textLogDate = itemView.findViewById(R.id.textLogDate);
            textLogCost = itemView.findViewById(R.id.textLogCost);
            textLogNotes = itemView.findViewById(R.id.textLogNotes);
        }
    }
}