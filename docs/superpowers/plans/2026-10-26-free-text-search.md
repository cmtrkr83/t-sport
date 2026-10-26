# Serbest Metin Araması ve 50 Link Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** X aramasını serbest metinle (`SAMvAMD broadcasts`) yapıp yalnızca broadcast linklerinden en fazla 50 tane toplamak.

**Architecture:** Sorgu üretimi hashtag zorunluluğundan çıkar; mevcut WebView DOM + GraphQL kazıma hattı korunur. Kazıma derinliği ve sonuç tavanı artırılır, `t.co` çözümlemesine bütçe eklenerek süre aşımında kısmi sonuç döndürülür. Sonuç filtresi (`x.com/i/broadcasts/<id>`) değişmez.

**Tech Stack:** Kotlin, Android WebView + JavaScript, coroutines, JUnit4, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-26-free-text-search-design.md`

## Global Constraints

- Sonuçlar yalnızca `https://x.com/i/broadcasts/<id>`; tekilleştirilmiş, en fazla **50** link.
- URL'de `f=top` / `f=live` olmaz; sekme X'in kendi sekmesine tıklanarak seçilir.
- Arayüz Türkçe; tüm kontroller D-pad ile odaklanabilir; seçimler `SharedPreferences`'ta kalır.
- Yeni bağımlılık eklenmez.
- `main` üzerinde doğrudan çalışılır; worktree açılmaz.
- **Kullanıcı açıkça istemedikçe commit yapılmaz.** Plan adımları değişiklikleri çalışma ağacında bırakır.
- Varsayılanlar: `broadcasts` kapalı, `filter:links` kapalı, sekme `En son`.

## Review Focus

- Girdide `broadcasts` zaten varsa ikinci kez eklenmemeli (büyük/küçük harf duyarsız) — Task 1 testi.
- Girdi yalnızca `#` veya boşluksa arama başlamamalı — Task 1 testi.
- Türkçe karakterli ve çok boşluklu girdi doğru kodlanmalı — Task 1 testi.
- `t.co` çözümleme bütçesi dolduğunda çözülen linkler korunmalı, arama komple düşmemeli — Task 2 testi.
- 50'den fazla aday geldiğinde tam 50 link dönmeli — Task 2 testi.

---

### Task 1: Serbest Metin Sorgusu ve Arayüz Metinleri

**Files:**
- Modify: `app/src/main/java/com/tsport/links/search/SearchQueryBuilder.kt`
- Modify: `app/src/main/java/com/tsport/links/search/SearchViewModel.kt:32`
- Modify: `app/src/main/java/com/tsport/links/ui/MainActivity.kt:87-96,147-159,269`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml:63`
- Test: `app/src/test/java/com/tsport/links/search/SearchQueryBuilderTest.kt`
- Test: `app/src/test/java/com/tsport/links/search/SearchViewModelTest.kt:60-72`

**Interfaces:**
- Consumes: mevcut `SearchTab`, `SearchOptions`, `SearchQueryBuilder.buildSearchUrl`.
- Produces:
  - `data class SearchOptions(val query: String, val includeBroadcasts: Boolean = false, val linksOnly: Boolean = false, val tab: SearchTab = SearchTab.LATEST)`
  - `SearchQueryBuilder.buildSearchUrl(options: SearchOptions): String?`
  - Sorgu kuralları: boşluklar tek boşluğa indirilir, `#` korunur, `broadcasts` tırnaksız eklenir, `filter:links` aynen eklenir, ikisi de yinelenmez.

- [ ] **Step 1: Write the failing tests**

`SearchQueryBuilderTest.kt` dosyasını aşağıdaki testlerle değiştir:

