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
import android.view.View;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView vehicleRecyclerView;
    private VehicleAdapter adapter;
    private DatabaseHelper dbHelper;

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

        dbHelper = new DatabaseHelper(this);
        vehicleRecyclerView = findViewById(R.id.vehicleRecyclerView);
        vehicleRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton addVehicleFab = findViewById(R.id.addVehicleFab);
        addVehicleFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddVehicleActivity.class);
            startActivity(intent);
        });

        setupSwipeToDelete();
        requestNotificationPermission();
        startBackgroundEngine();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void startBackgroundEngine() {
        PeriodicWorkRequest alertRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "MotoPingDailyCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                alertRequest
        );
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
                        .setMessage("Are you sure you want to delete '" + swipedVehicle.getName() + "'? This cannot be undone.")
                        .setCancelable(false)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dbHelper.deleteVehicle(swipedVehicle.getId());
                            adapter.removeVehicle(position);
                            Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .show();
            }

            // --- THE UPGRADED POLISHED SWIPE ANIMATION ---
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                // Create a digital paintbrush for the red background
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#FF1744")); // A deeper, premium red
                paint.setAntiAlias(true); // Smooths the edges

                // Convert your exact 16dp card radius into raw screen pixels
                float radius = 16 * itemView.getContext().getResources().getDisplayMetrics().density;

                Drawable deleteIcon = ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_delete);
                if (deleteIcon != null) {
                    deleteIcon.setTint(Color.WHITE); // Force the trash can to be bright white

                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                    c.save(); // Save the canvas state before we start cutting it up

                    if (dX > 0) { // Swiping Right
                        // Clip the canvas so we only draw exactly in the space revealed by the swipe
                        c.clipRect(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());

                        // Draw a full rounded rectangle behind the item. Because we clipped it above,
                        // it will only show the exposed rounded edges!
                        c.drawRoundRect(new RectF(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom()), radius, radius, paint);

                        int iconLeft = itemView.getLeft() + iconMargin;
                        int iconRight = itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth();
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(c);

                    } else if (dX < 0) { // Swiping Left
                        c.clipRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());

                        c.drawRoundRect(new RectF(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom()), radius, radius, paint);

                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(c);
                    }

                    c.restore(); // Put the canvas back to normal for the rest of the app
                }
            }
        }).attachToRecyclerView(vehicleRecyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVehicles();
    }

    private void loadVehicles() {
        List<Vehicle> vehicleList = dbHelper.getAllVehicles();
        adapter = new VehicleAdapter(vehicleList);
        vehicleRecyclerView.setAdapter(adapter);

        TextView emptyStateText = findViewById(R.id.emptyStateText);
        if (vehicleList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            emptyStateText.setVisibility(View.GONE);
        }
    }
}