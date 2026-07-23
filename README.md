# ScaleConnect

A custom Android app for Bluetooth body-composition scales that use the
Senssun BLE protocol — built from scratch after the official "Senssun Body
Monitor" Play Store app turned out to be unreliable.

## Inspiration

The scale itself is decent hardware, but its official companion app
(**Senssun Body Monitor**) would frequently hang forever on a
"Synchronizing…" screen after a weigh-in, with no way to recover except
force-closing the app — making the scale mostly unusable.

Rather than live with that, this project reverse-engineered the app instead
of fighting it:

- Decompiled the official APK to read its source.
- Captured the raw Bluetooth traffic between the phone and the scale.
- Found the actual bug: the official app's Bluetooth handler silently
  **drops any notification shorter than 8 bytes** — but the scale's real
  status replies are only 3 bytes. Every sync was being thrown away before
  the app ever saw it. It's a bug in the app itself, not something a user
  can fix from the outside.
- Fully decoded the scale's proprietary BLE protocol: weight, body fat %,
  hydration %, muscle %, bone %, and BMR, plus how the app pushes a user's
  age/height/sex profile to the scale so it can compute those metrics
  on-device.

ScaleConnect is the result: a from-scratch replacement app implementing that
protocol correctly, with a UI and features the original never had.

## Features

- Live weight + full body-composition readout (fat, hydration, muscle, bone,
  BMR) the moment you step on the scale
- **Background auto-connect** — a foreground service watches for the scale
  and connects automatically even when the app isn't open
- **Multiple profiles**, each with their own age/height/sex/goal weight, so
  the scale computes accurate metrics per person
- **History tracking** with a trend chart per metric:
  - Tap a point to filter the list down to that single weigh-in
  - Drag two fingers across the chart to compare any two dates, with the
    list filtered to everything in between and a delta shown
- Weight units (lb/kg) and height units (cm/ft-in) match whatever the
  scale's own physical toggle is set to — the protocol turns out to be
  unit-invariant, so this is purely a display preference
- Light / Dark / System theme

## Requirements

- Android 8.0 (API 26) or newer
- A Senssun-protocol BLE body-composition scale

## Installing on your phone

### Option A — install the pre-built APK (no computer needed after download)

1. On your phone, open the [**Releases**](../../releases) page for this repo
   and download the latest `ScaleConnect-*.apk`.
2. Open the downloaded file from your notifications or Downloads app to
   install it. Android will prompt you to allow installs from that source
   (e.g. Chrome or Files) the first time — allow it just for this install.
3. Launch ScaleConnect and grant the Bluetooth and Notification permissions
   it asks for (needed to find the scale and to show the background
   connection status).

> The release APK is a debug build signed with a local debug key, not a
> Play Store release — Android will flag it as being from an "unknown
> source," which is expected for a sideloaded personal project like this.

### Option B — build it yourself from source

1. Clone this repo and open it in Android Studio, **or** use the command
   line with the bundled Gradle wrapper.
2. Connect your phone via USB with USB debugging enabled (or use an
   emulator).
3. From the project root:
   ```
   ./gradlew installDebug
   ```
   This builds the app and installs it directly on the connected
   device/emulator. Use `./gradlew assembleDebug` instead if you just want
   the `.apk` file (found under `app/build/outputs/apk/debug/`).

## Disclaimer

This project is an independent, reverse-engineered replacement client and is
not affiliated with, endorsed by, or supported by Senssun. It was built for
personal use to fix a broken official app.
