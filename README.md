# Assignment 11 - Android Trip Planner

**Full Name of Student:** Lucnel Nordelus  
**Course / Assignment:** Assignment 11  

---

## Overview
Assignment 11 is an Android application built in Java that allows users to plan road trips, view custom driving routes using the Routes API v2, discover gas stations along the route using the Places API (New), and securely save or manage their trips using Firebase Authentication and Cloud Firestore.

## Features
- **Firebase Authentication:** Secure user registration, login, logout, and persistent sessions.
- **Places Autocomplete:** Utilizes `PlaceAutocomplete.IntentBuilder` and Places API (New) to select start and destination locations.
- **Driving Route & Polyline:** Fetches and draws a blue driving route polyline between start and destination using Google Routes API v2.
- **Gas Station Finder:** Automatically searches for and displays gas stations near the start, midpoint, and destination using Places API (New) nearby search.
- **Cloud Firestore Storage:** Saves trips associated with the authenticated user's UID.
- **Saved Trips Management:** View saved trips, inspect routes on the map, and delete saved trips with confirmation dialogs.
- **Secure Firestore Rules:** Ensures authenticated access and strict ownership enforcement.

---

## Tech Stack & Libraries
- **Language:** Java
- **UI Architecture:** Fragments, ViewBinding, ConstraintLayout, Material Design 3
- **Google Maps & Places:** Google Maps SDK for Android (`play-services-maps`), Places SDK (`6.0.2`), Places API (New), Routes API v2, Android Maps Utils (`PolyUtil`, `SphericalUtil`)
- **Backend & Database:** Firebase Authentication, Cloud Firestore
- **Networking & JSON:** OkHttp, Gson

---

## Firestore Security Rules
Make sure to publish the following rules in your Firebase Console under **Firestore Database -> Rules**:

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /trips/{tripId} {
      allow read, delete: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
      allow update: if request.auth != null && request.auth.uid == resource.data.userId && request.resource.data.userId == resource.data.userId;
    }
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## Building and Running
1. Clone the repository.
2. Add your Google Maps API key in `res/values/strings.xml` (`maps_api_key`).
3. Add your `google-services.json` file in the `app/` directory.
4. Build and run in Android Studio or via Gradle:
   ```bash
   ./gradlew clean assembleDebug
   ```
