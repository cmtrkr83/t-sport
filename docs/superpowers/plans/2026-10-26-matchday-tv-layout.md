# Maç Günü Paneli ve TV Yerleşimi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Boş açılış ekranını bugünün Süper Lig maçlarıyla doldurmak; maç seçiminde `broadcasts` aramasını otomatik başlatmak ve ekranı Android TV önceliğiyle iki sütunlu, telefonlarda dikey/yatay kullanılabilir hale getirmek.

**Architecture:** TheSportsDB ücretsiz fikstür API'si OkHttp ile okunur; saf bir parser JSON'u `TodayMatch` listesine çevirir; `MatchDayViewModel` 15 dakikalık bellek önbelleği ve `Loading / Success / Empty / Failure` durumlarını yönetir. `MainActivity` iki ViewModel'i birleştirir: arama `Idle` iken maç paneli, aksi halde mevcut sonuç paneli görünür. Yerleşim `layout/` (dikey) ve `layout-land/` (iki sütun) olarak yazılır; TV boyutları `values-television/` ile büyütülür.

**Tech Stack:** Kotlin, Android Views + ViewBinding, RecyclerView, coroutines, OkHttp, `org.json`, JUnit4, MockWebServer, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-26-matchday-tv-layout-design.md`

## Global Constraints

- Android TV birincil hedef; telefon dikey ve yatay da kullanılabilir olmalı.
- TV güvenli alanı **48dp**, telefon **24dp**. Sistem çubukları buna eklenir.
- TV tipografisi: başlık 28sp, gövde 20sp, ikincil 16sp; kontrol yüksekliği en az 56dp.
- Telefon tipografisi: başlık 24sp, gövde 16sp, ikincil 14sp; dokunma hedefi en az 48dp.
- Tüm kontroller D-pad ile odaklanabilir; odaktaki satır 3dp camgöbeği çerçeve taşır.
- Arayüz Türkçe. Seçenekler `SharedPreferences` anahtarları değişmeden kalıcı olur.
- Maç seçimi: sorgu `"$homeTeam $awayTeam"`, `broadcasts` işaretli, arama otomatik.
- İlk açılışta maç paneli; arama `Searching / Success / Empty / Failure` iken sonuç paneli görünür.
- `Günün maçları` aramayı iptal eder, formu/`broadcasts` seçimini geri almaz.
- TV'de ilk maç odağı yalnızca maç paneli görünürken, arama `Idle` iken, kullanıcı yazmıyorken ve yükleme başına bir kez alınır.
- Yalnızca resmi ücretsiz API kullanılır. `browse_tv` sayfası veya başka web sayfası kazınmaz.
- Yeni bağımlılık eklenmez. `main` üzerinde doğrudan çalışılır; worktree açılmaz.
- **Kullanıcı açıkça istemedikçe commit yapılmaz.** Tüm değişiklikler çalışma ağacında bırakılır.
- Mevcut serbest metin araması, 50 link tavanı ve testler korunur.
- Bu makinede sistem Java'sı 8'dir; AGP 8.6.1 çalışmaz. Gradle komutlarını Android Studio JBR 17 ile çalıştır:
  `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew ...`
- Baseline doğrulandı: 58 test, 0 failure (`testDebugUnitTest`).

## Review Focus

- Parser yalnızca `idLeague == "4339"` kayıtlarını almalı; takım alanları eksikse `strEvent` içindeki `A vs B` yedeğini kullanmalı, yine eksikse satırı atlamalı.
- `intRound`, skorlar ve saat alanları JSON'da sayı **veya** metin olarak gelebilir; ikisi de okunmalı.
- `dateEventLocal` katı filtre değildir; gece maçı farklı gün gösterse de kayıt korunur.
- Önbellek 15 dakika; tarih değişince geçersiz. Eski liste varken arka plan yenilemesi listeyi kaybettirmemeli.
- Maç seçimi `SearchQueryBuilder` ile `Galatasaray Fenerbahçe broadcasts` URL'sini üretmeli; `vs` aramaya girmemeli.
- TV odak yardımcısı arama sürerken, kullanıcı yazarken veya odak bir kez alındıktan sonra `false` dönmeli.
- `Günün maçları` oturum hatasında görünmemeli; diğer tüm arama durumlarında görünmeli.
- İki activity layout'unda aynı view ID'leri bulunmalı; yoksa ViewBinding çalışma zamanında kırılır.

## File Map

**New:**
- `app/src/main/java/com/tsport/links/matchday/TodayMatch.kt`
- `app/src/main/java/com/tsport/links/matchday/MatchDayParser.kt`
- `app/src/main/java/com/tsport/links/matchday/IstanbulDate.kt`
- `app/src/main/java/com/tsport/links/matchday/MatchDaySource.kt`
- `app/src/main/java/com/tsport/links/matchday/TheSportsDbSource.kt`
- `app/src/main/java/com/tsport/links/matchday/MatchDayState.kt`
- `app/src/main/java/com/tsport/links/matchday/MatchDayViewModel.kt`
- `app/src/main/java/com/tsport/links/ui/MatchDayViewModelFactory.kt`
- `app/src/main/java/com/tsport/links/ui/MatchDayUi.kt`
- `app/src/main/java/com/tsport/links/ui/MatchAdapter.kt`
- `app/src/main/res/layout/item_match.xml`
- `app/src/main/res/layout-land/activity_main.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/values-television/dimens.xml`
- `app/src/main/res/drawable/ic_football.xml`
- `app/src/main/res/drawable/bg_monogram.xml`
- `app/src/test/java/com/tsport/links/matchday/MatchDayParserTest.kt`
- `app/src/test/java/com/tsport/links/matchday/MatchDayQueryTest.kt`
- `app/src/test/java/com/tsport/links/matchday/IstanbulDateTest.kt`
- `app/src/test/java/com/tsport/links/matchday/TheSportsDbSourceTest.kt`
- `app/src/test/java/com/tsport/links/matchday/MatchDayViewModelTest.kt`
- `app/src/test/java/com/tsport/links/ui/MatchDayUiTest.kt`

**Modify:**
- `app/src/main/java/com/tsport/links/ui/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/item_link.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/drawable/focus_background.xml`
- `app/src/main/AndroidManifest.xml`

---

### Task 1: Maç Modeli, Parser, Tarih ve Sorgu

**Files:**
- Create: `app/src/main/java/com/tsport/links/matchday/TodayMatch.kt`
- Create: `app/src/main/java/com/tsport/links/matchday/MatchDayParser.kt`
- Create: `app/src/main/java/com/tsport/links/matchday/IstanbulDate.kt`
- Test: `app/src/test/java/com/tsport/links/matchday/MatchDayParserTest.kt`
- Test: `app/src/test/java/com/tsport/links/matchday/MatchDayQueryTest.kt`
- Test: `app/src/test/java/com/tsport/links/matchday/IstanbulDateTest.kt`

**Interfaces:**
- Consumes: mevcut `SearchOptions`, `SearchQueryBuilder`.
- Produces:
  - `enum class MatchStatus { SCHEDULED, FINISHED, LIVE, POSTPONED, CANCELLED, UNKNOWN }`
  - `data class TodayMatch(id, homeTeam, awayTeam, time, venue, round, homeScore, awayScore, status)`
  - `TodayMatch.query: String` (takım adları, `vs` yok)
  - `TodayMatch.scoreText: String?` (iki skor da varsa `"2 - 1"`)
  - `object MatchDayParser { fun parse(json: String): List<TodayMatch> }`
  - `object IstanbulDate { fun today(nowMillis: Long = System.currentTimeMillis()): String }`

- [ ] **Step 1: Write the failing parser, query and date tests**

`MatchDayParserTest.kt` en az şu davranışları kapsamalı:

```kotlin
package com.tsport.links.matchday

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchDayParserTest {

    private val superLig = """
        {
          "events": [
            {
              "idEvent": "3",
              "idLeague": "4339",
              "strEvent": "Rizespor vs Trabzonspor",
              "strHomeTeam": "Rizespor",
              "strAwayTeam": "Trabzonspor",
              "strTimeLocal": "19:00:00",
              "dateEventLocal": "2026-10-27",
              "strVenue": "Çaykur Didi Stadyumu",
              "intRound": "9",
              "intHomeScore": null,
              "intAwayScore": null,
              "strStatus": "NS"
            },
            {
              "idEvent": "1",
              "idLeague": "4339",
              "strEvent": "Beşiktaş vs İstanbul Başakşehir",
              "strHomeTeam": "Beşiktaş",
              "strAwayTeam": "İstanbul Başakşehir",
              "strTimeLocal": "19:00:00",
              "strVenue": "Tüpraş Stadyumu",
              "intRound": "9",
              "intHomeScore": "2",
              "intAwayScore": "1",
              "strStatus": "FT"
            },
            {
              "idEvent": "2",
              "idLeague": "4339",
              "strEvent": "Galatasaray vs Fenerbahçe",
              "strHomeTeam": "Galatasaray",
              "strAwayTeam": "Fenerbahçe",
              "strTimeLocal": "21:30:00",
              "strVenue": "Rams Park",
              "intRound": 9,
              "intHomeScore": null,
              "intAwayScore": null,
              "strStatus": "NS"
            },
            {
              "idEvent": "99",
              "idLeague": "9999",
              "strEvent": "Other vs Match",
              "strHomeTeam": "Other",
              "strAwayTeam": "Match",
              "strTimeLocal": "20:00:00"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `keeps only super lig events and sorts by kickoff`() {
        val matches = MatchDayParser.parse(superLig)
        assertEquals(listOf("1", "3", "2"), matches.map { it.id })
        assertEquals("Beşiktaş", matches[0].homeTeam)
        assertEquals("19:00", matches[0].time)
        assertEquals("Rams Park", matches[2].venue)
        assertEquals(9, matches[2].round)
    }

    @Test
    fun `parses scores and status labels`() {
        val matches = MatchDayParser.parse(superLig)
        assertEquals("2 - 1", matches[0].scoreText)
        assertEquals(MatchStatus.FINISHED, matches[0].status)
        assertEquals(MatchStatus.SCHEDULED, matches[1].status)
        assertNull(matches[1].scoreText)
    }

    @Test
    fun `falls back to event text when team fields are missing`() {
        val json = """{"events":[{"idEvent":"7","idLeague":"4339",
            "strEvent":"A vs B","strTimeLocal":"20:00:00","strStatus":"NS"}]}"""
        val match = MatchDayParser.parse(json).single()
        assertEquals("A", match.homeTeam)
        assertEquals("B", match.awayTeam)
        assertEquals("A B", match.query)
    }

    @Test
    fun `skips event without id or resolvable teams`() {
        val json = """{"events":[
            {"idLeague":"4339","strHomeTeam":"A","strAwayTeam":"B"},
            {"idEvent":"2","idLeague":"4339","strEvent":"not a fixture"}
        ]}"""
        assertTrue(MatchDayParser.parse(json).isEmpty())
    }

    @Test
    fun `maps postponed cancelled live and unknown statuses`() {
        val json = """{"events":[
            {"idEvent":"1","idLeague":"4339","strHomeTeam":"A","strAwayTeam":"B","strTimeLocal":"18:00:00","strStatus":"POST"},
            {"idEvent":"2","idLeague":"4339","strHomeTeam":"C","strAwayTeam":"D","strTimeLocal":"18:00:00","strStatus":"CANC"},
            {"idEvent":"3","idLeague":"4339","strHomeTeam":"E","strAwayTeam":"F","strTimeLocal":"18:00:00","strStatus":"2H","intHomeScore":"1","intAwayScore":"0"},
            {"idEvent":"4","idLeague":"4339","strHomeTeam":"G","strAwayTeam":"H","strTimeLocal":"18:00:00","strStatus":"???"}
        ]}"""
        val statuses = MatchDayParser.parse(json).associate { it.id to it.status }
        assertEquals(MatchStatus.POSTPONED, statuses["1"])
        assertEquals(MatchStatus.CANCELLED, statuses["2"])
        assertEquals(MatchStatus.LIVE, statuses["3"])
        assertEquals(MatchStatus.UNKNOWN, statuses["4"])
    }

    @Test
    fun `missing optional time is shown as placeholder and sorted last`() {
        val json = """{"events":[
            {"idEvent":"1","idLeague":"4339","strHomeTeam":"A","strAwayTeam":"B","strStatus":"NS"},
            {"idEvent":"2","idLeague":"4339","strHomeTeam":"C","strAwayTeam":"D","strTimeLocal":"10:00:00","strStatus":"NS"}
        ]}"""
        val matches = MatchDayParser.parse(json)
        assertEquals("--:--", matches.last().time)
        assertEquals("2", matches.first().id)
    }

    @Test
    fun `empty null and malformed responses`() {
        assertTrue(MatchDayParser.parse("""{"events":null}""").isEmpty())
        assertTrue(MatchDayParser.parse("""{"events":[]}""").isEmpty())
        assertThrows(JSONException::class.java) { MatchDayParser.parse("not json") }
    }
}
```

`MatchDayQueryTest.kt`:

```kotlin
package com.tsport.links.matchday

import com.tsport.links.search.SearchOptions
import com.tsport.links.search.SearchQueryBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchDayQueryTest {

    @Test
    fun `match selection builds broadcasts search without vs`() {
        val match = TodayMatch(
            id = "2527798", homeTeam = "Galatasaray", awayTeam = "Fenerbahçe",
            time = "21:30", venue = "Rams Park", round = 9,
            homeScore = null, awayScore = null, status = MatchStatus.SCHEDULED
        )
        val url = SearchQueryBuilder.buildSearchUrl(
            SearchOptions(match.query, includeBroadcasts = true)
        )
        assertEquals(
            "https://x.com/search?q=Galatasaray%20Fenerbah%C3%A7e%20broadcasts&src=typed_query",
            url
        )
    }
}
```

`IstanbulDateTest.kt`:

```kotlin
package com.tsport.links.matchday

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class IstanbulDateTest {

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun `uses europe istanbul date across utc midnight`() {
        assertEquals("2026-10-26", IstanbulDate.today(utc(2026, 10, 25, 22, 0)))
        assertEquals("2026-10-26", IstanbulDate.today(utc(2026, 10, 26, 20, 59)))
        assertEquals("2026-10-27", IstanbulDate.today(utc(2026, 10, 26, 21, 1)))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
./gradlew testDebugUnitTest \
  --tests "com.tsport.links.matchday.MatchDayParserTest" \
  --tests "com.tsport.links.matchday.MatchDayQueryTest" \
  --tests "com.tsport.links.matchday.IstanbulDateTest"
```

Expected: FAIL — `TodayMatch`, `MatchDayParser` ve `IstanbulDate` yok.

- [ ] **Step 3: Implement the model and parser**

`TodayMatch.kt`:

```kotlin
package com.tsport.links.matchday

enum class MatchStatus { SCHEDULED, FINISHED, LIVE, POSTPONED, CANCELLED, UNKNOWN }

data class TodayMatch(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val time: String = "--:--",
    val venue: String? = null,
    val round: Int? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: MatchStatus = MatchStatus.UNKNOWN
) {
    val query: String get() = "$homeTeam $awayTeam"
    val scoreText: String?
        get() = if (homeScore != null && awayScore != null) "$homeScore - $awayScore" else null
}
```

`MatchDayParser.kt` kuralları:

1. `JSONObject(json).optJSONArray("events") ?: return emptyList()`.
2. Her kayıtta `optString("idLeague") != "4339"` ise atla.
3. `idEvent` boşsa atla.
4. `strHomeTeam` / `strAwayTeam` boşsa `strEvent` değerini `(?i)\s+(?:vs\.?|@)\s+` ile iki parçaya ayırıp eksik tarafı doldur. Hâlâ eksikse atla.
5. Takım adlarını `trim()` ve ardışık boşlukları tek boşluğa indir.
6. `strTimeLocal`, yoksa `strTime` ilk 5 karakteri `HH:mm` biçimindeyse kullan; değilse `"--:--"`.
7. `strVenue` boşsa `null`; `intRound`, `intHomeScore`, `intAwayScore` sayı veya sayısal metin olarak okunmalı.
8. Durum eşlemesi:
   - `POST`, `PST`, `strPostponed == "yes"` → `POSTPONED`
   - `CANC`, `ABD` → `CANCELLED`
   - `FT`, `AET`, `PEN` → `FINISHED`
   - `1H`, `HT`, `2H`, `ET`, `P`, `BT` → `LIVE`
   - `null`, boş, `NS` → `SCHEDULED`
   - diğer → `UNKNOWN`
9. `dateEventLocal` filtresi uygulanmaz.
10. Aynı `id` ikinci kez gelirse ilk kayıt korunur (`distinctBy`).
11. Saate göre artan sırala; eşit saatte ev sahibi adına göre sırala; `"--:--"` en sona.
12. JSON bozuksa `JSONException` yukarı bırakılır; boş `events` boş liste döner.

`IstanbulDate.kt`:

```kotlin
package com.tsport.links.matchday

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object IstanbulDate {
    fun today(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Istanbul")
        }.format(Date(nowMillis))
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run the same three test classes. Expected: PASS.

---

### Task 2: TheSportsDB Fikstür Kaynağı

**Files:**
- Create: `app/src/main/java/com/tsport/links/matchday/MatchDaySource.kt`
- Create: `app/src/main/java/com/tsport/links/matchday/TheSportsDbSource.kt`
- Test: `app/src/test/java/com/tsport/links/matchday/TheSportsDbSourceTest.kt`

**Interfaces:**
- Consumes: `MatchDayParser.parse`.
- Produces:
  - `interface MatchDaySource { suspend fun load(date: String): Result<List<TodayMatch>> }`
  - `class TheSportsDbSource(client: OkHttpClient = defaultClient(), baseUrl: String = BASE_URL) : MatchDaySource`
  - `TheSportsDbSource.BASE_URL = "https://www.thesportsdb.com/api/v1/json/123/eventsday.php"`
  - `TheSportsDbSource.LEAGUE_ID = "4339"`
  - 10 saniye `connect/read/call` zaman aşımı.

- [ ] **Step 1: Write the failing source tests**

`TheSportsDbSourceTest.kt` şu testleri içermeli:

```kotlin
package com.tsport.links.matchday

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TheSportsDbSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var source: TheSportsDbSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        source = TheSportsDbSource(
            client = OkHttpClient(),
            baseUrl = server.url("/api/v1/json/123/eventsday.php").toString()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `requests super lig for requested date and parses response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"events":[{"idEvent":"1","idLeague":"4339",
                 "strHomeTeam":"Galatasaray","strAwayTeam":"Fenerbahçe",
                 "strTimeLocal":"21:30:00","strStatus":"NS"}]}"""
        ))
        val result = source.load("2026-10-26")
        assertTrue(result.isSuccess)
        assertEquals("Galatasaray", result.getOrThrow().single().homeTeam)
        val request = server.takeRequest()
        assertEquals("2026-10-26", request.requestUrl?.queryParameter("d"))
        assertEquals("4339", request.requestUrl?.queryParameter("l"))
        assertEquals("GET", request.method)
    }

    @Test
    fun `http error and malformed json become failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        assertTrue(source.load("2026-10-26").isFailure)

        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        assertTrue(source.load("2026-10-26").isFailure)
    }

    @Test
    fun `empty events is a successful empty list`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"events":null}"""))
        val result = source.load("2026-10-26")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }
}
```

