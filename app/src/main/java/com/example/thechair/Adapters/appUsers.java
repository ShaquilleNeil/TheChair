// Shaq’s Notes:
// This class models a user inside The Chair app. It mirrors exactly how user
// documents are structured in Firestore: role, services, portfolio images,
// address object, geolocation, tags, and ratings. All nested objects are set
// up to be Firestore-friendly (must have empty constructors and getters/setters).
// Professionals and customers both use this model, with extra fields (profession,
// services, portfolio) only populated for pros.

package com.example.thechair.Adapters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.firestore.GeoPoint;

public class appUsers implements Serializable {

    // Basic identity & role
    private String id;
    private String name;
    private String email;
    private String role;                // "professional" or "customer"

    // Rating system (for pros)
    private double rating;              // average rating
    private int ratingCount;            // number of reviews

    // Profile and portfolio
    private String profilepic;          // profile picture URL
    private List<String> portfolioImages; // images uploaded by professionals

    // Professional-specific fields
    private String profession;          // ex: barber, braider, makeup artist
    private List<Service> services;     // list of services they offer
    private List<String> tags;          // quick filters for search

    // Location information
    private Address address;            // stored as a nested object
    private GeoPoint geo;               // Firestore geopoint for map queries

    // Contact info
    private String phoneNumber;

    // Empty constructor required by Firestore’s automatic object mapping
    public appUsers() {}

    // Constructor used at signup (lists start empty)
    public appUsers(String id, String name, String email, String role,
                    Address address, GeoPoint geo, String phoneNumber) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;

        // Default rating on new accounts
        this.rating = 0.0;
        this.ratingCount = 0;

        this.profilepic = "";
        this.profession = "";

        // New pros start with empty lists until they fill their profile
        this.portfolioImages = new ArrayList<>();
        this.services = new ArrayList<>();
        this.tags = new ArrayList<>();

        this.address = address;
        this.geo = geo;
        this.phoneNumber = phoneNumber;
    }


    // -------------------- Getters & Setters --------------------

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public String getProfilepic() { return profilepic; }
    public void setProfilepic(String profilepic) { this.profilepic = profilepic; }

    public List<String> getPortfolioImages() { return portfolioImages; }
    public void setPortfolioImages(List<String> portfolioImages) {
        this.portfolioImages = portfolioImages;
    }

    public List<Service> getServices() { return services; }
    public void setServices(List<Service> services) {
        this.services = services;
    }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public GeoPoint getGeo() { return geo; }
    public void setGeo(GeoPoint geo) { this.geo = geo; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }


    // -------------------- Nested Classes --------------------
    // These must all be Serializable + have empty constructors for Firestore.

    // Address object stored as a map inside the user doc
    public static class Address implements Serializable {
        private String street;
        private String room;
        private String city;
        private String province;
        private String country;
        private String postalCode;

        public Address() {}

        public Address(String street, String room, String city,
                       String province, String country, String postalCode) {
            this.street = street;
            this.room = room;
            this.city = city;
            this.province = province;
            this.country = country;
            this.postalCode = postalCode;
        }

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }

        public String getRoom() { return room; }
        public void setRoom(String room) { this.room = room; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
    }

    // A simple lat/lng wrapper class (not used much since GeoPoint exists)
    public static class Geo implements Serializable {
        private double lat;
        private double lng;

        public Geo() {}

        public Geo(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
    }

    // Service object for professionals (name, price, duration)
    public static class Service implements Serializable {
        private String name;
        private double price;
        private int duration; // minutes

        public Service() {}

        public Service(String name, double price, int duration) {
            this.name = name;
            this.price = price;
            this.duration = duration;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }
}
