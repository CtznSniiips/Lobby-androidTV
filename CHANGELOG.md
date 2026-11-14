# Changelog

All notable changes to Lobby for Android TV are recorded here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[Unreleased]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.3.0...HEAD
[v0.3.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.2.0...v0.3.0
[v0.2.0]: https://github.com/CtznSniiips/Lobby-androidTV/compare/v0.1.0...v0.2.0
[v0.1.0]: https://github.com/CtznSniiips/Lobby-androidTV/releases/tag/v0.1.0
