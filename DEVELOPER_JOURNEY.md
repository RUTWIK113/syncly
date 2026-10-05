# Syncly: The Complete Developer Journey

*A comprehensive guide on building a context-aware Android application, navigating modern Android APIs, and solving real-world problems.*

---

## 1. Introduction & Motivation

**The Problem:** During university placement seasons, students are bombarded with hundreds of WhatsApp messages across various groups. Critical updates—like Pre-Placement Talks (PPTs), Online Assessments (OAs), or Interview shortlists—easily get buried under casual chatter. Missing a deadline can cost a student a job opportunity.

**The Solution:** **Syncly**. An automated, intelligent Android application that runs silently in the background. It intercepts WhatsApp notifications, uses a hybrid AI/Regex engine to determine if the message is a placement event, extracts critical information (Company, Date, Time, Venue, Meeting Links), and seamlessly syncs it to Google Calendar with a 1-hour exact reminder.

---

## 2. The Tech Stack & Architecture (And Why We Chose It)

Building a modern Android app requires choosing tools that prioritize maintainability, performance, and developer ergonomics. 

*   **Language:** **Kotlin**. (100% Kotlin codebase). It offers null-safety, concise syntax, and native support for asynchronous programming via Coroutines.
*   **UI Toolkit:** **Jetpack Compose**. Instead of legacy XML layouts, we used Compose. It is declarative, meaning the UI automatically updates when the underlying data state changes. It drastically reduced the time needed to build complex screens like the `EventDetailDialog` and dual-feed `HomeScreen`.
*   **Architecture:** **MVVM (Model-View-ViewModel) + Clean Architecture**.
    *   *Domain Layer*: Use cases (e.g., `ProcessNotificationUseCase`) hold the pure business logic.
    *   *Data Layer*: Repositories, Room Database (SQLite), and API clients.
    *   *Presentation Layer*: ViewModels (`MainViewModel`) manage StateFlows, and Compose handles rendering.
*   **Local Persistence:** 
    *   **Room Database**: For storing the `PlacementEvent` entities reliably.
    *   **DataStore (Preferences)**: A modern replacement for `SharedPreferences` to store lightweight settings (like Dark Mode preferences and API keys).
*   **Dependency Injection (DI):** **Manual DI (AppContainer)**. For a lightweight utility app, pulling in heavy frameworks like Hilt or Dagger can overkill the build times. A simple `AppContainer` interface instantiated in the `Application` class kept the architecture modular but lean.
*   **Intelligence:** **Google Gemini Pro API**. Used for unstructured text extraction, parsing chaotic human-written WhatsApp notices into structured JSON.

---

## 3. Deep Dive: How the Core Mechanics Work

### A. The Interception Engine (`WhatsAppNotificationListenerService`)
Android allows apps to listen to system notifications via `NotificationListenerService`. 
1. The service filters incoming notifications to only process `com.whatsapp`.
2. It ignores ongoing calls or "Group Summary" notifications.
3. It extracts the `EXTRA_TEXT` and passes it to the extraction pipeline.

### B. The Hybrid Extraction Pipeline
Not every message is a placement notice. The `ProcessNotificationUseCase` handles the filtering:
1. **Keyword Guard**: Checks against a whitelist (e.g., "PPT", "OA", "Registration"). If missing, it immediately drops the notification to save battery and API calls.
2. **Casual Chat Filter**: Suppresses messages starting with "Can anyone...", "Good morning", etc.
3. **Extraction**:
    *   **Primary (AI)**: Sends the text to Gemini to return a structured JSON response (Company, EventType, Date).
    *   **Fallback (Regex)**: If offline, rate-limited, or if the API key is missing, `HybridEventExtractor` instantly falls back to `RuleBasedExtractor`, which uses robust Regex patterns `(?i)\b(ppt|pre-placement talk)\b` to extract data.

### C. The Synchronization Engine
Once an event is approved, it needs to be pushed to Google Calendar.
*   We interact directly with the Android `CalendarContract` Content Provider using `ContentResolver`.
*   We insert the event into the user's primary calendar.
*   **The Sync Trick**: Inserting locally isn't enough; we programmatically request a cloud sync using `ContentResolver.requestSync()` targeting both `CalendarContract.AUTHORITY` and `com.google.android.calendar` so the event appears instantly on the user's laptop browser.

---

## 4. The Chronological Journey: Problems Faced & Solutions

