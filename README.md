# Lobby for Android TV

A native Android TV launcher for [Lobby](https://github.com/CtznSniiips/Lobby): Continue Watching, Next Up, Live TV,
sports and Seerr discovery/requests on the home screen, backed by the Lobby plugin for Jellyfin or Emby.

This is the Android TV sibling of the Samsung/Tizen app. The plugins are shared and **need no changes**: this app is
just another caller of the same `/Lobby/*` routes with the same `Authorization: Lobby <token>` header, and the same
pairing flow (enter the server address, get a code, approve it in the plugin's admin page).

Android TV only. Not intended for phones or tablets.

## Lobby: Emby/Jellyfin and Seerr on Home

Lobby adds rows from your own Emby or Jellyfin server and Seerr to Home: Continue Watching, On Now, games for teams you follow, and Seerr's Trending and Popular lists. OK resumes or plays in the Emby/Jellyfin app, or requests a title in Seerr as the right person. The Watch layout puts a rotating featured title at the top (including your teams' games when they're about to start), with one row per band below and your apps in a dock along the bottom.

Lobby ships a plugin for each server, built from the same design and talking the same TV-facing API:

1. Install whichever plugin matches your server — [`emby-plugin/`](emby-plugin/README.md) for Emby, [`jellyfin-plugin/`](jellyfin-plugin/README.md) for Jellyfin — and set up Seerr and account links on its page.
2. On the TV: Settings → Emby & Seerr → Connect, enter the server address and approve the code on the plugin page.
3. Choose rows, order, teams and playback timing in Settings → Emby & Seerr. Those settings belong to the TV and are kept on the server.

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

## Build

Open in Android Studio, or `./gradlew assembleDebug` with the Android SDK installed. CI builds a debug APK on every push.
Side-load with `adb install app-debug.apk`. To use it as the home screen, choose Lobby when the TV asks for a home app
(it declares the `HOME` category).
