# TSport — Serbest Metin Araması ve Daha Fazla Sonuç (Tasarım)

Tarih: 2026-10-26
Durum: Onay bekliyor

## 1. Amaç

Kullanıcı X'te manuel olarak `<etkinlik> broadcasts` gibi **serbest metin** araması yapıyor. Uygulama ise girdiyi zorla tek bir hashtag'e çeviriyor (`#etkinlik "broadcasts"`). Bu iki sorgu X'te aynı sonucu vermediği için uygulama 0-5 linkte kalıyor.

Bu değişiklik:

- Arama girdisini serbest metin olarak kullanır; boşlukları korur ve başına `#` eklemez.
- `broadcasts` ve `filter:links` seçeneklerini korur.
- Sonuç tavanını 20'den 50'ye çıkarır ve daha derin sayfalama yapar.

Sonuç filtresi değişmez: yalnızca `https://x.com/i/broadcasts/<id>` linkleri gösterilir.

## 2. Kullanıcı Akışı

1. Arama alanına serbest metin yazılır (ör. `SAMvAMD`, `SAMvAMD canlı yayın`, `#GSvFB`).
2. İsteğe bağlı olarak:
   - `broadcasts kelimesini ekle`
   - `filter:links (sadece link içerenler)`
3. Sekme seçilir: `Popüler` veya `En son` (varsayılan `En son`).
4. `Ara` ile arama başlar.
5. Liste yalnızca broadcast linklerini gösterir; en fazla 50 tekil link.

Varsayılanlar değişmez: iki seçenek kapalı, sekme `En son`.

## 3. Sorgu Üretimi

Girdi normalizasyonu:

- Baştaki/sondaki boşluklar atılır.
- Ardışık boşluklar tek boşluğa indirilir.
- `#` korunur; otomatik eklenmez veya silinmez.
- Girdi yalnızca `#` veya boşluklardan oluşuyorsa arama başlatılmaz.

Seçenekler:

- `broadcasts` açıksa ve sorguda zaten `broadcasts` kelimesi yoksa sonuna ` broadcasts` eklenir (tırnaksız).
- `filter:links` açıksa ve sorguda zaten `filter:links` yoksa sonuna ` filter:links` eklenir.
- Yinelenen kelime/operatör eklenmez (büyük/küçük harf duyarsız, kelime sınırına saygılı).

| Girdi | broadcasts | filter:links | Üretilen sorgu |
|---|---|---|---|
| `SAMvAMD` | kapalı | kapalı | `SAMvAMD` |
| `SAMvAMD` | açık | kapalı | `SAMvAMD broadcasts` |
| `SAMvAMD canlı yayın` | açık | kapalı | `SAMvAMD canlı yayın broadcasts` |
| `#GSvFB` | kapalı | açık | `#GSvFB filter:links` |
| `SAMvAMD broadcasts` | açık | kapalı | `SAMvAMD broadcasts` |

URL:

```
https://x.com/search?q=<kodlanmış sorgu>&src=typed_query
```

- URL'e `f=top` veya `f=live` eklenmez; sekme seçimi önceki tasarımdaki gibi sayfa yüklendikten sonra X'in kendi sekmesine tıklanarak yapılır.
- Sekme tıklanamazsa mevcut uyarı korunur.

## 4. Sonuç Toplama Sınırları

| Bileşen | Eski | Yeni |
|---|---|---|
| `LinkExtractor.maxLinks` | 20 | 50 |
| DOM aday tavanı (`__MAX__`) | 60 | 150 |
| DOM kaydırma turu | 10 | 15 (2 tur değişmezse erken dur) |
| GraphQL sayfası | 3 | 6 |
| Toplam arama süresi | 25 sn | 60 sn |
| DOM kazıma süresi | 25 sn | 30 sn |
| GraphQL süresi | 20 sn | 15 sn |

Aşama üst sınırları toplam 60 sn'ye sığacak şekilde seçilir: DOM 30 + GraphQL 15 + `t.co` 10 = 55 sn; 5 sn tampon. DOM script'i kendi içinde 20 sn sonra durur ve o ana kadar topladığını döndürür; Kotlin tarafındaki 30 sn son çaredir. Bir aşama zaman aşımına uğrarsa o aşama boş/eldeki sonuçla devam eder; tüm arama hata vermez.