### Phase 1: Foundation & The Android 13+ Notification Block
**The Goal:** Get the app to read WhatsApp messages.
**The Problem:** Android 13 introduced "Restricted Settings" for sideloaded apps. By default, Android grays out the "Allow Notification Access" toggle for APKs installed from a browser, preventing the app from working entirely.
**The Solution:** Built a dedicated `SettingsScreen` onboarding helper. We implemented a 1-tap intent that opens the exact App Info screen and provided visual instructions telling the user to click the three dots (⋮) and tap "Allow restricted settings" before requesting the permission again.

### Phase 2: Intelligence & Noise Reduction
**The Goal:** Parse the messages accurately.
**The Problem:** The app was picking up conversational noise ("*Did anyone get the PPT link?*") and logging them as calendar events. Also, WhatsApp dates are often conversational ("tomorrow", "Monday").
**The Solution:** 
1. Implemented a strict whitelist and blacklist algorithm.
2. Created `ResolveDateUseCase`, a custom date-math utility that takes conversational dates and calculates the absolute `LocalDate` based on the notification timestamp.

### Phase 3: Cloud Synchronization Conflicts
**The Goal:** Push the extracted events to Google Calendar.
**The Problem:** The app successfully created events, but they only existed on the phone's local storage. Furthermore, some Android skins (like Xiaomi/Samsung) crashed when we tried to write to the `CalendarContract.Events.VISIBLE` column.
**The Solution:** 
Removed all restricted column writes to guarantee cross-device compatibility. We added `READ_SYNC_SETTINGS` and `WRITE_SYNC_SETTINGS` permissions to manually command the Android OS to push the local calendar database to Google's cloud servers instantly.

### Phase 4: Precision Timers & Exact Alarms
**The Goal:** Alert the user 1 hour before the event.
**The Problem:** Android 14 heavily restricts `SCHEDULE_EXACT_ALARM` to save battery. If we tried to schedule an alarm, the app threw a fatal `SecurityException`.
**The Solution:** Wrapped the `AlarmManager.setExactAndAllowWhileIdle()` call in a strict OS-version check and a `try/catch` block. This provided graceful degradation—if the permission was revoked, the app survived and fell back to standard notifications.

### Phase 5: Over-The-Air (OTA) Updates & The Sideloading Bug
**The Goal:** Distribute updates seamlessly without the Google Play Store.
**The Problem:** We built a custom GitHub Pages backend serving a `version.json` file. When the app detected a new version, it downloaded the APK via Chrome. However, when users tapped "Open" in Chrome's download popup, Android threw a "Can't open file" / "Failing to open" error due to Chrome lacking package installer permissions.
**The Solution:** Handled it via UX documentation. We added explicit instructions on the website advising users to install updates via their native "File Manager" app, completely bypassing the browser restriction.

---

## 5. Version Iteration History

*   **v1.0.0 (Build 1)**: Initial core engine. Notification listening and basic DB insertion.
*   **v1.0.4 - v1.0.7**: Added the dual-feed UI. Separated "Pending Confirmation" from "Synced" events so users retain control over what enters their calendar. Added a Manual Entry Sandbox.
*   **v1.0.8**: Introduced the Android 13+ Accessibility Helper.
*   **v1.0.9**: Built the `CheckAppUpdateUseCase` reading from `version.json` for OTA updates, ensuring static `BuildConfig` version comparisons to prevent redundant prompts.
*   **v1.1.0**: Perfected the Dual-Authority Calendar Sync. Enforced strict casual-chat filtering.
*   **v1.1.1 - v1.1.2**: Polish phase. Added 1-hour Exact Alarm Reminders. Overhauled the Manual Parser to forcibly override keyword whitelists so users can manually inject any event. Cleaned up the Compose UI by removing redundant TopBar icons.

---

## 6. Key Learnings & Best Practices for Beginners

If you are learning Android development by reading this repository, keep these core philosophies in mind:

1.  **State is King (Use StateFlow + Compose):** Never manually update a text view. Read your Room Database directly into a `StateFlow`. Let Jetpack Compose observe that flow. When the database updates, the UI magically updates itself. It eliminates an entire class of synchronization bugs.
2.  **Graceful Degradation:** The Android ecosystem is heavily fragmented. Permissions change every API level (API 31 for exact alarms, API 33 for POST_NOTIFICATIONS). Always wrap sensitive API calls in `if (Build.VERSION.SDK_INT >= ...)` checks and `try/catch` blocks. If a feature fails, the app should continue running.
3.  **Clean Architecture Saves Time:** By completely separating the `RuleBasedExtractor` from the Android framework, we could write pure Kotlin unit tests for our Regex algorithms without booting up an emulator. 
4.  **Security by Default:** We kept the Gemini API Key strictly in local `DataStore` (never hardcoded), and ensured the app functions 100% offline using Regex if the user chooses not to use AI.

---

*Built with Kotlin ❤️*
