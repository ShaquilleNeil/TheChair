package com.example.thechair.Adapters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class appUsers implements Serializable {

    private String id;
    private String name;
    private String email;
    private String role;               // "professional" or "customer"
    private double rating;             // default 0.0
    private int ratingCount;           // default 0
    private String profilepic;         // URL
    private List<String> portfolioImages; // URLs
    private List<Service> services;    // nested service objects
    private List<String> tags;         // service tags

    private Address address;           // nested address map
    private Geo geo;
    //phone number
    private String phoneNumber;

    // nested geo map

    // Default constructor needed for Firestore
    public appUsers() {}

    // Full constructor (services/portfolio/tags can be empty at signup)
    public appUsers(String id, String name, String email, String role,
                    Address address, Geo geo, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.rating = 0.0;
        this.ratingCount = 0;
        this.profilepic = "";
        this.portfolioImages = new ArrayList<>();
        this.services = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.address = address;
        this.geo = geo;
        this.phoneNumber = phoneNumber;
    }

    // Getters and setters
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
    public void setPortfolioImages(List<String> portfolioImages) { this.portfolioImages = portfolioImages; }

    public List<Service> getServices() { return services; }
    public void setServices(List<Service> services) { this.services = services; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public Geo getGeo() { return geo; }
    public void setGeo(Geo geo) { this.geo = geo; }

    public String getPhoneNumber() { return phoneNumber; }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }


    // Nested classes
    public static class Address implements Serializable {
        private String street;
        private String room;
        private String city;
        private String province;
        private String country;
        private String postalCode;

        public Address() {}

        public Address(String street, String room, String city, String province, String country, String postalCode) {
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
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    }

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

    public static class Service implements Serializable {
        private String name;
        private double price;
        private int duration;

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
