package com.example.motoping;

import com.google.firebase.firestore.Exclude;

public class ServiceLog {
    private String id;
    private String title;
    private String date;
    private String cost;
    private String notes;

    // Required empty constructor for Firestore
    public ServiceLog() {}

    public ServiceLog(String id, String title, String date, String cost, String notes) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.cost = cost;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCost() { return cost; }
    public void setCost(String cost) { this.cost = cost; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}