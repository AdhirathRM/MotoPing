package com.example.motoping;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private List<Vehicle> vehicleList;

    public VehicleAdapter(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicleList.get(position);

        holder.textVehicleName.setText(vehicle.getName());
        holder.textInsurance.setText(vehicle.getInsuranceExpiry());
        holder.textService.setText(vehicle.getServiceDueDate());
        holder.textPuc.setText(vehicle.getPucDueDate());
        holder.textRc.setText(vehicle.getRcExpiry());

        String type = vehicle.getType();
        if (type != null) {
            if (type.equals("Bike")) {
                holder.iconVehicleType.setImageResource(R.drawable.ic_bike);
            } else if (type.equals("Scooter")) {
                holder.iconVehicleType.setImageResource(R.drawable.ic_scooter);
            } else {
                holder.iconVehicleType.setImageResource(R.drawable.ic_car);
            }
        }

        if (vehicle.getColorHex() != null && !vehicle.getColorHex().isEmpty()) {
            int cardColor = Color.parseColor(vehicle.getColorHex());
            holder.accentBar.setBackgroundColor(cardColor);
            holder.iconVehicleType.setColorFilter(cardColor);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EditVehicleActivity.class);
            intent.putExtra("ID", vehicle.getId()); // ID is now safely a String
            intent.putExtra("NAME", vehicle.getName());
            intent.putExtra("INSURANCE", vehicle.getInsuranceExpiry());
            intent.putExtra("SERVICE", vehicle.getServiceDueDate());
            intent.putExtra("PUC", vehicle.getPucDueDate());
            intent.putExtra("RC", vehicle.getRcExpiry());
            intent.putExtra("COLOR", vehicle.getColorHex());
            intent.putExtra("TYPE", vehicle.getType());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    public Vehicle getVehicleAt(int position) {
        return vehicleList.get(position);
    }

    // We removed the local removeVehicle() method because Firestore's real-time listener
    // handles deleting elements from the UI automatically now!

    public static class VehicleViewHolder extends RecyclerView.ViewHolder {
        TextView textVehicleName, textInsurance, textService, textPuc, textRc;
        ImageView iconVehicleType;
        View accentBar;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            textVehicleName = itemView.findViewById(R.id.textVehicleName);
            textInsurance = itemView.findViewById(R.id.textInsurance);
            textService = itemView.findViewById(R.id.textService);
            textPuc = itemView.findViewById(R.id.textPuc);
            textRc = itemView.findViewById(R.id.textRc);
            iconVehicleType = itemView.findViewById(R.id.iconVehicleType);
            accentBar = itemView.findViewById(R.id.accentBar);
        }
    }
}