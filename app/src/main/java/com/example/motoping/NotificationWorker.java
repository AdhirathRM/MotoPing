package com.example.motoping;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NotificationWorker extends Worker {

    private static final String CHANNEL_ID = "motoping_alerts";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 1. Check if a user is currently logged in. If not, stop the engine.
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return Result.success();
        }

        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Vehicle> vehicles = new ArrayList<>();

        // 2. Fetch the vehicles securely from Firestore
        try {
            // Tasks.await() forces the background thread to wait until cloud data arrives
            QuerySnapshot querySnapshot = Tasks.await(db.collection("users").document(userId).collection("vehicles").get());

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Vehicle vehicle = doc.toObject(Vehicle.class);
                if (vehicle != null) {
                    vehicles.add(vehicle);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("NotificationWorker", "Cloud fetch failed", e);
            return Result.retry(); // Tell Android to try again later if the network drops
        }

        // 3. Setup the date reader to understand our DD/MM/YYYY format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate today = LocalDate.now();

        // 4. Loop through every single cloud vehicle and check its dates
        for (Vehicle v : vehicles) {
            checkAndNotify(v.getName(), "Insurance", v.getInsuranceExpiry(), formatter, today);
            checkAndNotify(v.getName(), "Service", v.getServiceDueDate(), formatter, today);
            checkAndNotify(v.getName(), "PUC", v.getPucDueDate(), formatter, today);
            checkAndNotify(v.getName(), "RC", v.getRcExpiry(), formatter, today);
        }

        return Result.success();
    }

    private void checkAndNotify(String vehicleName, String type, String dateStr, DateTimeFormatter formatter, LocalDate today) {
        if (dateStr == null || dateStr.isEmpty()) return;

        try {
            LocalDate expiryDate = LocalDate.parse(dateStr, formatter);
            long daysLeft = ChronoUnit.DAYS.between(today, expiryDate);

            // If the date is exactly today, or within the next 7 days, trigger the ping
            if (daysLeft >= 0 && daysLeft <= 7) {
                sendPing(vehicleName, type, daysLeft);
            }
        } catch (Exception e) {
            // Ignore blank or badly formatted dates
        }
    }

    private void sendPing(String vehicleName, String type, long daysLeft) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Vehicle Compliance Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        String message = (daysLeft == 0) ? "Expires TODAY!" : "Expires in " + daysLeft + " days.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(vehicleName + ": " + type + " Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        int uniqueId = (int) System.currentTimeMillis() % 10000;
        notificationManager.notify(uniqueId, builder.build());
    }
}