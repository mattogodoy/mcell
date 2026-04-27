# mcell

> ¿Bloqueos de La Liga? Compruébalo en tu tele.

A small Android app — for both Android TV and phones — that tells you whether the ISP-level blocks ordered by La Liga are currently affecting the URLs you care about.

[**Landing page**](https://mattogodoy.github.io/mcell/) · [**Download APK**](https://github.com/mattogodoy/mcell/releases/latest/download/mcell.apk)

<p align="center">
  <img src="docs/assets/icon.png" alt="mcell icon" width="160" />
</p>

## Why

When there's a match on, the major Spanish ISPs (Movistar, MasOrange, Vodafone, DIGI) are required by court order to block entire IP ranges to fight piracy. The blocks are too broad and routinely take down legitimate CDNs that host shops, news sites, football clubs, sponsors, even the RAE. If your smart TV stops loading streams the moment kickoff happens, it's probably not your TV.

mcell shows, in two seconds, whether a URL you care about is reachable from your current network *right now*.

## Features

- Single APK runs on **Android TV** (leanback) and **Android phones** (touch). `minSdk` 26, `targetSdk` 34.
- Up to **6 user-configurable URLs**, three-state status (reachable / HTTP error / network error) with a localized reason on focus.
- A **global block banner** at the top, sourced from [hayahora.futbol](https://hayahora.futbol)'s public data and mirroring their homepage classification rule.
- **VPN detection** via `ConnectivityManager`.
- **No cache.** Every check is a fresh network request — that's the whole point.
- **No telemetry, no accounts, no analytics.** Network access is the only permission requested.
- Castilian Spanish UI only.

## Install

Grab the latest signed APK from [Releases](https://github.com/mattogodoy/mcell/releases/latest). Sideload it on your TV or phone (you'll need to allow installs from unknown sources).

## Build

Requirements: JDK 17, Android SDK with Compose support.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The unit-test suite covers the URL checker, the hayahora.futbol parser (with its staleness guard), the URL list repository, the banner derivation, and the `HomeViewModel` state machine — currently 35 tests.

## Stack

- Kotlin + Jetpack Compose, with `androidx.tv.material3` for TV-side focus styling.
- OkHttp for URL checks (HEAD with GET fallback, cache disabled at every layer).
- AndroidX DataStore Preferences for the user's URL list.
- `kotlinx.serialization` for the hayahora.futbol JSON.
- MVVM with a single `:app` module and manual DI via an `AppContainer`.

## How the global banner is computed

The banner mirrors the rule used by `hayahora.futbol`'s own homepage: blocks are considered active when **either** more than 10 distinct Cloudflare IPs are currently blocked by more than 2 ISPs each, **or** both `188.114.96.5` and `188.114.97.5` are blocked by any ISP. If hayahora's `lastUpdate` is older than 6 hours we fall back to "unknown" rather than reporting stale data.

## Attribution

Block data is generously published by **[hayahora.futbol](https://hayahora.futbol)**, an independent project that monitors La Liga blocks across all major Spanish ISPs in real time. mcell only consumes their public JSON — no scraping, no shadow API. If you find the app useful, consider helping document blocks from your own connection with [OONI Probe](https://ooni.org/install/).

## License

Not yet decided. Treat as "all rights reserved" until a `LICENSE` file is added.
