package com.example.motoping;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class GloveboxActivity extends AppCompatActivity {

    private Button btnUpload1, btnUpload2, btnUpload3, btnUpload4, btnUpload5;
    private LinearLayout cardDoc1, cardDoc2, cardDoc3, cardDoc4, cardDoc5;
    private TextView textDoc1, textDoc2, textDoc3, textDoc4, textDoc5;
    private ImageButton btnDelete1, btnDelete2, btnDelete3, btnDelete4, btnDelete5;

    private String vehicleId;
    private String targetDocumentField;

    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        handleFileSelected(selectedFileUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_glovebox);

        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        db = FirebaseFirestore.getInstance();
        vehicleId = getIntent().getStringExtra("VEHICLE_ID");

        // Bind UI Elements
        btnUpload1 = findViewById(R.id.btnUpload1);
        btnUpload2 = findViewById(R.id.btnUpload2);
        btnUpload3 = findViewById(R.id.btnUpload3);
        btnUpload4 = findViewById(R.id.btnUpload4);
        btnUpload5 = findViewById(R.id.btnUpload5);

        cardDoc1 = findViewById(R.id.cardDoc1);
        cardDoc2 = findViewById(R.id.cardDoc2);
        cardDoc3 = findViewById(R.id.cardDoc3);
        cardDoc4 = findViewById(R.id.cardDoc4);
        cardDoc5 = findViewById(R.id.cardDoc5);

        textDoc1 = findViewById(R.id.textDoc1);
        textDoc2 = findViewById(R.id.textDoc2);
        textDoc3 = findViewById(R.id.textDoc3);
        textDoc4 = findViewById(R.id.textDoc4);
        textDoc5 = findViewById(R.id.textDoc5);

        btnDelete1 = findViewById(R.id.btnDelete1);
        btnDelete2 = findViewById(R.id.btnDelete2);
        btnDelete3 = findViewById(R.id.btnDelete3);
        btnDelete4 = findViewById(R.id.btnDelete4);
        btnDelete5 = findViewById(R.id.btnDelete5);

        loadExistingDocuments();

        // Upload Listeners (Preserving old RC/Insurance field names for backward compatibility)
        btnUpload1.setOnClickListener(v -> { targetDocumentField = "rcImageUrl"; openFilePicker(); });
        btnUpload2.setOnClickListener(v -> { targetDocumentField = "insuranceImageUrl"; openFilePicker(); });
        btnUpload3.setOnClickListener(v -> { targetDocumentField = "doc3Url"; openFilePicker(); });
        btnUpload4.setOnClickListener(v -> { targetDocumentField = "doc4Url"; openFilePicker(); });
        btnUpload5.setOnClickListener(v -> { targetDocumentField = "doc5Url"; openFilePicker(); });

        // Delete Listeners
        btnDelete1.setOnClickListener(v -> deleteDocument("rcImageUrl"));
        btnDelete2.setOnClickListener(v -> deleteDocument("insuranceImageUrl"));
        btnDelete3.setOnClickListener(v -> deleteDocument("doc3Url"));
        btnDelete4.setOnClickListener(v -> deleteDocument("doc4Url"));
        btnDelete5.setOnClickListener(v -> deleteDocument("doc5Url"));
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelected(Uri fileUri) {
        Toast.makeText(this, "Uploading PDF to secure vault...", Toast.LENGTH_LONG).show();

        R2Uploader.uploadDocument(this, fileUri, "application/pdf", new R2Uploader.Callback() {
            @Override
            public void onSuccess(String fileUrl) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null && vehicleId != null) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    db.collection("users").document(userId)
                            .collection("vehicles").document(vehicleId)
                            .update(targetDocumentField, fileUrl)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(GloveboxActivity.this, "PDF Secured!", Toast.LENGTH_SHORT).show();
                                loadExistingDocuments();
                            });
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(GloveboxActivity.this, "Upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadExistingDocuments() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || vehicleId == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("vehicles").document(vehicleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        toggleSlot(documentSnapshot.getString("rcImageUrl"), btnUpload1, cardDoc1, textDoc1);
                        toggleSlot(documentSnapshot.getString("insuranceImageUrl"), btnUpload2, cardDoc2, textDoc2);
                        toggleSlot(documentSnapshot.getString("doc3Url"), btnUpload3, cardDoc3, textDoc3);
                        toggleSlot(documentSnapshot.getString("doc4Url"), btnUpload4, cardDoc4, textDoc4);
                        toggleSlot(documentSnapshot.getString("doc5Url"), btnUpload5, cardDoc5, textDoc5);
                    }
                });
    }

    private void toggleSlot(String url, Button uploadBtn, LinearLayout card, TextView titleText) {
        if (url != null && !url.isEmpty()) {
            uploadBtn.setVisibility(View.GONE);
            card.setVisibility(View.VISIBLE);
            titleText.setOnClickListener(v -> viewSecurePdf(url));
        } else {
            uploadBtn.setVisibility(View.VISIBLE);
            card.setVisibility(View.GONE);
        }
    }

    private void deleteDocument(String dbField) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || vehicleId == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("vehicles").document(vehicleId)
                .update(dbField, FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Document removed", Toast.LENGTH_SHORT).show();
                    loadExistingDocuments();
                });
    }

    private void viewSecurePdf(String storedUrl) {
        Toast.makeText(this, "Generating secure VIP pass...", Toast.LENGTH_SHORT).show();

        R2Uploader.getSecureReadUrl(storedUrl, new R2Uploader.Callback() {
            @Override
            public void onSuccess(String secureTempUrl) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(secureTempUrl), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(secureTempUrl)));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(GloveboxActivity.this, "Security Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}