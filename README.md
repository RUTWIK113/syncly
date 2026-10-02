# Syncly 🎯📅

> **Turn campus updates into calendar events.**

**Syncly** is an Android application designed for IIT Madras students to automatically capture WhatsApp placement announcements and notices (Pre-Placement Talks, Online Assessments, Interviews, PPIs, Company Sessions, and Deadlines), extract event metadata with AI & NLP heuristics, auto/manually join WhatsApp groups, and sync everything cleanly into Google Calendar via Android `CalendarContract`.

---

## 🎨 Design & Aesthetic
* **Monochrome Palette**: High-contrast black, neutral gray, and crisp white theme in both Dark Mode and Light Mode.
* **Minimalist & Clean**: Designed with sharp card borders, clear typography hierarchy, and distraction-free layouts.

---

## 🚀 Key Features

### 1. WhatsApp Notice Detection
* Uses Android `NotificationListenerService` strictly for `com.whatsapp` and `com.whatsapp.w4b`.
* **Zero Chat Storage**: Operates solely on notification preview text (`EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_TITLE`). Never touches private WhatsApp chat databases.
* Deduplicates notifications via local MD5 hashes.

### 2. Auto / Manual WhatsApp Group Joining
* **Automatic Mode**: When enabled in Settings, Syncly automatically launches WhatsApp invite links (`chat.whatsapp.com`) as soon as a placement notice arrives.
* **Manual Mode**: One-tap **"👥 Join WA Group"** action button right in the notification bar and in the in-app event card.

### 3. Dual-Engine Extraction (AI + Offline Heuristics)
* **Google Gemini AI**: Connects to Gemini Flash for deep NLP event classification.
* **Offline Campus Heuristic Fallback**: Built-in regex engine tuned for IIT Madras halls (`CLT`, `ICSR Auditorium`, `SAC`, `CRC`, `NAC`, `RMN 101`, `MSB`), assessment platforms (`HackerRank`, `HackerEarth`, `Mettl`, `Superset`), and Indian date/time styles.

### 4. Rich Google Calendar Integration
* **Title**: `[Company] | [Event Type]` (e.g. `[Honda R&D] | [Pre-Placement Talk]`).
* **Description**: Captures all key points, profiles offered, eligibility, portal outage notes, venue, and WhatsApp group link.
* **Reminders**: Automatically adds a **60-minute reminder** before every talk, test, or interview.

---

## 🛠 Tech Stack
* **Language**: Kotlin 2.0
* **UI**: Jetpack Compose (Material 3 Monochrome)
* **Architecture**: Clean Architecture (Domain, Data, Presentation)
* **Database**: Room Persistence Library
* **Settings**: Jetpack DataStore Preferences
* **Calendar Sync**: Android `CalendarContract`
