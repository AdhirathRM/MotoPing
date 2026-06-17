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

    private Button btnUploadRc, btnUploadInsurance;
    private LinearLayout cardRcDoc, cardInsuranceDoc;
    private TextView textRcName, textInsuranceName;
    private ImageButton btnDeleteRc, btnDeleteInsurance;

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

        btnUploadRc = findViewById(R.id.btnUploadRc);
        btnUploadInsurance = findViewById(R.id.btnUploadInsurance);

        cardRcDoc = findViewById(R.id.cardRcDoc);
        cardInsuranceDoc = findViewById(R.id.cardInsuranceDoc);

        textRcName = findViewById(R.id.textRcName);
        textInsuranceName = findViewById(R.id.textInsuranceName);

        btnDeleteRc = findViewById(R.id.btnDeleteRc);
        btnDeleteInsurance = findViewById(R.id.btnDeleteInsurance);

        loadExistingDocuments();

        btnUploadRc.setOnClickListener(v -> {
            targetDocumentField = "rcImageUrl";
            openFilePicker();
        });

        btnUploadInsurance.setOnClickListener(v -> {
            targetDocumentField = "insuranceImageUrl";
            openFilePicker();
        });

        btnDeleteRc.setOnClickListener(v -> deleteDocument("rcImageUrl"));
        btnDeleteInsurance.setOnClickListener(v -> deleteDocument("insuranceImageUrl"));
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
                        String rcUrl = documentSnapshot.getString("rcImageUrl");
                        String insuranceUrl = documentSnapshot.getString("insuranceImageUrl");

                        if (rcUrl != null && !rcUrl.isEmpty()) {
                            btnUploadRc.setVisibility(View.GONE);
                            cardRcDoc.setVisibility(View.VISIBLE);
                            textRcName.setOnClickListener(v -> viewSecurePdf(rcUrl));
                        } else {
                            btnUploadRc.setVisibility(View.VISIBLE);
                            cardRcDoc.setVisibility(View.GONE);
                        }

                        if (insuranceUrl != null && !insuranceUrl.isEmpty()) {
                            btnUploadInsurance.setVisibility(View.GONE);
                            cardInsuranceDoc.setVisibility(View.VISIBLE);
                            textInsuranceName.setOnClickListener(v -> viewSecurePdf(insuranceUrl));
                        } else {
                            btnUploadInsurance.setVisibility(View.VISIBLE);
                            cardInsuranceDoc.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void deleteDocument(String dbField) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || vehicleId == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("vehicles").document(vehicleId)
                .update(dbField, FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Document removed from vault", Toast.LENGTH_SHORT).show();
                    loadExistingDocuments();
                });
    }

    // NEW: Intercepts the click, generates a 60-second secure link, then opens it.
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