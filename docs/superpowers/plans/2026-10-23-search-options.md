# TSport Arama Seçenekleri Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kullanıcının yalnızca hashtag girerek, isteğe bağlı `"broadcasts"` ve `filter:links` ekleyerek, Popüler/En son sekmesini seçerek arama yapmasını sağlamak; sonuçlar her durumda yalnızca X broadcast linkleri olsun.

**Architecture:** `SearchQueryBuilder` parametreli `SearchOptions` üretir ve URL'e `f=` eklemez. Sekme seçimi, arama sayfası yüklendikten sonra `DomLinkScraper` içinde X'in kendi "En son" sekmesine JS ile tıklanarak yapılır. `LinkExtractor` mevcut broadcast filtresini korur.

**Tech Stack:** Kotlin 1.9.24, Android Views + ViewBinding, Coroutines, WebView, JUnit4.

**Spec:** `docs/superpowers/specs/2026-10-23-search-options-design.md`

## Global Constraints

- Kullanıcı alanı yalnızca hashtag alır; serbest X sorgusu desteklenmez.
- İki seçenek varsayılan olarak kapalıdır: `"broadcasts"` ve `filter:links`.
- Varsayılan sekme `En son`; `Popüler` de seçilebilir.
- Arama URL'ine `f=top` veya `f=live` **eklenmez**. Sekme, sayfadaki X sekmesine tıklanarak seçilir.
- Sonuçlar her durumda yalnızca `https://x.com/i/broadcasts/<id>` linkleridir; en fazla 20 tekil link.
- Tüm kullanıcı metinleri Türkçe; TV'de tüm kontroller D-pad ile odaklanabilir.
- Tag, seçenekler ve sekme `SharedPreferences`'ta saklanır.
- Commit yalnızca kullanıcı isterse yapılır.

## Review Focus

1. **Yalnızca tag araması:** Sorgu `#tag` olmalı; `"broadcasts"`, `filter:links` ve `f=` içermemeli. Sonuç yine yalnızca broadcast linkleri olmalı.
2. **"En son" sekmesi bulunamazsa:** Arama varsayılan sekmede sonuç döndürmeli ve "Sekme değiştirilemedi, Popüler sonuçlar gösteriliyor." uyarısı görünmeli.
3. **Tag kenar durumları:** Baştaki `#`, boşluklar, Türkçe karakterler ve boş tag doğru işlenmeli; URL çift `#` veya `f=` içermemeli.
4. **Kontroller:** Arama sürerken hashtag, checkbox'lar ve sekmeler devre dışı kalmalı; uygulama yeniden açıldığında son seçimler korunmalı; TV'de D-pad ile odaklanabilmeli.
5. **Geniş sorguda sonuç filtresi:** Yalnızca tag arandığında bile broadcast dışı linkler ve çözülemeyen `t.co` linkleri listeye girmemeli.

---

### Task 1: Sorgu seçenekleri ve URL üretimi

**Files:**
- Modify: `app/src/main/java/com/tsport/links/search/SearchQueryBuilder.kt`
- Test: `app/src/test/java/com/tsport/links/search/SearchQueryBuilderTest.kt`

**Interfaces:**
- Consumes: yok.
- Produces:
  - `enum class SearchTab { POPULAR, LATEST }`
  - `data class SearchOptions(val hashtag: String, val includeBroadcasts: Boolean = false, val linksOnly: Boolean = false, val tab: SearchTab = SearchTab.LATEST)`
  - `fun SearchQueryBuilder.buildSearchUrl(options: SearchOptions): String?`

- [ ] **Step 1: Write the failing tests**

Replace `SearchQueryBuilderTest.kt` with:

