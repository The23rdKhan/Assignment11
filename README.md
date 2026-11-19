# Assignment 11 - Android Trip Planner (100 Points)

**Full Name of Student:** Lucnel Nordelus  
**Course:** Mobile App Development - Assignment 11  

---

## Basic Instructions & Compliance
1. **File Headers:** Every submitted Java file includes:
   ```java
   /**
    * Assignment 11
    * File Name: [ActualFileName.java]
    * Full Name of Student: Lucnel Nordelus
    */
   ```
2. **Submission Archive:** The project is zipped into `Assignment11.zip` (excluding generated build artifacts, `.gradle`, `.idea`, and `local.properties`).

---

## Assignment Overview & APIs Used
In this assignment, we implement a direction and place trip planning app using several Google APIs and Firebase:
1. **Places Autocomplete SDK**: Used for picking start and end locations.
2. **Routes API**: Used for finding driving directions between start and destination.
3. **Places API**: Used for finding gas stations at the start, end, and mid path locations.
4. **Firebase Authentication**: Used for user login and registration.
5. **Cloud Firestore**: Used for storing and retrieving trip plans.
6. **Google Maps SDK for Android**: Used for map visualization and markers.

---

## App Requirements
1. **Firebase Authentication:** Allow users to login and register with persistent sessions. Trips are associated with the authenticated user's UID.
2. **Google Maps Integration:** Integrate Google Maps in the app.
3. **Trip Planning & Firestore Storage:** Allow the user to create trip plans, which include the start location, destination location, path from start to destination, and gas stations at start, end, and mid path locations. Stored under Firebase Firestore.
4. **Adding a New Trip:**
   - Pick start location and end location using the Places Autocomplete SDK.
   - Display location addresses on the screen after selection.
   - When both start and end locations are selected, use the **Routes API** to find directions between start and end.
   - Display the returned route from the Routes API on the Google Map as a polyline.
   - Using the **Places API**: find gas stations at the start, end, and mid path locations and display the results on the Google Map.
5. **Viewing Stored Trips:**
   - Users can view their stored trips.
   - Upon selecting a stored trip, all trip information is loaded from Firebase.
   - Display the returned route on the Google Map as a polyline.
   - Display the stored gas stations at the start, end, and mid path locations loaded from Firebase.

---

## Firestore Security Rules
Publish the following rules in your Firebase Console under **Firestore Database -> Rules**:

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
