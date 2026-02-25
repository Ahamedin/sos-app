# 🚨 SOS Emergency App

An Android emergency safety application that sends an SOS message with live location to registered contacts when the device is shaken.

---

## 📱 Features

- 📳 Shake Detection using Accelerometer
- 📍 Sends Live Google Maps Location
- 📩 SMS Alert to Saved Contacts
- 👥 Add & Manage Emergency Contacts
- 📝 Custom Emergency Message
- 🚑 Emergency Information Section
- 🔔 Foreground Monitoring Service

---

## 🛠 Tech Stack

- Java
- Android SDK
- SQLite Database
- FusedLocationProviderClient
- SMS Manager
- Material Design Components

---

## ⚙️ How It Works

1. User registers emergency contacts.
2. User starts monitoring service.
3. App runs in foreground.
4. When shake is detected:
   - Gets current location
   - Generates Google Maps link
   - Sends SOS SMS to all saved contacts

---

## 📦 Installation (For Developers)

### 1️⃣ Clone Repository

```bash
git clone https://github.com/Ahamedin/sos-app.git
```
### 2️⃣ Open in Android Studio

Open Android Studio

Click Open

Select cloned folder

Sync Gradle

### 3️⃣ Required Permissions

App requires:

SEND_SMS

ACCESS_FINE_LOCATION

ACCESS_COARSE_LOCATION

VIBRATE

⚠️ Must run on real device (SMS won’t work on emulator)