```kotlin
package com.tsport.links.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryBuilderTest {

    @Test
    fun `builds tag only url without f parameter`() {
        assertEquals(
            "https://x.com/search?q=%23GsvBJK&src=typed_query",
            SearchQueryBuilder.buildSearchUrl(SearchOptions("GsvBJK"))
        )
    }

    @Test
    fun `adds broadcasts when selected`() {
        assertEquals(
            "https://x.com/search?q=%23GsvBJK%20%22broadcasts%22&src=typed_query",
            SearchQueryBuilder.buildSearchUrl(SearchOptions("GsvBJK", includeBroadcasts = true))
        )
    }

    @Test
    fun `adds filter links when selected`() {
        assertEquals(
            "https://x.com/search?q=%23GsvBJK%20filter%3Alinks&src=typed_query",
            SearchQueryBuilder.buildSearchUrl(SearchOptions("GsvBJK", linksOnly = true))
        )
    }

    @Test
    fun `adds both options`() {
        assertEquals(
            "https://x.com/search?q=%23GsvBJK%20%22broadcasts%22%20filter%3Alinks&src=typed_query",
            SearchQueryBuilder.buildSearchUrl(
                SearchOptions("GsvBJK", includeBroadcasts = true, linksOnly = true)
            )
        )
    }

    @Test
    fun `does not put tab in url`() {
        val popular = SearchQueryBuilder.buildSearchUrl(SearchOptions("GsvBJK", tab = SearchTab.POPULAR))
        val latest = SearchQueryBuilder.buildSearchUrl(SearchOptions("GsvBJK", tab = SearchTab.LATEST))
        assertEquals(popular, latest)
        assertFalse(popular!!.contains("f="))
    }

    @Test
    fun `trims leading hash and whitespace`() {
        assertEquals(
            SearchQueryBuilder.buildSearchUrl(SearchOptions("GsvBJK")),
            SearchQueryBuilder.buildSearchUrl(SearchOptions("  #GsvBJK  "))
        )
    }

    @Test
    fun `returns null for blank hashtag`() {
        assertNull(SearchQueryBuilder.buildSearchUrl(SearchOptions("   ")))
        assertNull(SearchQueryBuilder.buildSearchUrl(SearchOptions(" # ")))
    }

    @Test
    fun `encodes turkish characters`() {
        assertEquals(
            "https://x.com/search?q=%23Fenerbah%C3%A7e&src=typed_query",
            SearchQueryBuilder.buildSearchUrl(SearchOptions("Fenerbahçe"))
        )
    }

    @Test
    fun `removes internal whitespace`() {
        assertEquals(
            SearchQueryBuilder.buildSearchUrl(SearchOptions("GsvBJK")),
            SearchQueryBuilder.buildSearchUrl(SearchOptions("Gsv BJK"))
        )
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest --tests "com.tsport.links.search.SearchQueryBuilderTest"
```

Expected: FAIL — `SearchOptions` / `SearchTab` unresolved.

- [ ] **Step 3: Implement `SearchTab`, `SearchOptions` and `buildSearchUrl`**

In `SearchQueryBuilder.kt`:

```kotlin
enum class SearchTab { POPULAR, LATEST }

data class SearchOptions(
    val hashtag: String,
    val includeBroadcasts: Boolean = false,
    val linksOnly: Boolean = false,
    val tab: SearchTab = SearchTab.LATEST
)

object SearchQueryBuilder {

    fun buildSearchUrl(options: SearchOptions): String? {
        val tag = options.hashtag.trim().removePrefix("#").replace(Regex("\\s+"), "")
        if (tag.isEmpty()) return null
        val query = buildString {
            append("#$tag")
            if (options.includeBroadcasts) append(" \"broadcasts\"")
            if (options.linksOnly) append(" filter:links")
        }
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return "https://x.com/search?q=$encoded&src=typed_query"
    }
}
```

`f=top` / `f=live` hiçbir koşulda URL'e eklenmez.

- [ ] **Step 4: Run tests to verify they pass**

Run the command from Step 2. Expected: PASS (9 test).

---

### Task 2: ViewModel'ın `SearchOptions` alması

