# Lobby for Android TV

A native Android TV launcher for [Lobby](https://github.com/CtznSniiips/Lobby): Continue Watching, Next Up, Live TV,
sports and Seerr discovery/requests on the home screen, backed by the Lobby plugin for Jellyfin or Emby.

This is the Android TV sibling of the Samsung/Tizen app. The plugins are shared and **need no changes**: this app is
just another caller of the same `/Lobby/*` routes with the same `Authorization: Lobby <token>` header, and the same
pairing flow (enter the server address, get a code, approve it in the plugin's admin page).

Android TV only. Not intended for phones or tablets.

<picture>
  <img src="docs/Lobby-android.png" alt="Lobby for Android">
</picture>

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

## Setting Lobby as your home screen

To make Lobby the launcher that opens when you press the Home button:

1. Make Lobby your home app — from Lobby's **System** settings, or in Android's **Settings → Apps → Special app access → Home app** (wording varies by device).
2. Turn on Lobby's **Home-button override** when prompted. It's an accessibility service that returns you to Lobby when the Home button would otherwise open the stock launcher; on Google TV the Home-app role alone isn't enough, so this is what makes the physical Home button come back to Lobby.

If the accessibility setting won't turn on — the toggle is greyed out, flips back off, or reports it's restricted — that's Android's "restricted settings" protection for sideloaded apps. Install Lobby with ADB (`adb install Lobby-<version>.apk`) and the accessibility toggle will enable normally, since apps installed over ADB aren't subject to that restriction. (On some devices you can instead allow it from **Settings → Apps → Lobby → ⋮ → Allow restricted settings**.)

### Keeping the override across in-app updates

Updating Lobby from inside the app can switch the Home-button override back off, and the restricted-settings block can stop you re-enabling it. To let Lobby turn its own override back on after each update, grant it secure-settings access once over ADB.

The `pm grant` command below runs over an existing ADB connection — it has no IP of its own; the TV's address goes in the `adb connect`/`adb pair` step first. A TV box usually has no USB port, so connect over the network:

1. On the TV, turn on **Developer options** (Settings → System → About, then click the build number several times), then enable **Network debugging** (older Android TV) or **Wireless debugging** (Android 11+) under Developer options.
2. From a computer with [platform-tools](https://developer.android.com/tools/releases/platform-tools), connect to the TV's IP (shown under Settings → Network, and on the Wireless debugging screen):
   - Older Android TV: `adb connect <tv-ip>:5555`
   - Android 11+ Wireless debugging (it shows a 6-digit pairing code and uses randomized ports, not 5555): `adb pair <tv-ip>:<pair-port>`, enter the code, then `adb connect <tv-ip>:<connect-port>` — both ports are shown on the Wireless debugging screen.
3. Accept the "Allow debugging?" prompt on the TV the first time. Once `adb devices` lists the TV, run the grant:

```
adb shell pm grant io.github.ctznsniiips.lobby android.permission.WRITE_SECURE_SETTINGS
```

If ADB reports `more than one device/emulator`, another device is also connected — target the TV by adding `-s <tv-ip>:5555` before `shell`, e.g. `adb -s 192.168.86.x:5555 shell pm grant …` (`adb devices` lists what's attached).

This permission can't be granted from the on-screen settings — only over ADB — but the grant survives app updates (it's cleared only by a full uninstall). With it, Lobby re-enables the Home-button override by itself whenever it finds it switched off, so updates no longer break it. Lobby shows this command in **System settings → Home button override** until the grant is in place.

## Build

Open in Android Studio, or `./gradlew assembleDebug` with the Android SDK installed. CI builds a debug APK on every push.
Side-load with `adb install app-debug.apk`. To use it as the home screen, choose Lobby when the TV asks for a home app
(it declares the `HOME` category).