```kotlin
package com.tsport.links.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryBuilderTest {

    private fun url(
        query: String,
        broadcasts: Boolean = false,
        linksOnly: Boolean = false
    ): String? = SearchQueryBuilder.buildSearchUrl(
        SearchOptions(query, includeBroadcasts = broadcasts, linksOnly = linksOnly)
    )

    @Test
    fun `keeps free text without adding hash`() {
        assertEquals("https://x.com/search?q=SAMvAMD&src=typed_query", url("SAMvAMD"))
    }

    @Test
    fun `adds unquoted broadcasts when selected`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20broadcasts&src=typed_query",
            url("SAMvAMD", broadcasts = true)
        )
    }

    @Test
    fun `keeps spaces in multi word queries`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20canl%C4%B1%20yay%C4%B1n%20broadcasts&src=typed_query",
            url("  SAMvAMD   canlı  yayın  ", broadcasts = true)
        )
    }

    @Test
    fun `adds filter links when selected`() {
        assertEquals(
            "https://x.com/search?q=%23GSvFB%20filter%3Alinks&src=typed_query",
            url("#GSvFB", linksOnly = true)
        )
    }

    @Test
    fun `adds both options`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20broadcasts%20filter%3Alinks&src=typed_query",
            url("SAMvAMD", broadcasts = true, linksOnly = true)
        )
    }

    @Test
    fun `does not duplicate broadcasts already in query`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20broadcasts&src=typed_query",
            url("SAMvAMD broadcasts", broadcasts = true)
        )
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20BROADCASTS&src=typed_query",
            url("SAMvAMD BROADCASTS", broadcasts = true)
        )
    }

    @Test
    fun `does not duplicate filter links already in query`() {
        assertEquals(
            "https://x.com/search?q=SAMvAMD%20filter%3Alinks&src=typed_query",
            url("SAMvAMD filter:links", linksOnly = true)
        )
    }

    @Test
    fun `does not put tab in url`() {
        val popular = SearchQueryBuilder.buildSearchUrl(
            SearchOptions("SAMvAMD", tab = SearchTab.POPULAR)
        )
        val latest = SearchQueryBuilder.buildSearchUrl(
            SearchOptions("SAMvAMD", tab = SearchTab.LATEST)
        )
        assertEquals(popular, latest)
        assertFalse(popular!!.contains("f="))
    }

    @Test
    fun `returns null for blank or hash only query`() {
        assertNull(url(""))
        assertNull(url("   "))
        assertNull(url("#"))
        assertNull(url(" # "))
        assertNull(url("##"))
        assertNull(url("# #"))
    }

    @Test
    fun `encodes turkish and special characters`() {
        assertEquals(
            "https://x.com/search?q=Fenerbah%C3%A7e&src=typed_query",
            url("Fenerbahçe")
        )
        assertEquals(
            "https://x.com/search?q=A%26B%3DC%25&src=typed_query",
            url("A&B=C%")
        )
    }
}
```

`SearchViewModelTest.kt` içindeki `blank hashtag fails without calling source` testini yeniden adlandır ve mesajı güncelle:

```kotlin
@Test
fun `blank query fails without calling source`() = runTest {
    var called = false
    val viewModel = SearchViewModel(LinkExtractor(resolver))
    viewModel.search(SearchOptions("   ")) {
        called = true
        emptyList()
    }
    advanceUntilIdle()
    val state = viewModel.state.value
    assertTrue(state is SearchState.Failure)
    assertEquals("Geçerli bir arama metni girin.", (state as SearchState.Failure).message)
    assertTrue(!called)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.tsport.links.search.SearchQueryBuilderTest" --tests "com.tsport.links.search.SearchViewModelTest"`

Expected: FAIL — `SearchOptions` içinde `query` yok / beklenen URL'ler `#` ile başlıyor / mesaj eski.

- [ ] **Step 3: Implement the query builder**

`SearchQueryBuilder.kt`:

```kotlin
package com.tsport.links.search

import java.net.URLEncoder

enum class SearchTab { POPULAR, LATEST }

data class SearchOptions(
    val query: String,
    val includeBroadcasts: Boolean = false,
    val linksOnly: Boolean = false,
    val tab: SearchTab = SearchTab.LATEST
)

object SearchQueryBuilder {

    fun buildSearchUrl(options: SearchOptions): String? {
        val base = options.query.trim().replace(Regex("\\s+"), " ")
        if (base.isEmpty() || base.all { it == '#' || it.isWhitespace() }) return null

        val parts = mutableListOf(base)
        if (options.includeBroadcasts && !containsToken(base, "broadcasts")) {
            parts += "broadcasts"
        }
        if (options.linksOnly && !containsToken(base, "filter:links")) {
            parts += "filter:links"
        }

        val encoded = URLEncoder.encode(parts.joinToString(" "), "UTF-8")
            .replace("+", "%20")
        return "https://x.com/search?q=$encoded&src=typed_query"
    }

    private fun containsToken(query: String, token: String): Boolean =
        Regex(
            "(?i)(?<![\\p{L}\\p{N}_])${Regex.escape(token)}(?![\\p{L}\\p{N}_])"
        ).containsMatchIn(query)
}
```