**Files:**
- Modify: `app/src/main/java/com/tsport/links/search/SearchViewModel.kt:26-34`
- Test: `app/src/test/java/com/tsport/links/search/SearchViewModelTest.kt`

**Interfaces:**
- Consumes: `SearchOptions`, `SearchQueryBuilder.buildSearchUrl(options)`.
- Produces: `fun SearchViewModel.search(options: SearchOptions, candidateSource: suspend (searchUrl: String) -> List<LinkCandidate>)`

- [ ] **Step 1: Update tests to the new signature and add a URL assertion**

In `SearchViewModelTest.kt`, replace every `viewModel.search("GsvBJK")` / `viewModel.search("   ")` call with `viewModel.search(SearchOptions("GsvBJK"))` / `viewModel.search(SearchOptions("   "))`.

Add this test and the `assertFalse` import:

```kotlin
@Test
fun `source receives url without f parameter`() = runTest {
    var receivedUrl: String? = null
    val viewModel = SearchViewModel(LinkExtractor(resolver))
    viewModel.search(SearchOptions("GsvBJK")) { searchUrl ->
        receivedUrl = searchUrl
        emptyList()
    }
    advanceUntilIdle()
    assertTrue(receivedUrl != null)
    assertFalse(receivedUrl!!.contains("f="))
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest --tests "com.tsport.links.search.SearchViewModelTest"
```

Expected: FAIL — `search(String, ...)` çağrıları `SearchOptions` ile eşleşmiyor.

- [ ] **Step 3: Change `SearchViewModel.search`**

Replace the signature and URL construction:

```kotlin
fun search(
    options: SearchOptions,
    candidateSource: suspend (searchUrl: String) -> List<LinkCandidate>
) {
    val searchUrl = SearchQueryBuilder.buildSearchUrl(options)
    if (searchUrl == null) {
        _state.value = SearchState.Failure("Geçerli bir hashtag girin.")
        return
    }
    // geri kalan gövde değişmez
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run the command from Step 2. Expected: PASS.

---

### Task 3: "En son" sekmesini sayfadan seçme

**Files:**
- Modify: `app/src/main/java/com/tsport/links/search/DomLinkScraper.kt:27-50`

**Interfaces:**
- Consumes: `SearchTab`.
- Produces:
  - `suspend fun scrape(searchUrl: String, tab: SearchTab = SearchTab.POPULAR, maxCandidates: Int = 60, timeoutMs: Long = 25_000): List<LinkCandidate>`
  - `val lastTabSelected: Boolean` (salt okunur; son aramada sekme seçilebildiyse `true`)

- [ ] **Step 1: Add the tab state and new signature**

Add the field and change `scrape`:

```kotlin
@Volatile
var lastTabSelected: Boolean = true
    private set

suspend fun scrape(
    searchUrl: String,
    tab: SearchTab = SearchTab.POPULAR,
    maxCandidates: Int = 60,
    timeoutMs: Long = 25_000
): List<LinkCandidate> = withTimeout(timeoutMs) {
    withContext(Dispatchers.Main) {
        loadAndWait(searchUrl)
        delay(2_000)
        lastTabSelected = if (tab == SearchTab.LATEST) {
            selectLatestTab().also { selected -> if (selected) delay(3_000) }
        } else {
            true
        }
        // mevcut deferred / evaluateJavascript / parseCandidates bloğu aynen kalır
    }
}
```

- [ ] **Step 2: Implement `selectLatestTab`**

Add to `DomLinkScraper`; `withTimeoutOrNull` import edilir:

```kotlin
private suspend fun selectLatestTab(): Boolean {
    val js = """
        (function () {
          const tab = document.querySelector('a[role="tab"][href*="f=live"]');
          if (!tab) return "missing";
          if (tab.getAttribute("aria-selected") === "true") return "already";
          tab.click();
          return "clicked";
        })();
    """.trimIndent()
    for (attempt in 0 until 8) {
        val result = CompletableDeferred<String>()
        webView.evaluateJavascript(js) { value -> result.complete(value) }
        val status = withTimeoutOrNull(500) { result.await() }
        if (status != null && (status.contains("clicked") || status.contains("already"))) {
            return true
        }
        delay(300)
    }
    return false
}
```

Sekme bulunamazsa `lastTabSelected = false` olur; arama yine varsayılan sekmede devam eder.

- [ ] **Step 3: Build**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. Sekme tıklaması WebView gerektirdiği için birim testi yok; manuel doğrulama Task 5'te.

---

### Task 4: Arayüz kontrolleri ve `MainActivity` bağlantısı

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/tsport/links/ui/MainActivity.kt`

