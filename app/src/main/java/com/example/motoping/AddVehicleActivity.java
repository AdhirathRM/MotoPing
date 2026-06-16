package com.example.motoping;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

public class AddVehicleActivity extends AppCompatActivity {

    private EditText editVehicleName, editInsurance, editService, editPuc, editRc;
    private Spinner spinnerVehicleType;
    private Button btnSaveVehicle;

    // Cloud Variables
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private CardView c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    private CardView[] allColors;
    private String selectedColorHex = "#EF5350";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_vehicle);

        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editVehicleName = findViewById(R.id.editVehicleName);
        editInsurance = findViewById(R.id.editInsurance);
        editService = findViewById(R.id.editService);
        editPuc = findViewById(R.id.editPuc);
        editRc = findViewById(R.id.editRc);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        btnSaveVehicle = findViewById(R.id.btnSaveVehicle);

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

        c1.setScaleX(1.2f); c1.setScaleY(1.2f);

        btnSaveVehicle.setOnClickListener(v -> {
            String name = editVehicleName.getText().toString().trim();
            String insurance = editInsurance.getText().toString().trim();
            String service = editService.getText().toString().trim();
            String puc = editPuc.getText().toString().trim();
            String rc = editRc.getText().toString().trim();
            String selectedType = spinnerVehicleType.getSelectedItem().toString();

            if (name.isEmpty() || mAuth.getCurrentUser() == null) {
                Toast.makeText(AddVehicleActivity.this, "Please enter a name / Verify Login", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Get the current user's ID
            String userId = mAuth.getCurrentUser().getUid();

            // 2. Generate a random secure ID for the new vehicle document
            String newVehicleId = db.collection("users").document(userId).collection("vehicles").document().getId();

            // 3. Create the object and push to Firestore
            Vehicle newVehicle = new Vehicle(newVehicleId, name, insurance, service, puc, rc, selectedColorHex, selectedType);

            db.collection("users").document(userId).collection("vehicles").document(newVehicleId)
                    .set(newVehicle)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddVehicleActivity.this, "Vehicle Synced to Cloud!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddVehicleActivity.this, "Cloud Sync Failed", Toast.LENGTH_SHORT).show());
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
                AddVehicleActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    editText.setText(formattedDate);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
}