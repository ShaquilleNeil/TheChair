// Shaq’s Notes:
// This class represents a single booking in The Chair.
// It matches the structure stored in Firestore under “bookings” for both
// customers and professionals. Each booking contains:
// - IDs and names of both sides,
// - service info (name, duration, price),
// - scheduling info (date, start time, end time),
// - status (pending / accepted / completed / rejected),
// - optional timestamp for sorting chronologically.
// Firestore needs the empty constructor and public fields for mapping.

package com.example.thechair.Adapters;

import com.google.firebase.firestore.GeoPoint;

public class Booking {

    // Firestore document ID for this booking
    public String bookingId;

    // Customer info
    public String customerId;
    public String customerName;

    // Professional info
    public String professionalId;
    public String professionalName;
    public String proPic;          // professional’s profile image URL

    // Service details
    public String serviceName;
    public String serviceTime;     // start time (e.g. "14:00")
    public String endTime;         // end time based on duration

    // Booking metadata
    public String selectedDate;    // e.g. "2025-03-12"
    public String status;          // pending / accepted / completed / rejected

    // NEW fields
    public int serviceDuration;    // minutes
    public int servicePrice;       // cost in dollars or local currency integer

    // For chronological sorting & preventing duplicate loads
    public long timestamp;


    public Address address;       // Mapped from the address map
    public GeoPoint geo;          // Mapped from the root field "geo"

    public static class Address {
        public String street;
        public String room;
        public String city;
        public String province;
        public String postalCode;

        public Address() {}
    }

    // Empty constructor needed by Firestore
    public Booking() {}

    // -------------------- Getters --------------------

    public String getBookingId() { return bookingId; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }

    public String getProfessionalId() { return professionalId; }
    public String getProfessionalName() { return professionalName; }
    public String getProPic() { return proPic; }

    public String getServiceName() { return serviceName; }
    public String getServiceTime() { return serviceTime; }
    public String getEndTime() { return endTime; }

    public String getSelectedDate() { return selectedDate; }
    public String getStatus() { return status; }

    public int getServiceDuration() { return serviceDuration; }
    public int getServicePrice() { return servicePrice; }

    public long getTimestamp() { return timestamp; }
    public Address getAddress() { return address; }
    public GeoPoint getGeo() { return geo; }
}
