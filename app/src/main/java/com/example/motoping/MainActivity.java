package com.example.motoping;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView vehicleRecyclerView;
    private VehicleAdapter adapter;
    private EditText searchEditText;
    private Button btnFilterAll, btnFilterCars, btnFilterBikes, btnFilterScooters;

    private String currentFilterType = "ALL";
    private String currentSearchQuery = "";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Vehicle> masterCloudList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        vehicleRecyclerView = findViewById(R.id.vehicleRecyclerView);
        vehicleRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchEditText = findViewById(R.id.searchEditText);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterCars = findViewById(R.id.btnFilterCars);
        btnFilterBikes = findViewById(R.id.btnFilterBikes);
        btnFilterScooters = findViewById(R.id.btnFilterScooters);

        FloatingActionButton addVehicleFab = findViewById(R.id.addVehicleFab);
        addVehicleFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddVehicleActivity.class);
            startActivity(intent);
        });

        // UPGRADED: Load Profile Picture into Header
        ImageView btnProfile = findViewById(R.id.btnProfile);
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getPhotoUrl() != null) {
            String highResUrl = mAuth.getCurrentUser().getPhotoUrl().toString().replace("s96-c", "s150-c");
            Glide.with(this).load(highResUrl).circleCrop().into(btnProfile);
        }
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applyFiltersToRecyclerView();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilterAll.setOnClickListener(v -> changeFilterMode("ALL", btnFilterAll));
        btnFilterCars.setOnClickListener(v -> changeFilterMode("Car", btnFilterCars));
        btnFilterBikes.setOnClickListener(v -> changeFilterMode("Bike", btnFilterBikes));
        btnFilterScooters.setOnClickListener(v -> changeFilterMode("Scooter", btnFilterScooters));

        setupSwipeToDelete();
        requestNotificationPermission();
        startBackgroundEngine();
        attachCloudListener();
    }

    private void attachCloudListener() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("vehicles")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("Firestore", "Listen failed.", error);
                        return;
                    }
                    masterCloudList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Vehicle vehicle = doc.toObject(Vehicle.class);
                            if (vehicle != null) {
                                masterCloudList.add(vehicle);
                            }
                        }
                    }
                    applyFiltersToRecyclerView();
                });
    }

    private void changeFilterMode(String type, Button selectedButton) {
        currentFilterType = type;
        btnFilterAll.setTextColor(Color.parseColor("#7A7A7A"));
        btnFilterCars.setTextColor(Color.parseColor("#7A7A7A"));
        btnFilterBikes.setTextColor(Color.parseColor("#7A7A7A"));
        btnFilterScooters.setTextColor(Color.parseColor("#7A7A7A"));

        selectedButton.setTextColor(Color.parseColor("#FFFFFF"));
        applyFiltersToRecyclerView();
    }

    private void applyFiltersToRecyclerView() {
        List<Vehicle> filteredList = new ArrayList<>();

        for (Vehicle v : masterCloudList) {
            boolean matchesType = currentFilterType.equals("ALL") || (v.getType() != null && v.getType().equalsIgnoreCase(currentFilterType));
            boolean matchesQuery = v.getName() != null && v.getName().toLowerCase().contains(currentSearchQuery);

            if (matchesType && matchesQuery) {
                filteredList.add(v);
            }
        }

        adapter = new VehicleAdapter(filteredList);
        vehicleRecyclerView.setAdapter(adapter);

        TextView emptyStateText = findViewById(R.id.emptyStateText);
        if (filteredList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void startBackgroundEngine() {
        PeriodicWorkRequest alertRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("MotoPingDailyCheck", ExistingPeriodicWorkPolicy.KEEP, alertRequest);
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Vehicle swipedVehicle = adapter.getVehicleAt(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Vehicle")
                        .setMessage("Are you sure you want to delete '" + swipedVehicle.getName() + "'? This will permanently remove it from the cloud.")
                        .setCancelable(false)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (mAuth.getCurrentUser() != null) {
                                String userId = mAuth.getCurrentUser().getUid();
                                db.collection("users").document(userId).collection("vehicles").document(swipedVehicle.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Deleted from Cloud", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error deleting", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                View itemView = viewHolder.itemView;
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#FF1744"));
                paint.setAntiAlias(true);
                float radius = 16 * itemView.getContext().getResources().getDisplayMetrics().density;
                Drawable deleteIcon = ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_delete);

                if (deleteIcon != null) {
                    deleteIcon.setTint(Color.WHITE);
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                    c.save();
                    if (dX > 0) {
                        c.clipRect(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                        c.drawRoundRect(new RectF(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom()), radius, radius, paint);
                        int iconLeft = itemView.getLeft() + iconMargin;
                        int iconRight = itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth();
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(c);
                    } else if (dX < 0) {
                        c.clipRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                        c.drawRoundRect(new RectF(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom()), radius, radius, paint);
                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(c);
                    }
                    c.restore();
                }
            }
        }).attachToRecyclerView(vehicleRecyclerView);
    }
}