- [ ] **Step 4: Update call sites and strings**

`SearchViewModel.kt:32` mesajı:

```kotlin
_state.value = SearchState.Failure("Geçerli bir arama metni girin.")
```

`MainActivity.kt` içinde alan adını güncelle:

- `hashtag = binding.hashtagInput.text.toString(),` → `query = binding.hashtagInput.text.toString(),`
- `options.hashtag` → `options.query` (iki yerde: `putString(KEY_LAST_HASHTAG, ...)` ve `outState.putString(KEY_STATE_HASHTAG, ...)`).

`strings.xml`:

```xml
<string name="search_hint">Etkinlik adı veya arama metni (örn. SAMvAMD)</string>
<string name="option_broadcasts">broadcasts kelimesini ekle</string>
<string name="status_idle">Bir arama metni girip Ara\'ya basın.</string>
<string name="status_empty">Sonuç yok. Aramayı kontrol edip tekrar deneyin.</string>
```

`hashtag_hint` satırını kaldır. `activity_main.xml:63`:

```xml
android:hint="@string/search_hint"
```

Arama alanı görsel kimliği (`hashtagInput`) ve `SharedPreferences` anahtarları değişmez.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.tsport.links.search.SearchQueryBuilderTest" --tests "com.tsport.links.search.SearchViewModelTest"`

Expected: PASS.

---

### Task 2: 50 Link Tavanı ve `t.co` Çözümleme Bütçesi

**Files:**
- Modify: `app/src/main/java/com/tsport/links/links/LinkExtractor.kt`
- Test: `app/src/test/java/com/tsport/links/links/LinkExtractorTest.kt`

**Interfaces:**
- Consumes: `UrlResolver.resolve(url: String): String?`, `LinkCandidate`.
- Produces:
  - `LinkExtractor(resolver: UrlResolver, maxLinks: Int = 50, resolveBudgetMs: Long = 15_000)`
  - `extract(candidates: List<LinkCandidate>): List<LinkItem>`
  - Bütçe dolduğunda o ana kadar çözülen `t.co` linkleri kullanılır; çözülemeyenler atılır.

- [ ] **Step 1: Write the failing tests**

`LinkExtractorTest.kt` sonuna ekle:

```kotlin
@Test
fun `default cap keeps fifty broadcast links`() = runBlocking {
    val urls = (1..55).map { "https://x.com/i/broadcasts/$it" }
    val result = LinkExtractor(FakeResolver()).extract(candidates(*urls.toTypedArray()))
    assertEquals(50, result.size)
    assertEquals("https://x.com/i/broadcasts/1", result.first().url)
}

@Test
fun `keeps resolved links when resolve budget expires`() = runBlocking {
    val resolver = object : UrlResolver {
        override suspend fun resolve(url: String): String? {
            if (url.endsWith("/slow")) delay(5_000)
            return when (url) {
                "https://t.co/fast" -> "https://x.com/i/broadcasts/fast"
                "https://t.co/slow" -> "https://x.com/i/broadcasts/slow"
                else -> null
            }
        }
    }
    val result = LinkExtractor(resolver, resolveBudgetMs = 100).extract(
        candidates("https://t.co/fast", "https://t.co/slow")
    )
    assertEquals(listOf("https://x.com/i/broadcasts/fast"), result.map { it.url })
}
```

Gerekli import: `import kotlinx.coroutines.delay`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.tsport.links.links.LinkExtractorTest"`

Expected: FAIL — varsayılan tavan 20 olduğu için 20 link dönüyor; `resolveBudgetMs` parametresi yok.

- [ ] **Step 3: Implement the cap and budget**

`LinkExtractor.kt`:

```kotlin
class LinkExtractor(
    private val resolver: UrlResolver,
    private val maxLinks: Int = 50,
    private val resolveBudgetMs: Long = 15_000
)
```

`extract` içindeki `t.co` bloğunu şu şekilde değiştir:

```kotlin
val resolvedTco = ConcurrentHashMap<String, String>()
coroutineScope {
    val permits = Semaphore(MAX_CONCURRENT_RESOLVES)
    val jobs = tcoUrls.map { url ->
        launch {
            permits.withPermit {
                resolver.resolve(url)?.let { resolvedTco[url] = it }
            }
        }
    }
    withTimeoutOrNull(resolveBudgetMs) { jobs.joinAll() }
    jobs.forEach { it.cancel() }
}
```

