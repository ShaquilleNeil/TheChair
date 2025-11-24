package com.example.thechair.Adapters;

public class HairAppointment {
    private final String clientName;
    private final String serviceType;
    private final String time;
    private final String notes; // optional, e.g. "bring extensions"
    private boolean isCompleted;

    public HairAppointment(String clientName, String serviceType, String time, String notes) {
        this.clientName = clientName;
        this.serviceType = serviceType;
        this.time = time;
        this.notes = notes;
        this.isCompleted = false;
    }

    // Getters
    public String getClientName() { return clientName; }
    public String getServiceType() { return serviceType; }
    public String getTime() { return time; }
    public String getNotes() { return notes; }
    public boolean isCompleted() { return isCompleted; }

    // Setters
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
}