**Interfaces:**
- Consumes: `SearchOptions`, `SearchTab`, `DomLinkScraper.scrape(searchUrl, tab)`, `DomLinkScraper.lastTabSelected`.
- Produces: çalışan arama ekranı; yeni `SharedPreferences` anahtarları.

- [ ] **Step 1: Add strings**

In `strings.xml`:

```xml
<string name="option_broadcasts">“broadcasts” kelimesini ekle</string>
<string name="option_links_only">filter:links (sadece link içerenler)</string>
<string name="tab_popular">Popüler</string>
<string name="tab_latest">En son</string>
<string name="status_tab_fallback">Sekme değiştirilemedi, Popüler sonuçlar gösteriliyor.</string>
```

Ayrıca `status_empty` metnini şu şekilde güncelle:

```xml
<string name="status_empty">Sonuç yok. Hashtag\'i kontrol edip tekrar deneyin.</string>
```

- [ ] **Step 2: Add controls to `activity_main.xml`**

`ProgressBar`'dan önce, arama satırının altına ekle:

```xml
<CheckBox
    android:id="@+id/broadcastsCheck"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:buttonTint="@color/accent"
    android:focusable="true"
    android:text="@string/option_broadcasts"
    android:textColor="@color/on_surface" />

<CheckBox
    android:id="@+id/linksOnlyCheck"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:buttonTint="@color/accent"
    android:focusable="true"
    android:text="@string/option_links_only"
    android:textColor="@color/on_surface" />

<RadioGroup
    android:id="@+id/tabGroup"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    android:orientation="horizontal">

    <RadioButton
        android:id="@+id/tabPopular"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:buttonTint="@color/accent"
        android:focusable="true"
        android:text="@string/tab_popular"
        android:textColor="@color/on_surface" />

    <RadioButton
        android:id="@+id/tabLatest"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:buttonTint="@color/accent"
        android:focusable="true"
        android:text="@string/tab_latest"
        android:textColor="@color/on_surface" />
</RadioGroup>
```

- [ ] **Step 3: Read and persist selections in `MainActivity`**

In `onCreate`, after `hashtagInput.setText(...)`:

```kotlin
binding.broadcastsCheck.isChecked = prefs().getBoolean(KEY_INCLUDE_BROADCASTS, false)
binding.linksOnlyCheck.isChecked = prefs().getBoolean(KEY_LINKS_ONLY, false)
if (prefs().getBoolean(KEY_TAB_LATEST, true)) {
    binding.tabLatest.isChecked = true
} else {
    binding.tabPopular.isChecked = true
}
```

Add a field to keep the active options:

```kotlin
private var lastOptions: SearchOptions? = null
```

In `startSearch`, build the options before `viewModel.search`:

```kotlin
val options = SearchOptions(
    hashtag = binding.hashtagInput.text.toString(),
    includeBroadcasts = binding.broadcastsCheck.isChecked,
    linksOnly = binding.linksOnlyCheck.isChecked,
    tab = if (binding.tabLatest.isChecked) SearchTab.LATEST else SearchTab.POPULAR
)
lastOptions = options
prefs().edit()
    .putString(KEY_LAST_HASHTAG, options.hashtag)
    .putBoolean(KEY_INCLUDE_BROADCASTS, options.includeBroadcasts)
    .putBoolean(KEY_LINKS_ONLY, options.linksOnly)
    .putBoolean(KEY_TAB_LATEST, options.tab == SearchTab.LATEST)
    .apply()
```

