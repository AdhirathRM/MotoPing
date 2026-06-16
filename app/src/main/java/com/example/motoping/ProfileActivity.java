package com.example.motoping;

import android.content.Intent;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        ImageView imgAvatar = findViewById(R.id.imgAvatar);
        TextView emailText = findViewById(R.id.textUserEmail);
        TextView countText = findViewById(R.id.textVehicleCount);
        TextView memberSinceText = findViewById(R.id.textMemberSince);
        Button btnLogout = findViewById(R.id.btnLogout);

        if (user != null) {
            emailText.setText(user.getEmail());

            // NEW: Fetch and load the Google Profile Picture
            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                // We use replace() to request a higher resolution image from Google (s96 -> s400)
                String highResUrl = photoUrl.toString().replace("s96-c", "s400-c");

                Glide.with(this)
                        .load(highResUrl)
                        .circleCrop() // This ensures the image stays perfectly circular
                        .into(imgAvatar);
            }

            if (user.getMetadata() != null) {
                long creationTime = user.getMetadata().getCreationTimestamp();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                String dateString = sdf.format(new Date(creationTime));
                memberSinceText.setText("MEMBER SINCE " + dateString.toUpperCase());
            }

            db.collection("users").document(user.getUid()).collection("vehicles")
                    .count().get(AggregateSource.SERVER)
                    .addOnSuccessListener(snapshot -> countText.setText(String.valueOf(snapshot.getCount())));
        }

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}