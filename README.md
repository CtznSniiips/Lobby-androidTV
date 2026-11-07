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
| Art Mode + Screen saver         | The system screen saver: Screen saver button on Home, optional idle start   |
| HDMI input switching            | TV Input Framework: HDMI inputs row on Home                                  |

## Status

First cut, written without an Android SDK available, so treat the first CI build as the real compile check.

- [x] Pairing (probe `/System/Info/Public` bare then `/emby`, `POST /Lobby/Pair`, poll `/Lobby/Pair/Status`)
- [x] Home: featured, rows with progress, app dock, refresh on return from the playback app
- [x] Play / resume via `/Lobby/Play`, then launch the Jellyfin/Emby app
- [x] Requests via `/Lobby/Requesters` + `/Lobby/Request`, with a "Who's requesting?" picker when the TV may request as several Seerr accounts (last requester focused first)
- [x] Search (library + Seerr, debounced; Back returns to Home)
- [x] Details for Seerr titles (facts, tagline, overview, credits, cast) with Request, and Play/Resume for titles already in the library; partly available shows offer both
- [x] Per-TV settings (`/Lobby/Settings`): show/hide and reorder rows, titles per row, sports look-ahead, playback delay/timeout, sports team picker, reset to server defaults
- [x] Screen saver: a Home button and an optional idle timer (Off/2/5/10/15/30 min under Lobby settings; Off by default like the Tizen screen saver) start whatever screen saver is chosen in Android's settings, via SystemUI's `Somnambulator` (falls back to a message if the device lacks it). Lobby doesn't draw its own
- [x] Make Lobby the home screen: a prompt on Home until it is ("Not now" dismisses it), and a Home screen entry in Lobby settings. Tries Android's home-role prompt, then the TV's home-app setting, then shows the `adb` command
- [x] HDMI input switching
- [ ] Real banner/icon artwork

## Build

Open in Android Studio, or `./gradlew assembleDebug` with the Android SDK installed. CI builds a debug APK on every push.
Side-load with `adb install app-debug.apk`. To use it as the home screen, choose Lobby when the TV asks for a home app
(it declares the `HOME` category).
