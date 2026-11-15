# Changelog

All notable changes to Lobby for Android TV are recorded here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[Unreleased]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.7.0...HEAD
[v0.7.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.6.0...v0.7.0
[v0.6.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.5.0...v0.6.0
[v0.5.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.4.0...v0.5.0
[v0.4.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.3.0...v0.4.0
[v0.3.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.2.0...v0.3.0
[v0.2.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.1.0...v0.2.0
[v0.1.0]: https://github.com/CtznSniiips/Lobby-androidTV/releases/tag/v0.1.0