Replace the search call with:

```kotlin
viewModel.search(options) { searchUrl ->
    val domCandidates = domScraper.scrape(searchUrl, options.tab)
    if (!authRepository.isLoggedIn()) {
        throw SessionExpiredException()
    }
    domCandidates + fallbackScraper.scrape(authRepository.csrfToken().orEmpty())
}
```

Add the new preference keys to the companion object:

```kotlin
private const val KEY_INCLUDE_BROADCASTS = "include_broadcasts"
private const val KEY_LINKS_ONLY = "links_only"
private const val KEY_TAB_LATEST = "tab_latest"
```

- [ ] **Step 4: Disable controls during search and show the tab warning**

In `render`, extend the busy handling:

```kotlin
binding.broadcastsCheck.isEnabled = !searching
binding.linksOnlyCheck.isEnabled = !searching
binding.tabPopular.isEnabled = !searching
binding.tabLatest.isEnabled = !searching
```

In the `SearchState.Success` branch:

```kotlin
val warning = lastOptions
    ?.takeIf { it.tab == SearchTab.LATEST && !domScraper.lastTabSelected }
    ?.let { getString(R.string.status_tab_fallback) }
val found = getString(R.string.status_found, state.links.size)
binding.statusText.text = if (warning == null) found else "$found · $warning"
```

Imports ekle: `com.tsport.links.search.SearchOptions`, `com.tsport.links.search.SearchTab`.

- [ ] **Step 5: Build**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 5: Doğrulama

**Files:**
- Test: mevcut test dosyaları; yeni dosya yok.

- [ ] **Step 1: Run the full unit test suite**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

Expected: PASS, failures 0.

- [ ] **Step 2: Run lint and build**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lint assembleDebug
```

Expected: `BUILD SUCCESSFUL`; APK `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Verify JS assets still parse**

Run:

```bash
node --check app/src/main/assets/scrape_dom.js && node --check app/src/main/assets/fallback_api.js
```

Expected: çıktı yok, çıkış kodu 0.

- [ ] **Step 4: Manual device checklist**

Aynı hesapla cihazda doğrula:

1. Sadece `GsvBJK` ile ara → uygulama URL'i `...q=%23GsvBJK&src=typed_query` olmalı; sonuçlar yalnızca broadcast linkleri.
2. `"broadcasts"` seç → sorguya `%20%22broadcasts%22` eklenmeli.
3. `filter:links` seç → sorguya `%20filter%3Alinks` eklenmeli.
4. İkisini birlikte seç → ikisi de eklenmeli.
5. `En son` seç → sayfa "En son" sekmesine geçmeli; sonuçlar gelmeli. X sekmesi değişmezse uyarı görünmeli.
6. `Popüler` seç → varsayılan sekmede arama yapılmalı.
7. Uygulamayı kapat/aç → tag, iki checkbox ve sekme korunmalı.
8. TV'de D-pad ile checkbox'lar ve sekmeler odaklanabilmeli.

- [ ] **Step 5: Commit (yalnızca kullanıcı isterse)**

```bash
git add app/src/main/java/com/tsport/links/search/SearchQueryBuilder.kt \
        app/src/main/java/com/tsport/links/search/SearchViewModel.kt \
        app/src/main/java/com/tsport/links/search/DomLinkScraper.kt \
        app/src/main/java/com/tsport/links/ui/MainActivity.kt \
        app/src/main/res/layout/activity_main.xml \
        app/src/main/res/values/strings.xml \
        app/src/test/java/com/tsport/links/search/SearchQueryBuilderTest.kt \
        app/src/test/java/com/tsport/links/search/SearchViewModelTest.kt
git commit -m "feat: add selectable search options and tabs"
```