Not: MockWebServer yerel `http` URL'i kullanır. Üretim `BASE_URL` sabiti `https` olmalı; şema testi yapılmaz.

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
./gradlew testDebugUnitTest --tests "com.tsport.links.matchday.TheSportsDbSourceTest"
```

Expected: FAIL — kaynak sınıfı yok.

- [ ] **Step 3: Implement the source**

`MatchDaySource.kt`:

```kotlin
package com.tsport.links.matchday

interface MatchDaySource {
    suspend fun load(date: String): Result<List<TodayMatch>>
}
```

`TheSportsDbSource.kt` davranışı:

```kotlin
class TheSportsDbSource(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = BASE_URL
) : MatchDaySource {

    override suspend fun load(date: String): Result<List<TodayMatch>> =
        withContext(Dispatchers.IO) {
            try {
                val url = baseUrl.toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.addQueryParameter("d", date)
                    ?.addQueryParameter("l", LEAGUE_ID)
                    ?.build()
                    ?: return@withContext Result.failure(IllegalArgumentException("Geçersiz API adresi"))
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", TcoResolver.USER_AGENT)
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Result.failure(IOException("HTTP ${response.code}"))
                    } else {
                        val body = response.body?.string()
                        if (body == null) {
                            Result.failure(IOException("Boş yanıt"))
                        } else {
                            Result.success(MatchDayParser.parse(body))
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    companion object {
        const val BASE_URL = "https://www.thesportsdb.com/api/v1/json/123/eventsday.php"
        const val LEAGUE_ID = "4339"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run the source test. Expected: PASS.

- [ ] **Step 5: Live API smoke check (manual, no code)**

```bash
curl -sS 'https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=2026-10-26&l=4339'
```

Expected: 2026-10-26 için Beşiktaş–Başakşehir, Rizespor–Trabzonspor ve Galatasaray–Fenerbahçe kayıtları.

---

### Task 3: Maç Günü Durumu, Önbellek ve ViewModel

**Files:**
- Create: `app/src/main/java/com/tsport/links/matchday/MatchDayState.kt`
- Create: `app/src/main/java/com/tsport/links/matchday/MatchDayViewModel.kt`
- Create: `app/src/main/java/com/tsport/links/ui/MatchDayViewModelFactory.kt`
- Test: `app/src/test/java/com/tsport/links/matchday/MatchDayViewModelTest.kt`

**Interfaces:**
- Consumes: `MatchDaySource.load(date)`, `IstanbulDate.today`.
- Produces:
  - `sealed interface MatchDayState { data object Loading; data class Success(date, matches); data object Empty; data object Failure }`
  - `MatchDayViewModel(source, dateProvider, nowProvider, cacheTtlMs)` with `state: StateFlow<MatchDayState>`, `load(force: Boolean = false)`, `retry()`.
  - `MatchDayViewModelFactory(source)`.

- [ ] **Step 1: Write the failing ViewModel tests**

`MatchDayViewModelTest.kt` şu testleri kapsamalı:

```kotlin
package com.tsport.links.matchday

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDayViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private var now = 1_000L
    private var date = "2026-10-26"

    private val match = TodayMatch(
        id = "1", homeTeam = "Galatasaray", awayTeam = "Fenerbahçe",
        time = "21:30", status = MatchStatus.SCHEDULED
    )

    private class FakeSource : MatchDaySource {
        var result: Result<List<TodayMatch>> = Result.success(emptyList())
        var calls = 0
        var lastDate: String? = null
        var delayMs = 0L

        override suspend fun load(date: String): Result<List<TodayMatch>> {
            calls++
            lastDate = date
            if (delayMs > 0) delay(delayMs)
            return result
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(source: MatchDaySource) = MatchDayViewModel(
        source = source,
        dateProvider = { date },
        nowProvider = { now },
        cacheTtlMs = 1_000L
    )

    @Test
    fun `loads matches and exposes success`() = runTest {
        val source = FakeSource().apply { result = Result.success(listOf(match)) }
        val vm = viewModel(source)
        assertTrue(vm.state.value is MatchDayState.Loading)
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value as MatchDayState.Success
        assertEquals("2026-10-26", state.date)
        assertEquals(listOf(match), state.matches)
        assertEquals("2026-10-26", source.lastDate)
    }

    @Test
    fun `empty list and failures map to matching states and retry works`() = runTest {
        val source = FakeSource().apply { result = Result.success(emptyList()) }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Empty)

        source.result = Result.failure(IllegalStateException("offline"))
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Failure)

        source.result = Result.success(listOf(match))
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Success)
        assertEquals(3, source.calls)
    }

    @Test
    fun `fresh cache avoids network until forced`() = runTest {
        val source = FakeSource().apply { result = Result.success(listOf(match)) }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        vm.load()
        advanceUntilIdle()
        assertEquals(1, source.calls)

        vm.load(force = true)
        advanceUntilIdle()
        assertEquals(2, source.calls)
    }

    @Test
    fun `changed istanbul date invalidates cache`() = runTest {
        val source = FakeSource().apply { result = Result.success(listOf(match)) }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()

        date = "2026-10-27"
        vm.load()
        advanceUntilIdle()
        assertEquals(2, source.calls)
        assertEquals("2026-10-27", (vm.state.value as MatchDayState.Success).date)
    }

    @Test
    fun `stale refresh keeps current matches visible`() = runTest {
        val source = FakeSource().apply {
            result = Result.success(listOf(match))
            delayMs = 1_000
        }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Success)

        now += 1_001
        source.delayMs = 10_000
        source.result = Result.success(listOf(match.copy(homeTeam = "Yeni")))
        vm.load()
        assertTrue(vm.state.value is MatchDayState.Success)

        advanceUntilIdle()
        assertEquals("Yeni", (vm.state.value as MatchDayState.Success).matches.single().homeTeam)
    }

    @Test
    fun `concurrent load for same date is not duplicated`() = runTest {
        val source = FakeSource().apply {
            result = Result.success(listOf(match))
            delayMs = 1_000
        }
        val vm = viewModel(source)
        vm.load()
        vm.load()
        assertEquals(1, source.calls)
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Success)
    }

    @Test
    fun `source exception becomes failure`() = runTest {
        val source = object : MatchDaySource {
            override suspend fun load(date: String): Result<List<TodayMatch>> =
                throw IllegalStateException("boom")
        }
        val vm = viewModel(source)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is MatchDayState.Failure)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
./gradlew testDebugUnitTest --tests "com.tsport.links.matchday.MatchDayViewModelTest"
```

Expected: FAIL — `MatchDayState` ve `MatchDayViewModel` yok.

- [ ] **Step 3: Implement the state and ViewModel**

`MatchDayState.kt`:

```kotlin
package com.tsport.links.matchday

sealed interface MatchDayState {
    data object Loading : MatchDayState
    data class Success(val date: String, val matches: List<TodayMatch>) : MatchDayState
    data object Empty : MatchDayState
    data object Failure : MatchDayState
}
```

`MatchDayViewModel.kt` davranışı:

```kotlin
class MatchDayViewModel(
    private val source: MatchDaySource,
    private val dateProvider: () -> String = { IstanbulDate.today() },
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val cacheTtlMs: Long = 15 * 60 * 1_000L
) : ViewModel() {

    private val _state = MutableStateFlow<MatchDayState>(MatchDayState.Loading)
    val state: StateFlow<MatchDayState> = _state.asStateFlow()

    private data class CacheEntry(
        val date: String,
        val loadedAt: Long,
        val matches: List<TodayMatch>
    )

    private var cache: CacheEntry? = null
    private var job: Job? = null
    private var inFlightDate: String? = null

    fun load(force: Boolean = false) {
        val date = dateProvider()
        val cached = cache

        if (!force && cached != null && cached.date == date &&
            nowProvider() - cached.loadedAt < cacheTtlMs
        ) {
            _state.value = cached.toState()
            return
        }
        if (!force && job?.isActive == true && inFlightDate == date) return

        val visibleCached = cached?.date == date && _state.value is MatchDayState.Success
        if (!visibleCached) _state.value = MatchDayState.Loading

        job?.cancel()
        inFlightDate = date
        job = viewModelScope.launch {
            val result = try {
                source.load(date)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }

            result.fold(
                onSuccess = { matches ->
                    cache = CacheEntry(date, nowProvider(), matches)
                    _state.value = if (matches.isEmpty()) {
                        MatchDayState.Empty
                    } else {
                        MatchDayState.Success(date, matches)
                    }
                },
                onFailure = {
                    if (!visibleCached) _state.value = MatchDayState.Failure
                }
            )
        }
    }

    fun retry() = load(force = true)

    private fun CacheEntry.toState(): MatchDayState =
        if (matches.isEmpty()) MatchDayState.Empty else MatchDayState.Success(date, matches)
}
```

`MatchDayViewModelFactory.kt`:

```kotlin
class MatchDayViewModelFactory(
    private val source: MatchDaySource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MatchDayViewModel(source) as T
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run the ViewModel tests. Expected: PASS.

---

### Task 4: Panel/Odak/Tarih Yardımcıları

**Files:**
- Create: `app/src/main/java/com/tsport/links/ui/MatchDayUi.kt`
- Test: `app/src/test/java/com/tsport/links/ui/MatchDayUiTest.kt`

**Interfaces:**
- Consumes: `SearchState`, `MatchDayState`.
- Produces:
  - `fun shouldShowMatchday(searchState: SearchState): Boolean`
  - `fun shouldShowMatchdayBack(searchState: SearchState, sessionExpiredMessage: String): Boolean`
  - `fun shouldFocusFirstMatch(isTelevision: Boolean, searchState: SearchState, matchState: MatchDayState, isTyping: Boolean, focusConsumed: Boolean): Boolean`
  - `fun formatMatchDate(isoDate: String): String?` (`26 Ekim 2026, Pazartesi`)

- [ ] **Step 1: Write the failing helper tests**

```kotlin
package com.tsport.links.ui

import com.tsport.links.matchday.MatchDayState
import com.tsport.links.matchday.MatchStatus
import com.tsport.links.matchday.TodayMatch
import com.tsport.links.search.SearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchDayUiTest {

    private val match = TodayMatch(
        "1", "Galatasaray", "Fenerbahçe", "21:30",
        "Rams Park", 9, null, null, MatchStatus.SCHEDULED
    )
    private val success = MatchDayState.Success("2026-10-26", listOf(match))

    @Test
    fun `matchday panel is shown only while search is idle`() {
        assertTrue(shouldShowMatchday(SearchState.Idle))
        assertFalse(shouldShowMatchday(SearchState.Searching))
        assertFalse(shouldShowMatchday(SearchState.Empty))
    }

    @Test
    fun `back link is hidden on idle and session expiry`() {
        val expired = "Oturum sona erdi, tekrar giriş yapın."
        assertFalse(shouldShowMatchdayBack(SearchState.Idle, expired))
        assertTrue(shouldShowMatchdayBack(SearchState.Searching, expired))
        assertTrue(shouldShowMatchdayBack(SearchState.Empty, expired))
        assertTrue(shouldShowMatchdayBack(SearchState.Failure("İnternet yok."), expired))
        assertFalse(shouldShowMatchdayBack(SearchState.Failure(expired), expired))
    }

    @Test
    fun `first match focus is tv only idle not typing and one shot`() {
        assertTrue(shouldFocusFirstMatch(true, SearchState.Idle, success, false, false))
        assertFalse(shouldFocusFirstMatch(false, SearchState.Idle, success, false, false))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Searching, success, false, false))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Idle, success, true, false))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Idle, success, false, true))
        assertFalse(shouldFocusFirstMatch(true, SearchState.Idle, MatchDayState.Empty, false, false))
    }

    @Test
    fun `formats turkish match date`() {
        assertEquals("26 Ekim 2026, Pazartesi", formatMatchDate("2026-10-26"))
        assertEquals(null, formatMatchDate("not-a-date"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
./gradlew testDebugUnitTest --tests "com.tsport.links.ui.MatchDayUiTest"
```

Expected: FAIL — helper'lar yok.

- [ ] **Step 3: Implement the helpers**

```kotlin
package com.tsport.links.ui

import com.tsport.links.matchday.MatchDayState
import com.tsport.links.search.SearchState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun shouldShowMatchday(searchState: SearchState): Boolean =
    searchState is SearchState.Idle

fun shouldShowMatchdayBack(
    searchState: SearchState,
    sessionExpiredMessage: String
): Boolean = when (searchState) {
    SearchState.Idle -> false
    is SearchState.Failure -> searchState.message != sessionExpiredMessage
    else -> true
}

fun shouldFocusFirstMatch(
    isTelevision: Boolean,
    searchState: SearchState,
    matchState: MatchDayState,
    isTyping: Boolean,
    focusConsumed: Boolean
): Boolean = isTelevision &&
    !focusConsumed &&
    !isTyping &&
    searchState is SearchState.Idle &&
    matchState is MatchDayState.Success &&
    matchState.matches.isNotEmpty()

fun formatMatchDate(isoDate: String): String? = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Europe/Istanbul")
        isLenient = false
    }
    val date = parser.parse(isoDate) ?: return null
    SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR")).apply {
        timeZone = TimeZone.getTimeZone("Europe/Istanbul")
    }.format(date)
}.getOrNull()
```

- [ ] **Step 4: Run tests to verify they pass**

Run the helper tests. Expected: PASS.

---

### Task 5: Maç Satırı, Futbol Simgesi ve Metinler

**Files:**
- Create: `app/src/main/java/com/tsport/links/ui/MatchAdapter.kt`
- Create: `app/src/main/res/layout/item_match.xml`
- Create: `app/src/main/res/drawable/ic_football.xml`
- Create: `app/src/main/res/drawable/bg_monogram.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/item_link.xml`
- Modify: `app/src/main/res/drawable/focus_background.xml`

**Interfaces:**
- Consumes: `TodayMatch`, `MatchStatus`.
- Produces: `MatchAdapter(onClick: (TodayMatch) -> Unit)` with `submit(List<TodayMatch>)`.
- IDs used by the activity: `matchList`, `matchSummary`, `matchdayPanel`, `matchdayTitle`, `matchDateText`, `matchLoadingGroup`, `matchProgress`, `matchLoadingText`, `matchStateContainer`, `matchStateIcon`, `matchStateTitle`, `matchStateCopy`, `matchRetryButton`, `matchdayAttribution`, `resultsPanel`, `matchdayBackButton`, `progress`, `statusText`, `linkList`.

- [ ] **Step 1: Add the Turkish strings and plural**

`strings.xml` değişiklikleri:

```xml
<string name="search_hint">Takım veya arama metni (örn. Galatasaray Fenerbahçe)</string>
<string name="option_broadcasts">broadcasts</string>
<string name="option_links_only">filter:links</string>

