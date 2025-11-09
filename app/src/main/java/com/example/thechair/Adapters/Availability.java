package com.example.thechair.Adapters;

// Represents one day of weekly availability
public class Availability {
    private String day;       // e.g. "monday"
    private boolean available;
    private String start;     // e.g. "09:00"
    private String end;       // e.g. "17:00"

    // Required empty constructor for Firestore
    public Availability() {}

    // Full constructor
    public Availability(String day, boolean available, String start, String end) {
        this.day = day;
        this.available = available;
        this.start = start;
        this.end = end;
    }

    // Getters
    public String getDay() { return day; }
    public boolean isAvailable() { return available; }
    public String getStart() { return start; }
    public String getEnd() { return end; }

    // Setters (needed for adapter updates and Firestore deserialization)
    public void setDay(String day) { this.day = day; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setStart(String start) { this.start = start; }
    public void setEnd(String end) { this.end = end; }
}
