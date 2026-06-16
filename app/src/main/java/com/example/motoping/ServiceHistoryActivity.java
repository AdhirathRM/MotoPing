package com.example.motoping;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ServiceHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerServiceLogs;
    private ServiceLogAdapter adapter;
    private List<ServiceLog> logList = new ArrayList<>();
    private TextView textEmptyLogs;
    private String vehicleId;
    private String vehicleName;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_service_history);

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

        vehicleId = getIntent().getStringExtra("VEHICLE_ID");
        vehicleName = getIntent().getStringExtra("VEHICLE_NAME");

        TextView headerVehicleName = findViewById(R.id.headerVehicleName);
        headerVehicleName.setText(vehicleName);

        textEmptyLogs = findViewById(R.id.textEmptyLogs);
        recyclerServiceLogs = findViewById(R.id.recyclerServiceLogs);
        recyclerServiceLogs.setLayoutManager(new LinearLayoutManager(this));

        // Pass the vehicleId to the adapter so it can trigger edits!
        adapter = new ServiceLogAdapter(logList, vehicleId);
        recyclerServiceLogs.setAdapter(adapter);

        FloatingActionButton fabAddLog = findViewById(R.id.fabAddLog);
        fabAddLog.setOnClickListener(v -> {
            Intent intent = new Intent(ServiceHistoryActivity.this, AddServiceLogActivity.class);
            intent.putExtra("VEHICLE_ID", vehicleId);
            startActivity(intent);
        });

        setupSwipeToDelete();
        loadServiceHistory();
    }

    private void loadServiceHistory() {
        if (mAuth.getCurrentUser() == null || vehicleId == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .collection("vehicles").document(vehicleId)
                .collection("service_logs")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("Firestore", "Listen failed.", error);
                        return;
                    }
                    logList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ServiceLog log = doc.toObject(ServiceLog.class);
                            if (log != null) {
                                logList.add(log);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (logList.isEmpty()) {
                        textEmptyLogs.setVisibility(View.VISIBLE);
                    } else {
                        textEmptyLogs.setVisibility(View.GONE);
                    }
                });
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
                ServiceLog swipedLog = logList.get(position);

                new AlertDialog.Builder(ServiceHistoryActivity.this)
                        .setTitle("Delete Record")
                        .setMessage("Delete '" + swipedLog.getTitle() + "'?")
                        .setCancelable(false)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (mAuth.getCurrentUser() != null) {
                                String userId = mAuth.getCurrentUser().getUid();
                                db.collection("users").document(userId)
                                        .collection("vehicles").document(vehicleId)
                                        .collection("service_logs").document(swipedLog.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(ServiceHistoryActivity.this, "Record Deleted", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // Restore item if canceled
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
                float radius = 12 * itemView.getContext().getResources().getDisplayMetrics().density;
                Drawable deleteIcon = ContextCompat.getDrawable(ServiceHistoryActivity.this, android.R.drawable.ic_menu_delete);

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
        }).attachToRecyclerView(recyclerServiceLogs);
    }
}