<string name="matchday_title">Bugünün Süper Lig maçları</string>
<string name="matchday_loading">Maçlar yükleniyor…</string>
<string name="matchday_empty_title">Bugün Süper Lig maçı yok</string>
<string name="matchday_empty_copy">Yeni maçlar açıklandığında burada görünecek.</string>
<string name="matchday_error_title">Maçlar alınamadı</string>
<string name="matchday_error_copy">İnternet bağlantısını kontrol edip yeniden deneyin.</string>
<string name="matchday_retry">Yenile</string>
<string name="matchday_attribution">Veri: TheSportsDB</string>
<string name="matchday_back">Günün maçları</string>
<string name="matchday_summary_empty">Bugün maç yok</string>
<string name="matchday_status_finished">Bitti</string>
<string name="matchday_status_live">Canlı</string>
<string name="matchday_status_postponed">Ertelendi</string>
<string name="matchday_status_cancelled">İptal</string>
<string name="matchday_round">%1$d. hafta</string>
<string name="matchday_select">%1$s - %2$s, %3$s. Maçı seç.</string>

<plurals name="matchday_summary">
    <item quantity="one">Bugün %1$d maç</item>
    <item quantity="other">Bugün %1$d maç</item>
</plurals>
```

- [ ] **Step 2: Create the football icon and monogram background**

`ic_football.xml`: 96dp × 96dp, `viewportWidth="24"`, `viewportHeight="24"`, `fillColor="@color/muted"`. Basit bir top dış çemberi + orta pentagon yeterli; ağdan görsel yüklenmez.

`bg_monogram.xml`: oval, `solid @color/surface`, 1dp `@color/accent` stroke. `item_match.xml` içindeki iki rozet bu arka planı kullanır.

`focus_background.xml`: focused stroke `2dp` → **3dp** yap. Diğer durumlar değişmez.

- [ ] **Step 3: Create the match row**

`item_match.xml` kökü:

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="8dp"
    android:background="@drawable/focus_background"
    android:clickable="true"
    android:focusable="true"
    android:foreground="?attr/selectableItemBackground"
    android:gravity="center_vertical"
    android:minHeight="@dimen/match_row_min_height"
    android:orientation="horizontal"
    android:padding="@dimen/match_row_padding">
```

