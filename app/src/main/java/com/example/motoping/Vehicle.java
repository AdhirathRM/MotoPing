package com.example.motoping;

import com.google.firebase.firestore.Exclude;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Vehicle {
    private String id;
    private String name;
    private String insuranceExpiry; // Restored original name
    private String serviceDueDate;  // Restored original name
    private String pucDueDate;      // Restored original name
    private String rcExpiry;        // Restored original name
    private String colorHex;
    private String type;
    private long orderIndex;

    public Vehicle() {
        // Required empty constructor for Firestore
    }

    public Vehicle(String id, String name, String insuranceExpiry, String serviceDueDate, String pucDueDate, String rcExpiry, String colorHex, String type) {
        this.id = id;
        this.name = name;
        this.insuranceExpiry = insuranceExpiry;
        this.serviceDueDate = serviceDueDate;
        this.pucDueDate = pucDueDate;
        this.rcExpiry = rcExpiry;
        this.colorHex = colorHex;
        this.type = type;
        this.orderIndex = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInsuranceExpiry() { return insuranceExpiry; }
    public void setInsuranceExpiry(String insuranceExpiry) { this.insuranceExpiry = insuranceExpiry; }
    public String getServiceDueDate() { return serviceDueDate; }
    public void setServiceDueDate(String serviceDueDate) { this.serviceDueDate = serviceDueDate; }
    public String getPucDueDate() { return pucDueDate; }
    public void setPucDueDate(String pucDueDate) { this.pucDueDate = pucDueDate; }
    public String getRcExpiry() { return rcExpiry; }
    public void setRcExpiry(String rcExpiry) { this.rcExpiry = rcExpiry; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getOrderIndex() { return orderIndex; }
    public void setOrderIndex(long orderIndex) { this.orderIndex = orderIndex; }

    @Exclude
    public int calculateHealthScore() {
        int score = 0;
        score += evaluateDate(insuranceExpiry);
        score += evaluateDate(serviceDueDate);
        score += evaluateDate(pucDueDate);
        score += evaluateDate(rcExpiry);
        return score;
    }

    @Exclude
    private int evaluateDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate expiry = LocalDate.parse(dateStr, formatter);
            LocalDate today = LocalDate.now();
            long daysLeft = ChronoUnit.DAYS.between(today, expiry);

            if (daysLeft >= 30) return 25;
            if (daysLeft >= 15) return 15;
            if (daysLeft > 0) return 5;
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}