package com.example.motoping;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class AddServiceLogActivity extends AppCompatActivity {

    private String vehicleId;
    private String existingLogId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_service_log);

        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        vehicleId = getIntent().getStringExtra("VEHICLE_ID");
        existingLogId = getIntent().getStringExtra("LOG_ID"); // Null if adding new
        db = FirebaseFirestore.getInstance();

        EditText inputTitle = findViewById(R.id.inputLogTitle);
        EditText inputDate = findViewById(R.id.inputLogDate);
        EditText inputCost = findViewById(R.id.inputLogCost);
        EditText inputNotes = findViewById(R.id.inputLogNotes);
        Button btnSaveLog = findViewById(R.id.btnSaveLog);

        // --- NATIVE DATE PICKER ---
        inputDate.setFocusable(false);
        inputDate.setClickable(true);
        inputDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(AddServiceLogActivity.this, (view, year, month, dayOfMonth) -> {
                String formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                inputDate.setText(formattedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // --- PRE-FILL DATA IF EDITING ---
        if (existingLogId != null) {
            inputTitle.setText(getIntent().getStringExtra("TITLE"));
            inputDate.setText(getIntent().getStringExtra("DATE"));
            inputCost.setText(getIntent().getStringExtra("COST"));
            inputNotes.setText(getIntent().getStringExtra("NOTES"));
            btnSaveLog.setText("UPDATE RECORD");
        }

        btnSaveLog.setOnClickListener(v -> {
            String title = inputTitle.getText().toString().trim();
            String date = inputDate.getText().toString().trim();
            String cost = inputCost.getText().toString().trim();
            String notes = inputNotes.getText().toString().trim();

            if (title.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Title and Date are required", Toast.LENGTH_SHORT).show();
                return;
            }

            saveServiceLog(title, date, cost, notes);
        });
    }

    private void saveServiceLog(String title, String date, String cost, String notes) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || vehicleId == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // If existingLogId is null, generate a new one. Otherwise, reuse the old one to overwrite!
        String logId = existingLogId != null ? existingLogId : db.collection("users").document(userId)
                .collection("vehicles").document(vehicleId)
                .collection("service_logs").document().getId();

        ServiceLog log = new ServiceLog(logId, title, date, cost, notes);

        db.collection("users").document(userId)
                .collection("vehicles").document(vehicleId)
                .collection("service_logs").document(logId)
                .set(log)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, existingLogId != null ? "Record Updated!" : "Record Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving record", Toast.LENGTH_SHORT).show());
    }
}