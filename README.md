# TSport

> An Android TV and phone app that finds X/Twitter hashtag broadcast links and opens them on the big screen.

[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Android TV](https://img.shields.io/badge/Android%20TV-leanback-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/training/tv)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

<p align="center">
  <a href="https://www.buymeacoffee.com/cmtrkr"><img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Buy Me a Coffee"></a>
</p>

TSport is a small Kotlin app for following live broadcasts shared under a hashtag on X. Sign in to X inside the app, type a hashtag (for example `GsvBJK`), and TSport searches X for posts containing `"broadcasts"` and links. It supports both **Popüler** and **En son** result tabs, extracts and de-duplicates `x.com/i/broadcasts/...` URLs, resolves `t.co` redirects, and presents a D-pad-friendly list. Selecting an item opens the broadcast in a browser or player.

It works on Android TV boxes and phones, has no backend, and stores only the X session cookies needed for the search.

## 📸 Screenshot

<p align="center">
  <img src="docs/screenshots/search.png" width="900" alt="TSport Android TV search screen with hashtag field and broadcast link list">
  <br>
  <sub>Representative preview built from the app's real TV layout, colors and labels — not a TV capture.</sub>
</p>

## ✨ Features

- 🔐 **In-app X login** — a WebView login flow with session detection.
- 🔎 **Hashtag search** — searches X for the hashtag plus `broadcasts`; optional `filter:links` support.
- 🗂️ **Popüler / En son tabs** — `En son` loads the `f=live` URL directly; if a tab is empty, TSport automatically tries the other one.
- 🔗 **Broadcast extraction** — keeps only `x.com/i/broadcasts/...` links.
- 🧹 **Deduplication and `t.co` resolution** — concurrent redirect resolution, one row per broadcast.
- ⚡ **Fast DOM scan** — reads tweets already rendered by X and exits early after 3 direct broadcasts or 12 candidate links.
- 🛟 **GraphQL fallback** — if both tabs return no candidates, uses a Scweet-style `SearchTimeline` request with a freshly generated `x-client-transaction-id`.
- 📺 **TV-first UI** — dark theme, focus rings, D-pad navigation and a leanback launcher entry.
- 📱 **Phone support** — the same APK runs on normal Android phones.
- 🚪 **Logout** — clears the local X session.
- 🛡️ **No backend** — searches happen in the app's WebView; there is no server in the middle.

## 🏗️ How it works

1. `LoginActivity` opens X in a WebView and detects a signed-in session.
2. `SearchQueryBuilder` normalizes the hashtag, appends `broadcasts` (and optionally `filter:links`), and builds the search URL. For `En son`, the direct `f=live` URL is preferred.
3. A hidden WebView loads the results page; `scrape_dom.js` reads the tweets currently rendered by X and collects candidate links. It scrolls a few times, but exits early after 3 direct broadcast links or 12 candidates so the UI stays responsive.
4. If the selected tab returns nothing, the other tab is tried. If both are empty, `ApiFallbackScraper` makes a `SearchTimeline` request (20 tweets per page, up to 6 pages) with a fresh transaction ID.
5. `LinkExtractor` normalizes URLs, resolves `t.co` redirects in parallel, keeps valid broadcast IDs and removes duplicates.
6. `MainActivity` renders the list; selecting a row opens the broadcast externally.

```text
Hashtag → X search (WebView DOM) → candidate URLs → t.co resolution
        → x.com/i/broadcasts/<id> → deduplicated list → external player

                 └─ empty tab → other tab → Scweet SearchTimeline fallback
```

The DOM path usually reads only the tweets X has rendered for the current result page (often 3–20 tweets); a tweet can contain several candidate links. The GraphQL fallback requests 20 tweets per page. Finding 3–4 final links therefore does **not** mean only 3–4 tweets were queried: the final count is the number of matching broadcast URLs after filtering and deduplication. The early-exit threshold can be adjusted in `app/src/main/assets/scrape_dom.js` if you prefer more results over speed.

## 🚀 Build

Requirements: JDK 17, Android SDK 35, Gradle (wrapper included).

```bash
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the project in Android Studio for a graphical build. The package id is `com.tsport.links`; minSdk 24, targetSdk 35.

## 📁 Project structure

```text
app/src/main/java/com/tsport/links/
├── auth/        # X WebView login and session detection
├── search/      # Query building, DOM scraper, Scweet fallback and view model
├── links/       # URL extraction, t.co resolution, link model
└── ui/          # MainActivity and RecyclerView adapter

app/src/main/assets/
├── scrape_dom.js          # DOM candidate collection and early exit
├── fallback_api.js        # Scweet-style SearchTimeline fallback
└── transaction_bundle.js  # vendored x-client-transaction-id generator
```

## ⚠️ Notes

- A signed-in X account is required; X may rate-limit or change its search markup, GraphQL query IDs or features at any time.
- The app is DOM-first. The GraphQL fallback depends on Scweet's manifest defaults and a generated `x-client-transaction-id`; if X changes either, the fallback may need an update.
- The `En son` tab uses `f=live`. If X drops that parameter, the app falls back to clicking the tab.
- Use it for lawful, personal access to public broadcasts. It is not affiliated with X Corp.
- Test on your device before relying on it during a live event.

## 🙏 Third-party

- DOM search and UI code are part of TSport (MIT).
- Scweet's default `SearchTimeline` query ID/features are adapted from [Altimis/Scweet](https://github.com/Altimis/Scweet) (MIT).
- `transaction_bundle.js` vendors [Lqm1/x-client-transaction-id](https://github.com/Lqm1/x-client-transaction-id) (MIT), minified into a WebView-friendly bundle.

## ❤️ Support

If TSport helps you find the match, you can buy me a coffee:

<p align="center">
  <a href="https://www.buymeacoffee.com/cmtrkr"><img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Buy Me a Coffee"></a>
</p>

## 📄 License

[MIT](LICENSE) © 2026 Cem Türker.
