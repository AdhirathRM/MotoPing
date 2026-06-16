package com.example.motoping;

import com.google.firebase.firestore.Exclude;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Vehicle {
    private String id;
    private String name;
    private String insuranceExpiry;
    private String serviceDueDate;
    private String pucDueDate;
    private String rcExpiry;
    private String colorHex;
    private String type;
    private long orderIndex;

    // Required empty public constructor for Firebase
    public Vehicle() {
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

    // --- NEW: Dynamic Compliance Engine ---

    @Exclude
    public int calculateHealthScore() {
        int score = 0;
        // 4 categories, 25 points each = 100 max score
        score += evaluateDate(insuranceExpiry);
        score += evaluateDate(serviceDueDate);
        score += evaluateDate(pucDueDate);
        score += evaluateDate(rcExpiry);
        return score;
    }

    @Exclude
    private int evaluateDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0; // Missing data = 0 health

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate expiry = LocalDate.parse(dateStr, formatter);
            LocalDate today = LocalDate.now();
            long daysLeft = ChronoUnit.DAYS.between(today, expiry);

            if (daysLeft >= 30) return 25; // Perfect Health (More than a month left)
            if (daysLeft >= 15) return 15; // Warning (Less than a month left)
            if (daysLeft > 0) return 5;    // Critical (Expires in days)
            return 0;                      // Expired
        } catch (Exception e) {
            return 0; // If date parsing fails, award no points
        }
    }
    public long getOrderIndex() { return orderIndex; }
    public void setOrderIndex(long orderIndex) { this.orderIndex = orderIndex; }
}