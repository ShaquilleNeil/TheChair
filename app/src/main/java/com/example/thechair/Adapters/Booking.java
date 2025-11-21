package com.example.thechair.Adapters;

public class Booking {

    public String bookingId;
    public String customerId;
    public String customerName;

    public String professionalId;
    public String professionalName;
    public String proPic;

    public String serviceName;
    public String serviceTime;
    public String endTime;

    public String selectedDate;
    public String status;

    public int serviceDuration;   // <-- NEW
    public int servicePrice;      // <-- NEW

    public long timestamp;        // <-- Optional but recommended

    public Booking() {}

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

    public int getServiceDuration() { return serviceDuration; }  // new getter
    public int getServicePrice() { return servicePrice; }        // new getter

    public long getTimestamp() { return timestamp; }
}