Import ekle: `java.util.concurrent.ConcurrentHashMap`, `kotlinx.coroutines.joinAll`, `kotlinx.coroutines.launch`, `kotlinx.coroutines.withTimeoutOrNull`. `resolvedTco[candidate]` erişimi aynı kalır.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.tsport.links.links.LinkExtractorTest"`

Expected: PASS.

---

### Task 3: Daha Derin Kazıma ve 60 Saniye Toplam Süre

**Files:**
- Modify: `app/src/main/assets/scrape_dom.js:62`
- Modify: `app/src/main/java/com/tsport/links/search/DomLinkScraper.kt:35-36`
- Modify: `app/src/main/java/com/tsport/links/search/ApiFallbackScraper.kt:22-23`
- Modify: `app/src/main/java/com/tsport/links/search/SearchViewModel.kt:80`
- Test: `app/src/test/java/com/tsport/links/search/SearchViewModelTest.kt:94-121`

**Interfaces:**
- Consumes: Task 1'in `SearchOptions.query` alanı ve Task 2'nin 50 link tavanı.
- Produces:
  - `DomLinkScraper.scrape(searchUrl, tab, maxCandidates = 150, timeoutMs = 30_000)`
  - `ApiFallbackScraper.scrape(csrfToken, maxPages = 6, timeoutMs = 30_000)`
  - `SearchViewModel` toplam süresi `60_000L`
  - DOM script kaydırma turu 15.

- [ ] **Step 1: Update the timeout tests to the new budget**

`SearchViewModelTest.kt` içindeki iki testte `advanceTimeBy(26_000)` → `advanceTimeBy(61_000)`. Test adlarını koru:

- `source time counts toward total timeout`
- `link extraction time counts toward total timeout`

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.tsport.links.search.SearchViewModelTest"`

Expected: FAIL — toplam süre hâlâ 25 sn olduğu için 61 sn ilerletmeden önce durum `Failure` olmuyor.

- [ ] **Step 3: Raise the limits**

`SearchViewModel.kt`:

```kotlin
private const val SEARCH_TIMEOUT_MS = 60_000L
```

`DomLinkScraper.kt` imzası:

```kotlin
suspend fun scrape(
    searchUrl: String,
    tab: SearchTab = SearchTab.LATEST,
    maxCandidates: Int = 150,
    timeoutMs: Long = 30_000
): List<LinkCandidate>
```

`ApiFallbackScraper.kt` imzası:

```kotlin
suspend fun scrape(
    csrfToken: String,
    maxPages: Int = 6,
    timeoutMs: Long = 30_000
): List<LinkCandidate>
```

`scrape_dom.js:62`:

```javascript
for (let i = 0; i < 15 && found.size < MAX; i++) {
```

Başka DOM/JS değişikliği yapılmaz; `__MAX__` zaten `maxCandidates` ile dolduruluyor.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.tsport.links.search.SearchViewModelTest"`

Expected: PASS.

---

### Task 4: Tam Doğrulama ve Cihaz Testi

**Files:**
- No source changes.

**Interfaces:**
- Consumes: Task 1-3 çıktıları.
- Produces: doğrulama kanıtı; commit yok.

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`, tüm testler geçer.

- [ ] **Step 2: Run lint and build the debug APK**

Run: `./gradlew lint assembleDebug`

Expected: `BUILD SUCCESSFUL`; APK: `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Verify JavaScript syntax**

Run: `node --check app/src/main/assets/scrape_dom.js && node --check app/src/main/assets/fallback_api.js`

Expected: çıkış kodu 0.

- [ ] **Step 4: Install and run on the device**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Sonra cihazda:
1. `SAMvAMD` yaz, `broadcasts` kutusunu işaretle, `En son` seç, `Ara`.
2. X uygulamasında `SAMvAMD broadcasts` aramasıyla sonucu karşılaştır.
3. Listenin 50 linke kadar dolabildiğini ve kaydırılabildiğini doğrula.
4. `Popüler` seçip tekrar ara; sekme davranışını ve uyarıyı kontrol et.
5. Uygulamayı kapat/aç; son arama metni, kutular ve sekmenin korunduğunu doğrula.

Expected: manuel aramayla aynı türden broadcast linkleri; 50 tavanı; seçimler kalıcı.

- [ ] **Step 5: Report evidence**

Test sayısı, lint/build sonucu, APK yolu ve cihazda gözlenen sonuç sayısı raporlanır. Commit yapılmaz.