İçerik:

- Sol blok, sabit `80dp`: `textTime` (gövde, bold) ve `textStatus` (ikincil, `@color/accent`, başlangıçta `gone`).
- Orta blok, `layout_weight="1"`:
  - ev sahibi satırı: `homeBadge` (monogram, `@dimen/badge_size`) + `textHome` (gövde, bold, tek satır, ellipsize);
  - deplasman satırı: `awayBadge` + `textAway`.
- Sağ blok, `layout_width="wrap_content"`: `textVenue` ve `textRound` (ikincil, tek satır, ellipsize). Boşsa `gone`.
- Tüm çocuklar `focusable="false"`; odak kökte kalır.

`MatchAdapter.kt`:

```kotlin
class MatchAdapter(
    private val onClick: (TodayMatch) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    private val items = mutableListOf<TodayMatch>()

    fun submit(newItems: List<TodayMatch>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder =
        MatchViewHolder(
            ItemMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onClick
        )

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class MatchViewHolder(
        private val binding: ItemMatchBinding,
        private val onClick: (TodayMatch) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: TodayMatch) {
            val context = binding.root.context
            val played = match.status == MatchStatus.FINISHED || match.status == MatchStatus.LIVE
            binding.textTime.text = if (played && match.scoreText != null) {
                match.scoreText
            } else {
                match.time
            }
            val status = when (match.status) {
                MatchStatus.FINISHED -> R.string.matchday_status_finished
                MatchStatus.LIVE -> R.string.matchday_status_live
                MatchStatus.POSTPONED -> R.string.matchday_status_postponed
                MatchStatus.CANCELLED -> R.string.matchday_status_cancelled
                else -> null
            }
            binding.textStatus.isVisible = status != null
            if (status != null) binding.textStatus.setText(status)

            binding.textHome.text = match.homeTeam
            binding.textAway.text = match.awayTeam
            binding.homeBadge.text = match.homeTeam.firstOrNull()?.uppercase().orEmpty()
            binding.awayBadge.text = match.awayTeam.firstOrNull()?.uppercase().orEmpty()

            binding.textVenue.isVisible = !match.venue.isNullOrBlank()
            binding.textVenue.text = match.venue.orEmpty()
            binding.textRound.isVisible = match.round != null
            binding.textRound.text = match.round?.let {
                context.getString(R.string.matchday_round, it)
            }.orEmpty()

            binding.root.contentDescription = context.getString(
                R.string.matchday_select,
                match.homeTeam,
                match.awayTeam,
                binding.textTime.text
            )
            binding.root.setOnClickListener { onClick(match) }
        }
    }
}
```

