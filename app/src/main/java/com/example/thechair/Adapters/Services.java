// Shaq’s Notes:
// This is a simple POJO representing a service offered by a professional.
// It mirrors the structure stored inside the "services" array of an appUsers
// Firestore document.
//
// A service consists of:
// - name (string)
// - price (double)
// - duration in minutes (int)
//
// Firestore requires an empty constructor and public getters/setters to map
// this class automatically from document data.

package com.example.thechair.Adapters;

public class Services {

    private String name;     // service name (e.g., "Haircut")
    private double price;    // service price
    private int duration;    // time required in minutes

    // Required empty constructor for Firestore
    public Services() {}

    // Full constructor for manual creation
    public Services(String name, double price, int duration) {
        this.name = name;
        this.price = price;
        this.duration = duration;
    }

    // -------------------- Getters & Setters --------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
