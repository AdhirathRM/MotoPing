package com.example.motoping;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class VehicleDetailActivity extends AppCompatActivity {

    private String vehicleId;
    private String vehicleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vehicle_detail);

        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        CircularProgressIndicator vaultHealthRing = findViewById(R.id.vaultHealthRing);
        ImageView vaultVehicleIcon = findViewById(R.id.vaultVehicleIcon);
        TextView vaultHealthScore = findViewById(R.id.vaultHealthScore);
        TextView vaultVehicleName = findViewById(R.id.vaultVehicleName);

        TextView vaultInsurance = findViewById(R.id.vaultInsurance);
        TextView vaultService = findViewById(R.id.vaultService);
        TextView vaultPuc = findViewById(R.id.vaultPuc);
        TextView vaultRc = findViewById(R.id.vaultRc);

        Button btnServiceHistory = findViewById(R.id.btnServiceHistory);
        Button btnEditVehicle = findViewById(R.id.btnEditVehicle);

        Intent intent = getIntent();
        if (intent == null) return;

        vehicleId = intent.getStringExtra("ID");
        vehicleName = intent.getStringExtra("NAME");
        String insurance = intent.getStringExtra("INSURANCE");
        String service = intent.getStringExtra("SERVICE");
        String puc = intent.getStringExtra("PUC");
        String rc = intent.getStringExtra("RC");
        String colorHex = intent.getStringExtra("COLOR");
        String type = intent.getStringExtra("TYPE");

        vaultVehicleName.setText(vehicleName);
        vaultInsurance.setText("Insurance: " + (insurance != null && !insurance.isEmpty() ? insurance : "--/--/----"));
        vaultService.setText("Service: " + (service != null && !service.isEmpty() ? service : "--/--/----"));
        vaultPuc.setText("PUC: " + (puc != null && !puc.isEmpty() ? puc : "--/--/----"));
        vaultRc.setText("RC: " + (rc != null && !rc.isEmpty() ? rc : "--/--/----"));

        if ("Bike".equals(type)) vaultVehicleIcon.setImageResource(R.drawable.ic_bike);
        else if ("Scooter".equals(type)) vaultVehicleIcon.setImageResource(R.drawable.ic_scooter);
        else vaultVehicleIcon.setImageResource(R.drawable.ic_car);

        if (colorHex != null && !colorHex.isEmpty()) {
            vaultVehicleIcon.setColorFilter(Color.parseColor(colorHex));
        }

        Vehicle tempVehicle = new Vehicle(vehicleId, vehicleName, insurance, service, puc, rc, colorHex, type);
        int healthScore = tempVehicle.calculateHealthScore();

        vaultHealthRing.setProgressCompat(healthScore, true);
        vaultHealthScore.setText(healthScore + "%");

        if (healthScore >= 75) {
            vaultHealthRing.setIndicatorColor(Color.parseColor("#00E676"));
            vaultHealthScore.setTextColor(Color.parseColor("#00E676"));
        } else if (healthScore >= 40) {
            vaultHealthRing.setIndicatorColor(Color.parseColor("#FFCA28"));
            vaultHealthScore.setTextColor(Color.parseColor("#FFCA28"));
        } else {
            vaultHealthRing.setIndicatorColor(Color.parseColor("#FF1744"));
            vaultHealthScore.setTextColor(Color.parseColor("#FF1744"));
        }

        // --- NEW: Route to Service History ---
        btnServiceHistory.setOnClickListener(v -> {
            Intent historyIntent = new Intent(VehicleDetailActivity.this, ServiceHistoryActivity.class);
            historyIntent.putExtra("VEHICLE_ID", vehicleId);
            historyIntent.putExtra("VEHICLE_NAME", vehicleName);
            startActivity(historyIntent);
        });

        btnEditVehicle.setOnClickListener(v -> {
            Intent editIntent = new Intent(VehicleDetailActivity.this, EditVehicleActivity.class);
            editIntent.putExtras(intent);
            startActivity(editIntent);
            finish();
        });

        Button btnGlovebox = findViewById(R.id.btnGlovebox);

        btnGlovebox.setOnClickListener(v -> {
            Intent gloveboxIntent = new Intent(VehicleDetailActivity.this, GloveboxActivity.class);
            gloveboxIntent.putExtra("VEHICLE_ID", vehicleId);
            startActivity(gloveboxIntent);
        });
    }
}