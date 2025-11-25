// Shaq’s Notes:
// This is a simple model representing a local, non-Firestore hair appointment.
// It isn’t tied to the database: it just stores info for an in-app list/UI.
// Each appointment contains:
// - the client's name
// - the service type (braids, haircut, etc.)
// - the scheduled time
// - optional notes (e.g. “bring extensions”)
// - a completion flag for marking the appointment done.

package com.example.thechair.Adapters;

public class HairAppointment {

    private final String clientName;     // customer's name
    private final String serviceType;    // type of service
    private final String time;           // appointment time
    private final String notes;          // optional notes
    private boolean isCompleted;         // whether the appointment is finished

    // Constructor initializes a new appointment (not completed by default)
    public HairAppointment(String clientName, String serviceType, String time, String notes) {
        this.clientName = clientName;
        this.serviceType = serviceType;
        this.time = time;
        this.notes = notes;
        this.isCompleted = false;
    }

    // ------------ Getters ------------
    public String getClientName() { return clientName; }
    public String getServiceType() { return serviceType; }
    public String getTime() { return time; }
    public String getNotes() { return notes; }
    public boolean isCompleted() { return isCompleted; }

    // ------------ Setter ------------
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
}