`androidx.core.view.isVisible` importunu unutma.

- [ ] **Step 4: Update existing result rows for TV**

`item_link.xml`:
- `layout_height` `wrap_content` kalır; `android:minHeight="@dimen/link_row_min_height"` ve `android:gravity="center_vertical"` ekle.
- `textNumber` boyutu `@dimen/body_text_size`, `textDomain` `@dimen/body_text_size`, `textUrl` `@dimen/secondary_text_size`.
- Kök `focusable/clickable` ve `focus_background` aynı kalır.

- [ ] **Step 5: Build to catch resource/binding errors**

Run:
```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. `item_match.xml` henüz bir layout'ta kullanılmadığı için binding yalnızca adapter üzerinden doğrulanır.

---

### Task 6: Dikey ve Yatay Yerleşimler, TV Boyutları

**Files:**
- Create: `app/src/main/res/values/dimens.xml`
- Create: `app/src/main/res/values-television/dimens.xml`
- Replace: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout-land/activity_main.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Her iki activity layout'u aynı ID'leri içerir: `scrapeWebView`, `content`, `logoutButton`, `hashtagInput`, `searchButton`, `broadcastsCheck`, `linksOnlyCheck`, `tabGroup`, `tabPopular`, `tabLatest`, `matchSummary`, `matchdayPanel`, `matchdayTitle`, `matchDateText`, `matchLoadingGroup`, `matchProgress`, `matchLoadingText`, `matchStateContainer`, `matchStateIcon`, `matchStateTitle`, `matchStateCopy`, `matchRetryButton`, `matchList`, `matchdayAttribution`, `resultsPanel`, `matchdayBackButton`, `progress`, `statusText`, `linkList`.

- [ ] **Step 1: Add dimens**

`values/dimens.xml`:

```xml
<resources>
    <dimen name="screen_padding">24dp</dimen>
    <dimen name="title_text_size">24sp</dimen>
    <dimen name="body_text_size">16sp</dimen>
    <dimen name="secondary_text_size">14sp</dimen>
    <dimen name="control_min_height">48dp</dimen>
    <dimen name="match_row_min_height">72dp</dimen>
    <dimen name="match_row_padding">12dp</dimen>
    <dimen name="link_row_min_height">56dp</dimen>
    <dimen name="badge_size">32dp</dimen>
    <dimen name="column_gap">20dp</dimen>
    <dimen name="empty_icon_size">96dp</dimen>
