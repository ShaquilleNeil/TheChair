// Shaq’s Notes:
// This model represents a single day's availability for a professional.
// It's stored as part of their Firestore document. Each day holds:
// - the day name,
// - whether the pro is working that day,
// - the start time,
// - the end time.
// Firestore requires an empty constructor + setters for automatic mapping.

package com.example.thechair.Adapters;

public class Availability {

    // Name of the weekday (e.g. "monday")
    private String day;

    // Whether the professional is working that day
    private boolean available;

    // Time range for that day (24h format strings)
    private String start;   // opening time
    private String end;     // closing time

    // Empty constructor required by Firestore
    public Availability() {}

    // Full constructor used when creating availability manually in code
    public Availability(String day, boolean available, String start, String end) {
        this.day = day;
        this.available = available;
        this.start = start;
        this.end = end;
    }

    // Getters for accessing in UI/adapters
    public String getDay() { return day; }
    public boolean isAvailable() { return available; }
    public String getStart() { return start; }
    public String getEnd() { return end; }

    // Setters (Firestore needs them for object mapping)
    public void setDay(String day) { this.day = day; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setStart(String start) { this.start = start; }
    public void setEnd(String end) { this.end = end; }
}
