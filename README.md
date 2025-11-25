# 📱 The Chair – Beauty & Haircare Booking App

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase_Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Google Maps](https://img.shields.io/badge/Google_Maps_SDK-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)
![Glide](https://img.shields.io/badge/Glide-33B5E5?style=for-the-badge&logo=android&logoColor=white)
![Material Design](https://img.shields.io/badge/Material_Design_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

## 📸 Screenshots

<h3>Login & Signup</h3>
<p align="center">
  <img src="ScreenShots/LoginScreen.png" width="250"/>
  <img src="ScreenShots/SignUp.png" width="250"/>
  <img src="ScreenShots/SignUp2.png" width="250"/>
  <img src="ScreenShots/SignUp3.png" width="250"/>
</p>

<h3> Home and Search </h3>
<p align="center">
  <img src="ScreenShots/CustomerHomeScreen.png" width="250"/>
  <img src="ScreenShots/CustomerSearchcreen.png" width="250"/>
</p>

<h3>Nearby Map</h3>
<p align="center">
  <img src="ScreenShots/CustomerNearbyScreen.png" width="250"/>
</p>


<h3>Bookings</h3>
<p align="center">
  <img src="ScreenShots/CustomerBookingScreen.png" width="250"/>
</p>


<h3>Profile</h3>
<p align="center">
  <img src="ScreenShots/CustomerProfileScreen.png" width="250"/>
  <img src="ScreenShots/CustomerEditProfileScreen.png" width="250"/>
</p>

### Professional – Add Service
<p align="center">
  <img src="ScreenShots/ProfessionalAddService.png" width="250"/>
</p>

### Professional – Set Availability
<p align="center">
  <img src="ScreenShots/ProfessionalAvailability.png" width="250"/>
</p>

### Professional – Home Screen
<p align="center">
  <img src="ScreenShots/ProfessionalHomeScreen.png" width="250"/>
</p>

### Professional – Profile Screen
<p align="center">
  <img src="ScreenShots/ProfessionalProfileScreen.png" width="250"/>
</p>

### Professional – Public Profile Screen
<p align="center">
  <img src="ScreenShots/ProfessionalPublicProfileScreen.png" width="250"/>
</p>







---

# 🚀 Features

## Customer Features
- Interactive Google Map with radius filters
- Search by name, profession, service, or tags  
- Directions via external Google Maps  
- Booking lifecycle (pending → accepted → completed → rated)  
- Cancel, rebook, and rate sessions  
- View Today / Upcoming / Past bookings  

## Professional Features
- Edit profile, services, and portfolio  
- Set availability  
- Manage bookings  
- Receive ratings and build reputation  

---

# 🔥 Firebase Integration

### Authentication
Email/password login  

### Firestore Structure

```
Users/{uid}/
    name
    role
    profession
    address{}
    geo
    profilepic
    services[]
    tags[]
    bookings/
    ratings/
```

### Storage
- Profile images  
- Portfolio images  

---

# 🧱 App Architecture

```
/AuthFlow
/Customer
/Professional
/Adapters
/Models
/Utils
```

---

# 🧭 Booking Lifecycle

- pending  
- accepted  
- completed  
- cancelled  

Mirrored data under:

```
Users/{customerId}/bookings/
Users/{professionalId}/bookings/
```

---

# ⭐ Rating System

Weighted formula:

```
newAverage = ((oldRating * count) + newRating) / (count + 1)
```

Stored fields:
```
customerName
customerID
rating
comment
timestamp
```

---

# ⚙️ Installation & Setup

### 1. Clone
```
git clone https://github.com/yourusername/TheChair.git
cd TheChair
```

### 2. Open in Android Studio  
Open → Existing Project → Wait for Gradle Sync

---

# 🔐 API Keys

### Google Maps SDK Key

Enable:
- Maps SDK
- Geocoding API

Add to `local.properties`:

```
MAPS_API_KEY=your_key_here
```

Add to Manifest:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

---

# 🔥 Firebase Setup

### 1. Create Firebase Project  
Enable Auth, Firestore, Storage

### 2. Download google-services.json  
Place into:

```
/app/google-services.json
```

### 3. Recommended Firestore Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /Users/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;

      match /bookings/{bookingId} {
        allow read, write: if request.auth != null;
      }

      match /ratings/{ratingId} {
        allow create: if request.auth != null;
      }
    }
  }
}
```

---

# ▶️ Running the App

1. Connect Android device  
2. Enable USB debugging  
3. Run from Android Studio  

---

# ❗ Troubleshooting

### Blank Google Map  
- Incorrect API key  
- Maps SDK disabled  
- Billing disabled  

### Missing pro marker image  
- Storage rule issues  

### Crash tapping marker  
- Missing geo field  

---

# 👥 Contributing

1. Fork  
2. Create feature branch  
3. Commit  
4. Open pull request  

---

# 📄 License  
© 2025 Shaquille Neil. All rights reserved.