</resources>
```

`values-television/dimens.xml`:

```xml
<resources>
    <dimen name="screen_padding">48dp</dimen>
    <dimen name="title_text_size">28sp</dimen>
    <dimen name="body_text_size">20sp</dimen>
    <dimen name="secondary_text_size">16sp</dimen>
    <dimen name="control_min_height">56dp</dimen>
    <dimen name="match_row_min_height">88dp</dimen>
    <dimen name="match_row_padding">16dp</dimen>
    <dimen name="link_row_min_height">64dp</dimen>
    <dimen name="badge_size">40dp</dimen>
    <dimen name="column_gap">28dp</dimen>
    <dimen name="empty_icon_size">120dp</dimen>
</resources>
```

- [ ] **Step 2: Rewrite the portrait layout**

`layout/activity_main.xml` yapısı:

```text
FrameLayout
├── WebView @+id/scrapeWebView (alpha .01, focusable false)
└── LinearLayout @+id/content (vertical, padding @dimen/screen_padding)
    ├── header: TextView TSport (title_text_size, bold) + Button @+id/logoutButton
    ├── search row: EditText @+id/hashtagInput (weight 1) + Button @+id/searchButton
    ├── options row: CheckBox @+id/broadcastsCheck (weight 1) + CheckBox @+id/linksOnlyCheck (weight 1)
    ├── RadioGroup @+id/tabGroup: @+id/tabPopular + @+id/tabLatest
    ├── TextView @+id/matchSummary (secondary, gone until data)
    └── FrameLayout (height 0dp, weight 1)
        ├── LinearLayout @+id/matchdayPanel (vertical, match_parent)
        │   ├── TextView @+id/matchdayTitle
        │   ├── TextView @+id/matchDateText
        │   ├── LinearLayout @+id/matchLoadingGroup (horizontal/center)
        │   │   ├── ProgressBar @+id/matchProgress
        │   │   └── TextView @+id/matchLoadingText
        │   ├── LinearLayout @+id/matchStateContainer (vertical/center, height 0, weight 1)
        │   │   ├── ImageView @+id/matchStateIcon (ic_football, @dimen/empty_icon_size)
        │   │   ├── TextView @+id/matchStateTitle
        │   │   ├── TextView @+id/matchStateCopy
        │   │   └── Button @+id/matchRetryButton
        │   ├── RecyclerView @+id/matchList (height 0, weight 1)
        │   └── TextView @+id/matchdayAttribution
        └── LinearLayout @+id/resultsPanel (vertical, match_parent, gone)
            ├── LinearLayout header
            │   ├── TextView @+id/statusText (weight 1)
            │   └── Button @+id/matchdayBackButton (text @string/matchday_back)
            ├── ProgressBar @+id/progress
            └── RecyclerView @+id/linkList (height 0, weight 1)
