# Nijatva Dialer

A fast, private, offline-first dialer, contacts and SMS app for Android. Built on the AOSP dialer core and heavily customised — **no ads, no trackers, no internet permission. Your data never leaves your phone.**

![minSdk](https://img.shields.io/badge/minSdk-21-blue) ![targetSdk](https://img.shields.io/badge/targetSdk-34-green) ![license](https://img.shields.io/badge/license-Apache--2.0-orange)

## Features

**Phone & Dialer**
- Speed dial (keys 2–9) with avatars and search
- Spam, scammer and promotional call blocking (140-series, private numbers, custom blacklist)
- Caller ID popup with photo
- Fake call, flash alert, call announcer, hide number
- App lock with PIN or pattern

**Messages (SMS)**
- Smart categories: Personal, OTPs, Others
- Expired OTP auto-delete (24 hours)
- Spam and fraud SMS protection
- Delivery reports and swipe actions

**Contacts**
- Photo avatars, fast search, sort options
- Account filters (SIM, WhatsApp, Google and others)
- Merge, import/export and favourites

**Appearance & About**
- Theme: System, Dark, Light, Black (AMOLED)
- Accent colour: Green, Saffron, Indigo
- Developer page with project links
- **Nijatva apps** — soft links to Gallery, Photos, Camera and Files

## Download

Grab the latest APK from the [Releases](../../releases) page. It is about 510 KB.

## Build

Pure framework Java — no AndroidX, no third-party libraries.

```
./gradlew assembleRelease
```

Open the project in Android Studio, or build from the command line. Signing configuration is intentionally omitted — use your own keystore.

## Permissions

Nijatva Dialer requests Phone/SMS/Contacts permissions because it *is* your phone and SMS app. It is a default-handler app in the spirit of the AOSP dialer. There is no network access.

## The Nijatva family

The app carries the shared Nijatva signature: the Appearance and Developer screens come from the same source as the other Nijatva apps, so the whole family looks and reads the same. `NijatvaMediaIntents` provides soft hand-offs to the sibling apps — every call is optional, so a missing app never crashes this one.

## Notes for contributors

- The optional "Support the developer" donation button is disabled in this public source. To enable it in your own build, set `SUPPORT_UPI_ID` in `SettingsActivity.java`.
- Android backup is disabled by design (`allowBackup="false"`).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
