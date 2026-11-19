# Changelog

All notable changes to Lobby for Android TV are recorded here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v0.14.3] - 11/19/2026

### Fixed
- "Connect" no longer says it couldn't reconnect while actually leaving the connection live (after which "Re-pair" complained Lobby was already connected). The reconnect check used the same stream-close-prone reader; it now reads tolerantly and treats an already-established connection as success.

## [v0.14.2] - 11/19/2026

### Fixed
- The ADB self-install no longer fails with "Stream closed." The short install commands close their stream the instant they finish, which the ADB client raised as an error rather than a clean end-of-output; Lobby now reads command output and streams the APK tolerantly, and reports which step failed if one does. (This fix was written earlier but didn't make it into 0.14.0/0.14.1.)

## [v0.14.1] - 11/19/2026

### Changed
- The on-device ADB self-update is no longer labelled experimental. Its Settings section is now "Self-update over ADB".

## [v0.14.0] - 11/19/2026

### Changed
- Installing an update now re-checks GitHub for the current latest release at that moment and installs that, instead of whatever version an earlier update-check had cached. If you're already on the newest version it says so rather than reinstalling.

## [v0.13.0] - 11/19/2026

### Added
- Lobby can now update itself over the air, on the device, with no PC. It pairs once with this TV's Wireless Debugging, then installs its own updates straight through ADB — no installer prompt and no unknown-sources wall. Pairing is under Settings → System → Experimental: tap **Pair automatically**, open the TV's "Pair device with pairing code" dialog, and Lobby reads the code off the screen and pairs itself (needs Lobby's accessibility service on). Pairing persists, so after that a single **Connect** reconnects without pairing again. Because the ADB install is a trusted-source install, it also stops updates from re-locking the Home override.

### Changed
- Applying an update now always goes through ADB (the Updates section's button is **Install over ADB**). The update check, notice, and release notes are unchanged.

### Removed
- The old installer-prompt update path (hand the APK to the system package installer) is gone in favour of the ADB self-install.

## [v0.12.0] - 11/18/2026

### Fixed
- The Home-override self-heal now also does the *first* turn-on. 0.11.0 only re-enabled an override that had already been on once — but the restricted toggle blocks that first turn-on, so it could never arm. Now, whenever Lobby holds `WRITE_SECURE_SETTINGS` and the override is off, it switches it on itself (on launch/resume). Granting the permission is all that's needed; to turn the override off, revoke the permission.
- When the device reverts the write anyway — the strictest Google TVs undo it for a restricted sideloaded app no matter what — System settings now says so plainly and points to the ADB-install route in the README, instead of claiming the override is handled.

## [v0.11.0] - 11/16/2026

### Added
- Lobby can now keep its Home-button override on across in-app updates. Updating used to switch the override off, and Android's restricted-settings block could stop you re-enabling it. If you grant Lobby secure-settings access once over ADB (`adb shell pm grant io.github.ctznsniiips.lobby android.permission.WRITE_SECURE_SETTINGS`), Lobby re-enables the override by itself whenever it finds it switched off. System settings shows the exact command until the grant is in place.

## [v0.10.0] - 11/16/2026

### Fixed
- The screen saver shortcut now opens the actual Screen saver screen (Settings → Device Preferences → Screen saver) instead of the top level of Settings. Android TV's Settings doesn't register the standard screen-saver intent, so Lobby now launches that screen's own activity directly.

## [v0.9.0] - 11/16/2026

### Changed
- When the device blocks Lobby from starting the screen saver (common on Google TV), pressing the Screensaver button now opens the system Screen saver settings — where you can start it or set it up — instead of just showing a message.

## [v0.8.0] - 11/16/2026

### Added
- Rearrange apps from the home screen: long-press an app to pick it up, then move it with the D-pad (left/right in the dock, up/down in the List layout) and press OK or Back to drop it. A hint appears while a tile is lifted, and the new order is saved (and shared with the dock, List and Settings).

## [v0.7.0] - 11/15/2026

### Fixed
- The top row (e.g. "Continue Watching") is no longer shadowed at rest. The cause was the rows' top-edge fade — meant to dissolve rows under the hero as they scroll up — which was applied even while nothing was scrolled, partially erasing the first row's title and cards. It now kicks in only once the list is actually scrolled up. This also reverts the 0.6.0 hero colour-wash tweak, which was an incorrect attempt at the same issue.

## [v0.6.0] - 11/15/2026

### Added
- Home-screen update notice: when an update is available, a banner appears at the top of Home with an "Update" shortcut that jumps straight to the Updates settings to apply it, and a "Hide" button that dismisses it until a newer release. A new System setting, "Show a notice on the home screen", turns the banner off entirely.
- The rail's "Go to apps" button now opens a full-screen grid of every launchable app on the system — TV apps plus regular-launcher apps that Google TV often installs for sideloaded phone apps — instead of just jumping to the bottom dock.

### Fixed
- Pressing left on a row's first card no longer lands on an invisible target (which could launch an app in the dock); it now moves to the rail, or to the app list in the List layout.
- The top row's title (e.g. "Continue Watching") is no longer dimmed: the hero's upper-left colour wash was bleeding down onto it; it's now kept tighter to the hero area so it fades out above the first row.

## [v0.5.0] - 11/14/2026

### Fixed
- The hero no longer gets stuck on a row title: pressing up from a home row returns to the featured carousel and left/right cycles it again, instead of staying pinned to whichever card was focused.
- Continue Watching and Next Up titles are kept out of the hero carousel; they only appear in their own rows.

## [v0.4.0] - 11/14/2026

### Added
- Go quiet on idle: an optional System setting that pauses the home screen's hero rotation and clock after a configurable idle period, so the box can settle and sleep the way the stock launcher does when the TV is switched off.
- Check for updates: an optional System setting that periodically checks the GitHub repository for a newer release.
- In-app updater: the Updates section shows the installed version, the latest release's changelog, and a "Download & install" button that pulls the release APK and hands it to the system installer (you still confirm the install). Falls back to opening the release page when a release has no APK asset.

## [v0.3.0] - 11/14/2026

### Fixed
- Playing a title no longer prompts to install Emby when it's already installed. Lobby now also recognizes the general Emby for Android (`com.mb.android`) and Jellyfin (`org.jellyfin.mobile`) apps that Google TV often installs in place of the Android TV builds, and hands playback to them.

## [v0.2.0] - 11/14/2026

### Fixed
- Home-button override no longer bounces Google TV's system Settings (or the notification shade and other launcher panels) back to Lobby. It now recaptures only the launcher's actual Home screen, matched by its exact activity, so the Home button still returns to Lobby while everything else opens normally.

## [v0.1.0] - 11/12/2026

### Added
- Native Android TV launcher that pairs with the Lobby plugin and hands playback to the installed Jellyfin/Emby app (no in-app player).
- Home screen modelled on the Samsung app: full-bleed backdrop that follows the focused or featured title, a dominant-colour wash, greeting, clock and weather, a featured hero (tagline heading, overview, Play/Resume/Request button, show logo), and media rows.
- Featured hero cycling with left/right; left from the first title moves to the rail.
- Search, title details, and a "who's requesting" picker for Seerr requests.
- Per-TV settings mirroring the plugin, grouped like the Samsung app: Screen saver, Apps on Home, Inputs on Home, Emby & Seerr (home rows, titles per row, teams, sports look-ahead, playback delay/timeout, server defaults, connection) and System.
- App dock of installed apps with show/hide and reordering; HDMI input switching via the TV Input Framework with show/hide.
- System screen saver support with an idle timer, and make-Lobby-the-home-screen onboarding.
- Weather icon and temperature from Open-Meteo, using the location the plugin supplies.
- Lobby app icon and Android TV banner.

### Build
- GitHub Actions build check on every pull request.
- Fixed signing key via a GitHub Actions secret, so new builds install over older ones without uninstalling.

[Unreleased]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.12.0...HEAD
[v0.14.3]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.14.2...v0.14.3
[v0.14.2]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.14.1...v0.14.2
[v0.14.1]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.14.0...v0.14.1
[v0.14.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.13.0...v0.14.0
[v0.13.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.12.0...v0.13.0
[v0.12.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.11.0...v0.12.0
[v0.11.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.10.0...v0.11.0
[v0.10.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.9.0...v0.10.0
[v0.9.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.8.0...v0.9.0
[v0.8.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.7.0...v0.8.0
[v0.7.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.6.0...v0.7.0
[v0.6.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.5.0...v0.6.0
[v0.5.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.4.0...v0.5.0
[v0.4.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.3.0...v0.4.0
[v0.3.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.2.0...v0.3.0
[v0.2.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.1.0...v0.2.0
[v0.1.0]: https://github.com/CtznSniiips/Lobby-androidTV/releases/tag/v0.1.0
