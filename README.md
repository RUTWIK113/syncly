# ✨ Syncly — Placement Intelligence & Auto-Calendar Sync

<div align="center">

![Syncly Logo](app/src/main/res/drawable/syncly_logou.png)

### **Smart Placement & Internship Event Automation for Engineering Students**
*Never miss an assessment, interview slot, pre-placement talk, or incentive point again.*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_26+-green.svg?style=for-the-badge&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room DB](https://img.shields.io/badge/Room_DB-Offline_First-orange.svg?style=for-the-badge&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Firebase](https://img.shields.io/badge/Firebase-Analytics_%26_Realtime_DB-FFCA28.svg?style=for-the-badge&logo=firebase)](https://firebase.google.com)

---

</div>

## 📌 Problem Statement
During placement and internship seasons, campus placement cells and coordinators post hundreds of urgent WhatsApp notices across multiple batch groups. Important test links, Zoom interview schedules, submission deadlines, and pre-placement talks easily get lost in chat noise, resulting in missed tests and forfeited opportunities.

**Syncly** solves this by listening to incoming placement notices in real-time, intelligently extracting dates, timings, company names, round types, and incentive points via a hybrid rule + Gemini AI engine, and syncing them into your Google Calendar.

---

## 🚀 Key Features

### 🔔 1. Real-Time Notification Intelligence
- **Zero-Latency Listener**: Intercepts WhatsApp placement notifications immediately in the background (`NotificationListenerService`).
- **Hybrid Extraction Engine**:
  - **Rule-Based Extractor**: Ultra-fast local regex parsing for companies, dates (`Tomorrow`, `15th Oct`, `Friday`), times (`10:00 AM`, `14:30`), and links (`Unstop`, `HackerRank`, `Google Meet`, `Zoom`, `MS Teams`).
  - **Gemini AI Fallback**: Seamlessly handles unstructured, ambiguous, or multi-paragraph announcements.
- **Smart Duplicate Prevention**: Ensures identical notices forwarded across multiple groups are never duplicated.

### 📅 2. Google Calendar Integration
- **1-Tap & Automated Sync**: Add events directly to your native Google Calendar with custom alerts (15m, 1h, 1d before).
- **Color Coded by Round Type**:
  - 🔵 **OA / Online Assessment** (Tomato / High Priority)
  - 🟣 **Technical / HR Interview** (Grape / Critical)
  - 🟡 **Pre-Placement Talk (PPT)** (Banana / Informational)
  - 🟢 **Shortlist / Result** (Basil)
  - 🟠 **Deadline / Registration** (Flamingo)
- **Fallback Intent Flow**: Even if calendar provider permissions are restricted, opens Google Calendar with pre-populated fields.

### 💼 3. Placement Vault & Company Dossiers
- **Company Tracking**: Organize companies into categorized dossiers with role titles, CTC/stipend details, and process stages.
- **Document & JD Storage**: Attach Job Descriptions (JDs), PDFs, and notes stored in a dedicated local `Syncly` directory.
- **Round Timeline**: Automatically links upcoming test and interview dates to their respective company profile.

### 🏆 4. Incentive & Attendance Points Ledger
- **Automatic Points Extraction**: Detects `"5 points"`, `"10 incentive points"`, etc. in announcements and displays a high-visibility badge.
- **Personal Points Ledger**: Record attendance, pre-placement talks, and hackathon scores to maintain a cumulative score.
- **Smart Filter Chips**: Filter your feed by `All`, `Points = Yes`, and `Points = No`.

### 🛡️ 5. Privacy & Offline-First Design
- **Local SQLite / Room Database**: All events, company dossiers, and notes remain on your device.
- **Zero Sensitive Data Exposure**: Admin emails and secret credentials are fully safeguarded.
- **Non-Intrusive Telemetry**: Aggregated anonymous stats stream to Firebase Realtime DB for analytics.

---

## 🏗️ Architecture & Tech Stack

```
com.ppicalendar.app/
├── data/
│   ├── calendar/          # Android Calendar Provider Contract & Intent Helpers
│   ├── datastore/         # Jetpack DataStore Preferences
│   ├── extractor/         # RuleBasedExtractor, GeminiApiExtractor & DateTimeParser
│   ├── local/             # Room Database (Entities, TypeConverters, DAOs)
│   ├── notification/      # NotificationListenerService & Heads-Up Alerts
│   ├── repository/        # Clean Architecture Repository Implementations
│   └── telemetry/         # Firebase Realtime Telemetry Sync
├── di/                    # Manual Dependency Injection (AppContainer)
├── domain/                # Use Cases, Domain Models, & Repository Contracts
├── presentation/          # Jetpack Compose UI (Screens, ViewModels, Dialogs, Theme)
└── ui/theme/              # Design System (Amber #FFC212, Dark Charcoal #171721)
```

| Layer | Technologies Used |
| :--- | :--- |
| **UI & Presentation** | Jetpack Compose, Material 3, Compose Navigation, StateFlow |
| **Architecture** | MVVM + Clean Architecture + Repository Pattern |
| **Local Database** | Room SQLite DB v4 (Entities: Events, Companies, Attachments, Points) |
| **Background Processing** | Android `NotificationListenerService`, Kotlin Coroutines |
| **Networking & AI** | OkHttp 4, Kotlinx Serialization, Google Gemini 1.5 Flash API |
| **Cloud & Analytics** | Firebase Android BoM (Analytics, Realtime Database) |

---

## 🛠️ Getting Started & Build Instructions

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17+
- Android Device or Emulator with **Android 8.0 (API level 26)** or higher

### 1. Clone the Repository
```bash
git clone https://github.com/RUTWIK113/syncly.git
cd syncly
```

### 2. Add Firebase Configuration
1. Create a project on the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app with package name `com.ppicalendar.app`.
3. Download `google-services.json` and place it in the `app/` directory:
   ```
   app/google-services.json
   ```

### 3. Build & Run
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 📸 Screenshots & UI Design System

* Designed around the signature **Syncly Amber** (`#FFC212`) accent on a deep **Dark Charcoal** (`#171721`) background.
* High readability typography and fluid Jetpack Compose micro-interactions.

---

## 👨‍💻 Author

**Crafted with ❤️ by Rutwik**
- GitHub: [@RUTWIK113](https://github.com/RUTWIK113)
- Project: [Syncly](https://github.com/RUTWIK113/syncly)

---

## 📄 License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