`t.co` çözümlemesi için ayrı bir bütçe (10 sn) tanımlanır. Bütçe dolarsa çözülen broadcast linkleri korunur, çözülemeyen `t.co` adayları atılır. Böylece uzun çözümleme tüm aramayı iptal etmez; kısmi sonuç gösterilir.

## 5. Arayüz

- Arama alanı etiketi: `Etkinlik adı veya arama metni (örn. SAMvAMD)`.
- Boş durum metni: `Bir arama metni girip Ara'ya basın.`
- Sonuç yok metni: `Sonuç yok. Aramayı kontrol edip tekrar deneyin.`
- Geçersiz girdi metni: `Geçerli bir arama metni girin.`
- `broadcasts` onay kutusu ve sekme kontrolleri aynı kalır; tümü D-pad ile odaklanabilir.
- Arama sürerken kontroller devre dışı kalır.
- Son arama metni, seçenekler ve sekme `SharedPreferences`'ta saklanmaya devam eder.

## 6. Teknik Değişiklikler

- `SearchQueryBuilder`
  - `SearchOptions.hashtag` alanı `query` olarak yeniden adlandırılır.
  - `buildSearchUrl`: serbest metin normalizasyonu, `broadcasts` / `filter:links` ekleme, `%20` kodlaması.
  - `#` öneki kaldırılır.
- `LinkExtractor`
  - `maxLinks` varsayılanı 50.
  - `resolveBudgetMs` parametresi (varsayılan 15_000); bütçe aşımında kısmi sonuç.
- `ScrapeBridge` / `DomLinkScraper`: aday tavanı 150, kazıma zaman aşımı 30 sn.
- `scrape_dom.js`: kaydırma turu 15.
- `ApiFallbackScraper`: varsayılan sayfa 6, zaman aşımı 30 sn.
- `fallback_api.js`: değişiklik gerekmez; sayfa sayısı parametre olarak gelir.
- `SearchViewModel`: toplam zaman aşımı 60 sn; geçersiz girdi mesajı güncellenir.
- `MainActivity`: `query` alanı, mesajlar ve `LinkExtractor` yapılandırması.
- `activity_main.xml` / `strings.xml`: yeni metinler.
- Testler:
  - `SearchQueryBuilderTest`: serbest metin, boşluk koruma, `#` koruma, dört kombinasyon, yinelenen kelime, Türkçe karakter kodlaması, geçersiz girdi.
  - `LinkExtractorTest`: 50 tavanı ve çözümleme bütçesinde kısmi sonuç.
  - `SearchViewModelTest`: yeni alan adı ve zaman aşımı.

## 7. Kapsam Dışı

- Broadcast dışındaki linklerin gösterilmesi.
- `from:`, `list:`, `since:` gibi gelişmiş X operatörleri için özel arayüz.
- X hesap/oturum güvenliği ve giriş akışının yeniden tasarımı.
- Sunucu tarafı veri toplama veya içerik analizi (incelenen Python depoları bu kapsamda kullanılmaz).
- 50'den fazla link.

## 8. Riskler

- Serbest metin araması da X'te 0 sonuç dönebilir; uygulama garanti vermez, yalnızca manuel aramayla aynı sorguyu kurar.
- Daha derin sayfalama X hız sınırlarına (429) takılabilir; API tarafı ilk hata veren sayfada durur.
- Arama süresi 60 saniyeye çıkabilir; ilerleme göstergesi korunur.
- 50 link daha eski yayınları da içerebilir; tekilleştirme aynı yayının tekrarını engeller.
- X DOM/GraphQL yapısı değişirse kazıma yine kırılabilir; mevcut hata mesajları korunur.

## 9. Doğrulama

- Birim testleri: sorgu matrisi, kodlama, yinelenen ekleme, boş girdi, 50 tavanı, kısmi sonuç.
- `./gradlew test lint assembleDebug`.
- Cihazda manuel:
  1. `SAMvAMD` + `broadcasts` ile ara; X'teki manuel `SAMvAMD broadcasts` sonucuyla karşılaştır.
  2. 50'ye kadar link geldiğini ve listenin kaydırılabildiğini doğrula.
  3. `Popüler` / `En son` seçiminin korunduğunu doğrula.
