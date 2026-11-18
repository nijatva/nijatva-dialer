# Changelog

## 1.6 — 18 November 2026

- **Fixed:** the APK shipped without a `<uses-sdk>` block, so minSdk and targetSdk were missing from the manifest. Now declares minSdk 21 / targetSdk 34, which also lets aapt2 drop redundant per-version resources (smaller APK).
- Icon: uses the app's own drawable / adaptive foreground, matching the website

## 1.5 — 18 November 2026

- New app icon: bold outlined glyph, centred in the slate rounded square
- Landing page added under `docs/` (GitHub Pages)

## 1.4 — 17 November 2026

- Added the Nijatva family media helper (`NijatvaMediaIntents`)
- New "Nijatva apps" section on the Developer screen (Gallery, Photos, Camera, Files)

## 1.3 — 17 November 2026

- App name is now **Nijatva Dialer**
- New app icon: slate rounded square with a white outlined handset

## 1.2 — 16 November 2026

- New app icon: slate rounded square with a white outlined handset
- Icon applied across the launcher, About dialog, app lock and shortcuts

## 1.1 — 13 November 2026

- New **Appearance** screen: Theme (System, Dark, Light, Black) and Accent colour (Green, Saffron, Indigo)
- New **Developer** page with project links
- Both accessible from the top of Settings

## 1.0 — 13 November 2026

First public release.

- Dialer with speed dial, spam blocking, caller ID popup, fake call and app lock
- SMS with smart categories, OTP auto-delete and spam protection
- Contacts with avatars, search, account filters and favourites
- Pure dark AMOLED interface
- No internet permission, no ads, no trackers
- Android backup disabled by design
