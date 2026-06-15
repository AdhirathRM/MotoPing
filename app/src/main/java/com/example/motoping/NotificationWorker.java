package com.example.motoping;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NotificationWorker extends Worker {

    private static final String CHANNEL_ID = "motoping_alerts";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 1. Open database and grab all vehicles
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        List<Vehicle> vehicles = dbHelper.getAllVehicles();

        // 2. Setup the date reader to understand our DD/MM/YYYY format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate today = LocalDate.now();

        // 3. Loop through every single vehicle and check its dates
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
            // Calculate exact days remaining
            long daysLeft = ChronoUnit.DAYS.between(today, expiryDate);

            // If the date is exactly today, or within the next 7 days, trigger the ping
            if (daysLeft >= 0 && daysLeft <= 7) {
                sendPing(vehicleName, type, daysLeft);
            }
        } catch (Exception e) {
            // If the user left a date blank or it formatted wrong, just skip it
        }
    }

    private void sendPing(String vehicleName, String type, long daysLeft) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Modern Android requires a "Notification Channel" to be created first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Vehicle Compliance Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        String message = (daysLeft == 0) ? "Expires TODAY!" : "Expires in " + daysLeft + " days.";

        // Build the actual visual notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // We will replace this with your logo later
                .setContentTitle(vehicleName + ": " + type + " Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Generate a random ID so multiple notifications don't overwrite each other
        int uniqueId = (int) System.currentTimeMillis() % 10000;
        notificationManager.notify(uniqueId, builder.build());
    }
}