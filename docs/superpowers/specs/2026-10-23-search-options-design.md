# TSport — Arama Seçenekleri (Tasarım)

Tarih: 2026-10-23
Durum: Onay bekliyor

## 1. Amaç

Kullanıcı yalnızca bir hashtag girer. İsteğe bağlı olarak aramaya `"broadcasts"` kelimesini ve/veya `filter:links` operatörünü ekler. Ayrıca "Popüler" veya "En son" sekmesini seçer.

Sonuç ne olursa olsun yalnızca X canlı yayın linkleri (`https://x.com/i/broadcasts/<id>`) listelenir.

## 2. Kullanıcı Akışı

1. Hashtag alanına tag yazılır (ör. `GsvBJK`).
2. İki seçenek isteğe bağlı işaretlenir:
   - `"broadcasts" kelimesini ekle`
   - `Sadece link içerenler (filter:links)`
3. Sekme seçilir: `Popüler` veya `En son`.
4. `Ara` ile arama başlar.
5. Sonuçlar mevcut liste ekranında, yalnızca broadcast linkleri olarak gösterilir.

Varsayılanlar: iki seçenek kapalı, sekme `En son`.

## 3. Sorgu Üretimi

| broadcasts | filter:links | Üretilen sorgu |
|---|---|---|
| kapalı | kapalı | `#tag` |
| açık | kapalı | `#tag "broadcasts"` |
| kapalı | açık | `#tag filter:links` |
| açık | açık | `#tag "broadcasts" filter:links` |

URL:

```
https://x.com/search?q=<kodlanmış sorgu>&src=typed_query
```

- URL'in sonuna `f=top` veya `f=live` **eklenmez**; kullanıcı deneyimine göre bu parametre eklendiğinde arama/linkler düzgün çalışmıyor.
- Sekme seçimi URL ile değil, sayfa yüklendikten sonra X'in kendi sekmesine tıklanarak yapılır:
  - `Popüler`: varsayılan sekme; tıklama gerekmez.
  - `En son`: sayfadaki "En son" sekmesi (`a[role="tab"][href*="f=live"]`) JS ile tıklanır; ardından sonuçların gelmesi beklenir ve kazıma başlar.
- Sekme tıklaması başarısız olursa arama varsayılan sekmede devam eder; kullanıcıya "Sekme değiştirilemedi, Popüler sonuçlar gösteriliyor." uyarısı verilir.
- Tag temizliği: baştaki/sondaki boşluklar atılır, baştaki `#` kaldırılır, iç boşluklar mevcut davranışa uygun şekilde silinir.
- Boş tag ile arama başlatılmaz; "Geçerli bir hashtag girin." gösterilir.
- `src=typed_query` eklenir; böylece URL, X arama kutusuna yazılmış gibi davranır.

## 4. Sonuç Filtresi

Mevcut davranış korunur:

- DOM kazıma birincil, GraphQL `SearchTimeline` yedeği ikincil.
- `t.co` linkleri çözülür.
- Yalnızca sonu `x.com/i/broadcasts/<id>` olan linkler tutulur.
- Tekilleştirme ve en fazla 20 link sınırı değişmez.

## 5. Arayüz

- Hashtag alanı ve `Ara` butonu mevcut yerinde kalır.
- Altına iki `CheckBox` eklenir:
  - `"broadcasts" kelimesini ekle`
  - `filter:links (sadece link içerenler)`
- Altına yatay `RadioGroup` eklenir:
  - `Popüler`
  - `En son`
- Tüm kontroller TV'de D-pad ile odaklanabilir olur.
- Arama sürerken hashtag alanı, seçenekler ve sekme devre dışı kalır.
- Son kullanılan tag, iki seçeneğin durumu ve seçilen sekme `SharedPreferences`'ta saklanır.

## 6. Teknik Değişiklikler

- `SearchQueryBuilder`
  - `enum class SearchTab { POPULAR, LATEST }`
  - `data class SearchOptions(val hashtag: String, val includeBroadcasts: Boolean, val linksOnly: Boolean, val tab: SearchTab)`
  - `fun buildSearchUrl(options: SearchOptions): String?`
- `SearchViewModel.search(options: SearchOptions, candidateSource: ...)`
- `DomLinkScraper.scrape(searchUrl, tab)`: sayfa yüklendikten sonra `En son` seçiliyse X'in sekmesine tıklar, sonuçların gelmesini bekler, sonra kazır. `Popüler` için ek işlem yapmaz.
- `MainActivity`: kontrolleri oku, `SearchOptions` üret, seçimleri kaydet; seçilen sekmeyi `DomLinkScraper`'a geçir.
- `activity_main.xml`: checkbox'lar ve RadioGroup.
- `strings.xml`: yeni etiketler ve "sekme değiştirilemedi" uyarısı.
- `SearchQueryBuilderTest`: 4 sorgu kombinasyonu, kodlama, boş tag ve iç boşluk testleri; URL'de `f=` bulunmadığı doğrulanır.
- `SearchViewModelTest`: yeni imzaya uyarlanır.
- Sekme tıklama davranışı WebView gerektirdiği için birim testi yerine manuel doğrulanır.

## 7. Kapsam Dışı

- Serbest X sorgusu / `from:`, `list:`, `since:` gibi operatörler.
- 20 link sınırının değiştirilmesi.
- Broadcast dışındaki linklerin gösterilmesi.
- Çoklu hashtag.

## 8. Riskler

- X'in sekme HTML yapısı değişebilir; "En son" tıklaması bulunamazsa arama varsayılan sekmede devam eder ve kullanıcı uyarılır.
- Sekme tıklaması SPA içi gezinme tetiklerse sayfa yeniden yüklenir; kazıma, yeni sonuçlar gelene kadar bekletilmelidir.
- X arama sonuçları zamanla ve istemciye göre değişebilir; uygulama yine ilk 20 tekil linki gösterir.
