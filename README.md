# ADNM (Waste Management & Recycling)

ADNM is a mobile application designed to bridge the gap between waste reporters and collectors. The platform encourages environmental cleanliness by allowing users to report waste locations and enabling collectors to gather recyclables in exchange for rewards.

## 🚀 Key Features

- **Dual Role System:**
    - **Reporter:** Easily report waste locations with photos and descriptions.
    - **Collector:** Locate reported waste on a map and use real-time routing to reach them.
- **Real-time Map Integration:** Uses Google Maps to display waste markers with status-coded colors (Accepted, In-Progress, Collected).
- **Smart Routing:** Integrated with OSRM (Open Source Routing Machine) to provide collectors with optimal driving directions.
- **Push Notifications:** Powered by Firebase Cloud Messaging (FCM) to notify collectors of new reports and reporters of status updates.
- **Multi-language Support:** Full localization in **English**, **Arabic**, and **French**.
- **Secure Authentication:** JWT-based authentication with role-based navigation.
- **In-App Updates:** Integrated with Google Play In-App Updates to ensure users always have the latest features.

## 🛠 Tech Stack

- **Language:** Kotlin
- **Architecture:** Android ViewBinding, MVC/Clean approaches.
- **Networking:** Volley for API requests.
- **Maps & Location:** Google Play Services (Maps & Location).
- **Notifications:** Firebase Cloud Messaging (FCM).
- **Routing:** OSRM Driving API.
- **Backend:** Integrated via RESTful API (krim-rachida-recyclage.dz).

## 📦 Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/ADNM.git
    ```
2.  **Open in Android Studio:**
    Ensure you are using the latest version of Flamingo or Hedgehog.
3.  **API Keys:**
    - Add your `google_maps_key` in `res/values/strings.xml`.
    - Ensure `google-services.json` is placed in the `app/` directory.
4.  **Build:**
    Sync project with Gradle files and run on an emulator or physical device (Min SDK 26).

## 🌍 Localization

The app detects system language and switches between:
- 🇺🇸 **English** (Default)
- 🇩🇿 **Arabic** (Local terminology included)
- 🇫🇷 **French**

## 📝 Recent Version History (v4.2.0)

- **Fixes:** Resolved critical `DeadObjectException` and OSRM routing headers.
- **Stability:** Improved Login flow and FCM token synchronization.
- **Features:** Added full Arabic and French translation suites.
- **SDK Update:** Target SDK updated to 35 (Android 15).

## 📄 License

Copyright © 2024 DLD Development. All rights reserved.
