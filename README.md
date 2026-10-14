<div align="center">

# 📱 MeetTrack

A modern Android app for managing clients, scheduling meetings, capturing GPS locations, and sending actionable meeting reminders.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Design%20System-795548?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Room](https://img.shields.io/badge/Room-SQLite%202.6.1-47A248?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Hilt](https://img.shields.io/badge/DI-Dagger%20Hilt%202.54-00599C?style=for-the-badge&logo=dagger&logoColor=white)](https://dagger.dev/hilt/)

[Key Features](#key-features) • [Tech Stack](#architecture--tech-stack) • [Project Structure](#project-structure) • [Getting Started](#getting-started) • [Backup & Export](#backup--export) • [License](#license)

</div>

---

> [!TIP]
> MeetTrack is built for field professionals who need fast client access, meeting tracking, and exact location-aware reminders—without relying on the internet for daily tasks.

> [!NOTE]
> Maps and reverse geocoding require a network connection. Client and meeting records are stored locally on the device.

## Overview

MeetTrack is a native Android application built with Jetpack Compose and Material 3 for managing clients, scheduling meetings, capturing home or office locations, and staying on top of daily appointments through smart reminders.

It helps users keep contact details organized, know where to meet someone, and never miss an important meeting thanks to exact alarm scheduling and offline-first local storage.

## Key Features

### Client Management
- Full client profiles with name, phone, email, company, address notes, and creation date
- One-tap GPS capture using Android Location Services
- Quick call, email, and navigation actions from the client detail view
- Real-time search and filtering by name, phone, or company

### Meeting Scheduling & Calendar
- Create and update meetings with title, date, time, location, and agenda notes
- Meeting status tracking: `UPCOMING`, `COMPLETED`, `CANCELLED`, and `RESCHEDULED`
- Interactive month-based calendar with agenda visibility per day
- Quick reschedule and completion actions from meeting cards

### Location & Navigation
- View client locations on an embedded Google Map
- Launch turn-by-turn navigation with prefilled coordinates
- Keep track of the last location capture timestamp

### Reminders & Notifications
- Reminder intervals including 10 minutes, 30 minutes, 1 hour, 2 hours, and 1 day
- Exact alarms with `AlarmManager.setExactAndAllowWhileIdle`
- Actionable notifications to navigate, call, or open the meeting details
- Auto-reschedule alarms after device reboot or app update

### Data Persistence & Backup
- Offline-first storage using Room SQLite
- JSON backup and restore support
- CSV export for clients and meetings in spreadsheet-friendly formats

### Modern UI
- Material 3 theming with light and dark mode support
- Edge-to-edge Android UI patterns
- Clean, responsive composition for day-to-day usability

---

## Architecture & Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| Dependency Injection | Dagger Hilt |
| Database | Room SQLite with Flow & Coroutines |
| Preferences | AndroidX DataStore |
| Scheduling | AlarmManager + BroadcastReceiver |
| Location & Maps | Google Play Services Location + Maps Compose |
| Serialization | Gson |
| Build System | Gradle Kotlin DSL |

### Core Versions
- Kotlin: `2.1.0`
- Android compile SDK: `35`
- Minimum SDK: `26`
- Room: `2.6.1`
- Hilt: `2.54`
- Compose BOM: `2024.12.01`

---

## Project Structure

```text
MeetTrack/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/meettrack/app/
│       │   ├── MainActivity.kt
│       │   ├── MeetTrackApplication.kt
│       │   ├── data/
│       │   │   ├── backup/
│       │   │   ├── db/
│       │   │   ├── model/
│       │   │   ├── preferences/
│       │   │   └── repository/
│       │   ├── di/
│       │   ├── location/
│       │   ├── notifications/
│       │   └── ui/
│       └── res/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── README.md
└── LICENSE
```

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+) or later
- JDK 17
- Android device or emulator running API 26+ (Android 8.0+)

### Setup
```bash
git clone https://github.com/Cyrus-Hackman/MeetTrack.git
cd MeetTrack
```

1. Open the project in Android Studio.
2. Let Gradle sync dependencies.
3. Choose a device or emulator.
4. Run the app from Android Studio or use:

```bash
./gradlew assembleDebug
```

### Google Maps Configuration
To enable in-app maps, add your Google Maps API key to the ignored `local.properties` file at the project root:

```properties
MAPS_API_KEY=your_google_maps_api_key
```

The project builds without a key, but map tiles will not load until one is configured. Maps and reverse geocoding also require a network connection.

---

## Backup & Export

MeetTrack includes easy data portability options:

- Full JSON backup and restore
- CSV export for clients and meetings
- Device-storage-compatible export flow using Android file access APIs

This keeps your records accessible when switching devices or migrating data.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more information.

<div align="center">

Built with ❤️ using Jetpack Compose and Material 3.

</div>