```

Kurallar:

- Arama satırında `Ara` butonu telefon dikeyde yanındadır.
- Seçenekler tek satırda; `broadcastsCheck` ve `linksOnlyCheck` `layout_width="0dp"` + eşit `layout_weight`; metinler `@dimen/body_text_size`, `maxLines="1"`.
- Sekmeler hemen altında; `tabGroup` yatay.
- `matchSummary` varsayılan `gone`; `renderMatchDay` tarafından yönetilir.
- `matchLoadingGroup`, `matchStateContainer` ve `matchList` başlangıçta sırayla `visible/gone/gone`; `matchProgress` `indeterminate`; `matchLoadingText` metni `@string/matchday_loading`.
- `matchdayTitle` metni `@string/matchday_title`, `matchdayAttribution` metni `@string/matchday_attribution`, `matchRetryButton` metni `@string/matchday_retry` ve başlangıçta `gone`.
- `matchStateIcon` kaynağı `@drawable/ic_football`; `matchStateTitle` `textStyle="bold"`; `matchRetryButton` `gone` başlar.
- `matchdayBackButton` `gone` başlar; `resultsPanel` `gone`.
- `matchList`/`linkList` `clipToPadding="false"`, alt padding `@dimen/screen_padding`.
- Tüm buton, input, checkbox, radio ve satırlar `minHeight="@dimen/control_min_height"` (satırlarda kendi dimensi).
- `matchStateTitle`, `matchLoadingText` ve `statusText` için `android:accessibilityLiveRegion="polite"`.
- `android:nextFocusRight` ile soldan sağa geçiş desteklenebilir; ID'ler yukarıdaki gibi korunur.

- [ ] **Step 3: Create the landscape/TV layout**

`layout-land/activity_main.xml` yapısı:

```text
FrameLayout
├── WebView @+id/scrapeWebView
└── LinearLayout @+id/content (vertical, padding @dimen/screen_padding)
    ├── header (TSport + @+id/logoutButton)
    └── LinearLayout (horizontal, height 0, weight 1)
        ├── LinearLayout sol panel (width 0, weight 0.32, vertical)
        │   ├── @+id/hashtagInput
        │   ├── @+id/searchButton (tam genişlik, input'un altında)
        │   ├── options row (@+id/broadcastsCheck + @+id/linksOnlyCheck)
        │   ├── @+id/tabGroup (@+id/tabPopular + @+id/tabLatest)
        │   ├── @+id/matchSummary
        │   └── Space (weight 1)
        └── FrameLayout sonuç alanı (width 0, weight 0.68, marginStart @dimen/column_gap)
            ├── @+id/matchdayPanel
            └── @+id/resultsPanel
```

`matchdayPanel` ve `resultsPanel` içerikleri dikey layout ile **birebir aynı ID ve alt bileşenleri** kullanır. Sağ panelde başlık, tarih, maç listesi/sonuç listesi bulunur. TV'de `Ara` input'un altında, tam genişliktedir.

- [ ] **Step 4: Add soft-keyboard behavior**

`AndroidManifest.xml` içindeki `MainActivity` etiketine ekle:

```xml
android:windowSoftInputMode="adjustResize"
```

- [ ] **Step 5: Verify ID parity and build**

```bash
grep -o 'android:id="@+id/[A-Za-z0-9_]*"' app/src/main/res/layout/activity_main.xml | sort -u > /tmp/portrait-ids.txt
grep -o 'android:id="@+id/[A-Za-z0-9_]*"' app/src/main/res/layout-land/activity_main.xml | sort -u > /tmp/land-ids.txt
diff /tmp/portrait-ids.txt /tmp/land-ids.txt
./gradlew assembleDebug
```

Expected: `diff` çıktısı boş; `BUILD SUCCESSFUL`.

---

### Task 7: MainActivity Bağlantısı, Otomatik Arama ve Odak

**Files:**
- Modify: `app/src/main/java/com/tsport/links/ui/MainActivity.kt`

**Interfaces:**
- Consumes: Task 1–6 çıktıları, mevcut `SearchViewModel`, `SearchOptions`, `SearchQueryBuilder`.
- Produces:
  - `matchDayViewModel: MatchDayViewModel`
  - `matchAdapter: MatchAdapter`
  - `renderMatchDay(state: MatchDayState)`
  - `updatePanelVisibility()`
  - `maybeFocusFirstMatch()`
  - `startMatchSearch(match: TodayMatch)`
  - `showMatchdayAgain()`

- [ ] **Step 1: Wire the ViewModel and adapters**

`onCreate` içinde:

```kotlin
private val matchDayViewModel: MatchDayViewModel by viewModels {
    MatchDayViewModelFactory(TheSportsDbSource())
}
```

Alanlar:

```kotlin
private lateinit var matchAdapter: MatchAdapter
private var currentSearchState: SearchState = SearchState.Idle
private var currentMatchDayState: MatchDayState = MatchDayState.Loading
private var matchdayFocusConsumed = false
```

Kurulum:

```kotlin
matchAdapter = MatchAdapter(::startMatchSearch)
binding.matchList.layoutManager = LinearLayoutManager(this)
binding.matchList.adapter = matchAdapter
binding.matchRetryButton.setOnClickListener { matchDayViewModel.retry() }
binding.matchdayBackButton.setOnClickListener { showMatchdayAgain() }
```

İkinci state koleksiyonu:

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        matchDayViewModel.state.collect(::renderMatchDay)
    }
}
```

`onCreate` sonunda `matchDayViewModel.load()` çağır. `onResume` içinde, giriş kontrolünden sonra ve arama `Idle` ise `matchDayViewModel.load()` çağır; taze önbellek ağa çıkmaz.

- [ ] **Step 2: Store and merge search state**

`render(state)` başına `currentSearchState = state` ekle. Mevcut enable/progress/status/adapter davranışını koru. Sonra:

```kotlin
updatePanelVisibility()
```

`updatePanelVisibility`:

```kotlin
private fun updatePanelVisibility() {
    val showMatchday = shouldShowMatchday(currentSearchState)
    binding.matchdayPanel.isVisible = showMatchday
    binding.resultsPanel.isVisible = !showMatchday
    binding.matchdayBackButton.isVisible = shouldShowMatchdayBack(
        currentSearchState,
        getString(R.string.session_expired)
    )
    binding.matchdayAttribution.isVisible = showMatchday
    if (showMatchday) maybeFocusFirstMatch()
}
```

Arama `Success` olduğunda mevcut `binding.linkList.requestFocus()` davranışını koru; fakat odağı vermeden önce `updatePanelVisibility()` çalışmış olmalı. Arama `Searching` olduğunda TV'de `matchdayBackButton.requestFocus()` çağır. Arama `Failure` ve oturum hatası değilse mevcut `searchButton.requestFocus()` korunur.

`applySystemBarInsets` içindeki sabit `24` kaldırılır:

```kotlin
val base = resources.getDimensionPixelSize(R.dimen.screen_padding)
```

- [ ] **Step 3: Render matchday states**

```kotlin
private fun renderMatchDay(state: MatchDayState) {
    currentMatchDayState = state
    if (state is MatchDayState.Loading) {
        matchdayFocusConsumed = false
        binding.matchLoadingGroup.isVisible = true
        binding.matchStateContainer.isVisible = false
        binding.matchList.isVisible = false
        binding.matchDateText.text = ""
        binding.matchSummary.isVisible = false
        matchAdapter.submit(emptyList())
    }
    when (state) {
        MatchDayState.Loading -> Unit
        is MatchDayState.Success -> {
            binding.matchLoadingGroup.isVisible = false
            binding.matchStateContainer.isVisible = false
            binding.matchList.isVisible = true
            binding.matchDateText.text = formatMatchDate(state.date).orEmpty()
            matchAdapter.submit(state.matches)
            binding.matchSummary.text = resources.getQuantityString(
                R.plurals.matchday_summary,
                state.matches.size,
                state.matches.size
            )
            binding.matchSummary.isVisible = true
        }
        MatchDayState.Empty -> {
            binding.matchLoadingGroup.isVisible = false
            binding.matchList.isVisible = false
            binding.matchStateContainer.isVisible = true
            binding.matchStateIcon.setImageResource(R.drawable.ic_football)
            binding.matchStateTitle.setText(R.string.matchday_empty_title)
            binding.matchStateCopy.setText(R.string.matchday_empty_copy)
            binding.matchStateCopy.isVisible = true
            binding.matchRetryButton.isVisible = true
            binding.matchDateText.text = ""
            binding.matchSummary.setText(R.string.matchday_summary_empty)
            binding.matchSummary.isVisible = true
            matchAdapter.submit(emptyList())
        }
        MatchDayState.Failure -> {
            binding.matchLoadingGroup.isVisible = false
            binding.matchList.isVisible = false
            binding.matchStateContainer.isVisible = true
            binding.matchStateIcon.setImageResource(R.drawable.ic_football)
            binding.matchStateTitle.setText(R.string.matchday_error_title)
            binding.matchStateCopy.setText(R.string.matchday_error_copy)
            binding.matchStateCopy.isVisible = true
            binding.matchRetryButton.isVisible = true
            binding.matchDateText.text = ""
            binding.matchSummary.isVisible = false
            matchAdapter.submit(emptyList())
        }
    }
    updatePanelVisibility()
    maybeFocusFirstMatch()
}
```

`matchSummary`/`matchDateText`/`matchLoadingText` başlangıç metinleri `strings.xml`'den gelir. `matchdayTitle` her durumda `@string/matchday_title`.

- [ ] **Step 4: Focus, match selection and return behavior**

```kotlin
private fun maybeFocusFirstMatch() {
    val isTyping = binding.hashtagInput.hasFocus() &&
        binding.hashtagInput.text.isNotBlank()
    val shouldFocus = shouldFocusFirstMatch(
        isTelevision = isTelevision(),
        searchState = currentSearchState,
        matchState = currentMatchDayState,
        isTyping = isTyping,
        focusConsumed = matchdayFocusConsumed
    )
    if (!shouldFocus) return
    matchdayFocusConsumed = true
    binding.matchList.post {
        if (currentSearchState !is SearchState.Idle) return@post
        val layoutManager = binding.matchList.layoutManager as? LinearLayoutManager
        layoutManager?.scrollToPosition(0)
        layoutManager?.findViewByPosition(0)?.requestFocus()
    }
}

private fun startMatchSearch(match: TodayMatch) {
    binding.hashtagInput.setText(match.query)
    binding.broadcastsCheck.isChecked = true
    persistOptionSelection()
    startSearch()
}

private fun showMatchdayAgain() {
    matchdayFocusConsumed = false
    viewModel.reset()
    matchDayViewModel.load()
}
```

`renderMatchDay` `Success` sonrası odak `maybeFocusFirstMatch` ile alınır; aynı yükleme ikinci kez odak çalmaz. `showMatchdayAgain` formu temizlemez ve `broadcasts` seçimini geri almaz.

- [ ] **Step 5: Compile and run existing tests**

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected: mevcut testler + yeni testler PASS; `BUILD SUCCESSFUL`.

---

### Task 8: Tam Doğrulama ve Cihaz Testi

**Files:**
- No source changes.

**Interfaces:**
- Consumes: Task 1–7.
- Produces: test/lint/build/cihaz kanıtı; commit yok.

- [ ] **Step 1: Run the full unit suite**

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`; mevcut 58 test ve yeni testler geçer. Test sayısını raporla.

- [ ] **Step 2: Run lint and build**

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew lint assembleDebug
```

Expected: `BUILD SUCCESSFUL`; APK: `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Verify no web scraping and ID parity**

```bash
grep -R "browse_tv\|thesportsdb.com/browse" -n app/src/main || true
grep -o 'android:id="@+id/[A-Za-z0-9_]*"' app/src/main/res/layout/activity_main.xml | sort -u > /tmp/portrait-ids.txt
grep -o 'android:id="@+id/[A-Za-z0-9_]*"' app/src/main/res/layout-land/activity_main.xml | sort -u > /tmp/land-ids.txt
diff /tmp/portrait-ids.txt /tmp/land-ids.txt
```

Expected: scraping referansı yok; `diff` boş.

- [ ] **Step 4: Install and verify on the connected phone**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cihazda:

1. Uygulamayı aç; sağ panelde bugünün 3 maçı ve `Veri: TheSportsDB` görünmeli.
2. Galatasaray–Fenerbahçe satırına dokun; arama metni `Galatasaray Fenerbahçe` olmalı, `broadcasts` işaretlenmeli, arama otomatik başlamalı.
3. Arama sürerken maç paneli gizlenmeli; sonuç/boş/hata durumlarından biri görünmeli.
4. `Günün maçları` ile maç listesine dön; form ve `broadcasts` seçimi korunmalı.
5. Cihazı yataya çevir; iki sütunlu B düzeni görünmeli, taşma olmamalı.
6. Uygulamayı kapat/aç; son arama metni ve seçenekler korunmalı.
7. Ağ kapalıyken maç paneli `Yenile` göstermeli; ağ açılıp `Yenile` çalışmalı. Arama hatası maç panelini kilitlememeli.

Expected: dikeyde tek sütun, yatayda iki sütun; maç seçimi doğru sorgu ve otomatik arama; hata/boş durumlarında `Yenile`.

- [ ] **Step 5: Verify TV behavior**

Varsa Android TV emülatörü/cihazında:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

1. Uygulama açılışında 48dp güvenli alan ve büyük tipografi görünmeli.
2. Maçlar yüklendiğinde odak ilk maça gitmeli; kullanıcı arama alanında yazıyorsa odak çalınmamalı.
3. D-pad ile sol panelden sağ panele ve maç satırlarına geçilebilmeli; odak halkası görünmeli.
4. OK ile maç seçildiğinde arama başlamalı; `Günün maçları` D-pad ile seçilebilmeli.
5. Arama `Success` olduğunda odak link listesine, `Failure` olduğunda `Ara` butonuna gitmeli.
6. `Günün maçları` ile dönüldüğünde odak ilk maça gitmeli.

TV cihazı yoksa: telefon yatayını + `values-television` kaynaklarının derlendiğini doğrula; TV emülatörü kurulamadıysa bunu raporda açıkça belirt. TV kabul kriteri emülatör/cihaz kanıtı olmadan "tam doğrulandı" diye raporlanmaz.

- [ ] **Step 6: Report evidence**

Raporla:

- toplam test sayısı ve sonuç,
- `lint assembleDebug` sonucu,
- APK yolu,
- dikey/yatay cihaz gözlemleri,
- TV emülatörü/cihazı varsa D-pad gözlemleri; yoksa bu eksik,
- hiçbir commit yapılmadığı.
