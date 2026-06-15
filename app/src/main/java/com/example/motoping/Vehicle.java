package com.example.motoping;

public class Vehicle {
    private int id;
    private String name;
    private String insuranceExpiry;
    private String serviceDueDate;
    private String pucDueDate;
    private String rcExpiry;
    private String colorHex; // NEW

    public Vehicle(int id, String name, String insuranceExpiry, String serviceDueDate, String pucDueDate, String rcExpiry, String colorHex) {
        this.id = id;
        this.name = name;
        this.insuranceExpiry = insuranceExpiry;
        this.serviceDueDate = serviceDueDate;
        this.pucDueDate = pucDueDate;
        this.rcExpiry = rcExpiry;
        this.colorHex = colorHex;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
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
}