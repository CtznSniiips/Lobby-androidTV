# Lobby for Android TV

A native Android TV launcher for [Lobby](https://github.com/CtznSniiips/Lobby): Continue Watching, Next Up, Live TV,
sports and Seerr discovery/requests on the home screen, backed by the Lobby plugin for Jellyfin or Emby.

This is the Android TV sibling of the Samsung/Tizen app. The plugins are shared and **need no changes**: this app is
just another caller of the same `/Lobby/*` routes with the same `Authorization: Lobby <token>` header, and the same
pairing flow (enter the server address, get a code, approve it in the plugin's admin page).

Android TV only. Not intended for phones or tablets.

## How playback works

Lobby never plays media itself. Pressing a title calls `POST /Lobby/Play`; the plugin finds the official Jellyfin or
Emby Android TV app's session by `DeviceId` and sends it a play command, then Lobby opens that app. Install the
official app on the same device alongside Lobby.

## What replaces the Tizen-only features

| Tizen (`device.js`)             | Android TV                                                                  |
| ------------------------------- | --------------------------------------------------------------------------- |
| Installed-app dock              | `PackageManager` query for `LEANBACK_LAUNCHER` activities (`TvDevice.dockApps`) |
| Samsung Home/Settings shortcuts | `Settings.ACTION_SETTINGS`                                                  |
| DUID for pairing                | Android ID sent as `Duid`; admin can assign the app's real `DeviceId`       |
| Art Mode                        | Ambient/backdrop/screensaver mode (not yet built)                           |
| HDMI input switching            | To investigate (see LtvLauncher) (not yet built)                            |

## Status

First cut, written without an Android SDK available, so treat the first CI build as the real compile check.

- [x] Pairing (probe `/System/Info/Public` bare then `/emby`, `POST /Lobby/Pair`, poll `/Lobby/Pair/Status`)
- [x] Home: featured, rows with progress, app dock, refresh on return from the playback app
- [x] Play / resume via `/Lobby/Play`, then launch the Jellyfin/Emby app
- [x] Requests via `/Lobby/Requesters` + `/Lobby/Request` (uses last/first requester; picker still to do)
- [x] Search (library + Seerr, debounced; Back returns to Home)
- [ ] Details, per-TV Settings, Sports team picker
- [ ] Ambient mode, HDMI input switching, make-default-home onboarding
- [ ] Real banner/icon artwork

## Build

Open in Android Studio, or `./gradlew assembleDebug` with the Android SDK installed. CI builds a debug APK on every push.
Side-load with `adb install app-debug.apk`. To use it as the home screen, choose Lobby when the TV asks for a home app
(it declares the `HOME` category).
