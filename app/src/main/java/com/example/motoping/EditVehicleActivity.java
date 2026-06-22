package com.example.motoping;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class EditVehicleActivity extends AppCompatActivity {

    private EditText editVehicleName, editInsurance, editService, editPuc, editRc;
    private AutoCompleteTextView spinnerVehicleType;
    private Button btnUpdateVehicle;

    // Cloud Variables
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String vehicleId;

    private CardView c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    private CardView[] allColors;
    private String selectedColorHex = "#EF5350";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_vehicle);

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

        editVehicleName = findViewById(R.id.editVehicleName);
        editInsurance = findViewById(R.id.editInsurance);
        editService = findViewById(R.id.editService);
        editPuc = findViewById(R.id.editPuc);
        editRc = findViewById(R.id.editRc);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        btnUpdateVehicle = findViewById(R.id.btnUpdateVehicle);

        String[] types = {"Car", "Bike", "Scooter"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerVehicleType.setAdapter(typeAdapter);

        c1 = findViewById(R.id.c1);
        c2 = findViewById(R.id.c2);
        c3 = findViewById(R.id.c3);
        c4 = findViewById(R.id.c4);
        c5 = findViewById(R.id.c5);
        c6 = findViewById(R.id.c6);
        c7 = findViewById(R.id.c7);
        c8 = findViewById(R.id.c8);
        c9 = findViewById(R.id.c9);
        c10 = findViewById(R.id.c10);

        allColors = new CardView[]{c1, c2, c3, c4, c5, c6, c7, c8, c9, c10};

        editInsurance.setFocusable(false);
        editService.setFocusable(false);
        editPuc.setFocusable(false);
        editRc.setFocusable(false);

        editInsurance.setOnClickListener(v -> showDatePicker(editInsurance));
        editService.setOnClickListener(v -> showDatePicker(editService));
        editPuc.setOnClickListener(v -> showDatePicker(editPuc));
        editRc.setOnClickListener(v -> showDatePicker(editRc));

        setupColorListener(c1, "#EF5350");
        setupColorListener(c2, "#FF1744");
        setupColorListener(c3, "#F48FB1");
        setupColorListener(c4, "#7E57C2");
        setupColorListener(c5, "#42A5F5");
        setupColorListener(c6, "#00E5FF");
        setupColorListener(c7, "#66BB6A");
        setupColorListener(c8, "#FFEE58");
        setupColorListener(c9, "#FF9800");
        setupColorListener(c10, "#FFFFFF");

        Intent intent = getIntent();
        if (intent != null) {
            vehicleId = intent.getStringExtra("ID");
            editVehicleName.setText(intent.getStringExtra("NAME"));
            editInsurance.setText(intent.getStringExtra("INSURANCE"));
            editService.setText(intent.getStringExtra("SERVICE"));
            editPuc.setText(intent.getStringExtra("PUC"));
            editRc.setText(intent.getStringExtra("RC"));

            String savedType = intent.getStringExtra("TYPE");
            if (savedType != null) {
                spinnerVehicleType.setText(savedType, false);
            }

            String savedColor = intent.getStringExtra("COLOR");
            if (savedColor != null) {
                selectedColorHex = savedColor;
                if (savedColor.equals("#EF5350")) { c1.setScaleX(1.2f); c1.setScaleY(1.2f); }
                else if (savedColor.equals("#FF1744")) { c2.setScaleX(1.2f); c2.setScaleY(1.2f); }
                else if (savedColor.equals("#F48FB1")) { c3.setScaleX(1.2f); c3.setScaleY(1.2f); }
                else if (savedColor.equals("#7E57C2")) { c4.setScaleX(1.2f); c4.setScaleY(1.2f); }
                else if (savedColor.equals("#42A5F5")) { c5.setScaleX(1.2f); c5.setScaleY(1.2f); }
                else if (savedColor.equals("#00E5FF")) { c6.setScaleX(1.2f); c6.setScaleY(1.2f); }
                else if (savedColor.equals("#66BB6A")) { c7.setScaleX(1.2f); c7.setScaleY(1.2f); }
                else if (savedColor.equals("#FFEE58")) { c8.setScaleX(1.2f); c8.setScaleY(1.2f); }
                else if (savedColor.equals("#FF9800")) { c9.setScaleX(1.2f); c9.setScaleY(1.2f); }
                else if (savedColor.equals("#FFFFFF")) { c10.setScaleX(1.2f); c10.setScaleY(1.2f); }
            }
        }

        btnUpdateVehicle.setOnClickListener(v -> {
            String name = editVehicleName.getText().toString().trim();
            String insurance = editInsurance.getText().toString().trim();
            String service = editService.getText().toString().trim();
            String puc = editPuc.getText().toString().trim();
            String rc = editRc.getText().toString().trim();
            String selectedType = spinnerVehicleType.getText().toString();

            if (name.isEmpty() || mAuth.getCurrentUser() == null || vehicleId == null) {
                Toast.makeText(EditVehicleActivity.this, "Error updating vehicle", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();

            db.collection("users").document(userId).collection("vehicles").document(vehicleId)
                    .update(
                            "name", name,
                            "insuranceExpiry", insurance,
                            "serviceDueDate", service,
                            "pucDueDate", puc,
                            "rcExpiry", rc,
                            "colorHex", selectedColorHex,
                            "type", selectedType
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditVehicleActivity.this, "Cloud Update Successful!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(EditVehicleActivity.this, "Update Failed", Toast.LENGTH_SHORT).show());
        });
    }

    private void setupColorListener(CardView card, String hex) {
        card.setOnClickListener(v -> {
            selectedColorHex = hex;
            for (CardView c : allColors) {
                c.setScaleX(1f); c.setScaleY(1f);
            }
            card.setScaleX(1.2f); card.setScaleY(1.2f);
        });
    }

    private void showDatePicker(EditText editText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                EditVehicleActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    editText.setText(formattedDate);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        // NEW: Injects a "CLEAR" button directly into the calendar popup
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "CLEAR", (dialog, which) -> {
            editText.setText("");
        });

        datePickerDialog.show();
    }
}