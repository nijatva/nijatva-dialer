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
- Expired OTP auto-delete
- Spam and fraud SMS protection
- Delivery reports and swipe actions

**Contacts**
- Photo avatars, fast search, sort options
- Account filters (SIM, WhatsApp, Google and others)
- Merge, import/export and favourites

**Appearance**
- Theme: System, Dark, Light, Black (AMOLED)
- Accent colour: Green, Saffron, Indigo
- Saved on the device only

**About**
- Developer page with project links

## Download

Grab the latest APK from the [Releases](../../releases) page.

## Build

Pure framework Java — no AndroidX, no third-party libraries.

```
./gradlew assembleRelease
```

Open the project in Android Studio, or build from the command line. Signing configuration is intentionally omitted — use your own keystore.

## Permissions

Nijatva Dialer requests Phone/SMS/Contacts permissions because it *is* your phone and SMS app. It is a default-handler app in the spirit of the AOSP dialer. There is no network access.

## Notes for contributors

- The optional "Support the developer" donation button is disabled in this public source. To enable it in your own build, set `SUPPORT_UPI_ID` in `SettingsActivity.java`.
- Android backup is disabled by design (`allowBackup="false"`).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
