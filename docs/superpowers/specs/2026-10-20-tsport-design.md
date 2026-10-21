# TSport — X Hashtag Link Bulucu (Tasarım)

Tarih: 2026-10-20
Durum: Onaylandı (kullanıcı sözlü onayı)

## 1. Amaç

Android telefon ve Android TV'de çalışan tek APK'lık minimalist uygulama:

1. Kullanıcı, uygulama içi WebView'de kendi X (Twitter) hesabıyla giriş yapar.
2. Hashtag girer (örn. `GsvBJK`).
3. Uygulama X'te `#GsvBJK "broadcasts" filter:links` sorgusunu **En son** sekmesinde çalıştırır.
4. Sonuç tweet'lerindeki dış linkleri çıkarır, tekilleştirir ve en fazla **20** linki listeler.
5. Linke tıklanınca varsayılan tarayıcıda açılır.

Başarı ölçütü: Gerçek hesapla giriş yapıldıktan sonra bir hashtag araması 25 saniye içinde en az bir sonuç döndürüyorsa ve linkler tarayıcıda açılıyorsa uygulama başarılıdır.

Kapsam dışı: Tweet beğenme/retweet, mesajlaşma, geçmiş/favoriler, bildirimler, içerik indirme, yayın oynatma.

## 2. Kısıtlar ve Varsayımlar

- Giriş zorunludur; X anonim aramayı kısıtlar.
- Şifre saklanmaz; yalnızca WebView çerezleri kalıcıdır.
- Hashtag her aramada elle girilir; son değer hatırlanır.
- t.co kısaltmaları gerçek hedefe çözülür.
- X DOM'u ve dahili API'leri zamanla değişebilir; uygulama bu değişikliklere karşı kırılgan olabilir.
- X içeriğini kazımak X kullanım şartlarına aykırı olabilir; uygulama kişisel kullanım içindir.
- Gerçek X girişi/kazıma testi kullanıcı hesabıyla cihazda yapılır.

## 3. Mimari

- Dil/teknoloji: Kotlin, klasik Android Views (XML), RecyclerView, Coroutines, OkHttp.
- minSdk 24, compileSdk/targetSdk 35, paket `com.tsport.links`, uygulama adı `TSport`.
- İki Activity: giriş noktası `MainActivity` (telefon `LAUNCHER` + TV `LEANBACK_LAUNCHER`); oturum yoksa `LoginActivity`'ye yönlendirir.
- WebView ayarları: JavaScript, DOM storage, çerezler açık; `; wv` içermeyen Chrome Android user-agent; mixed content kapalı. Gizli kazıma WebView'inde yalnızca `x.com`/`twitter.com`; login WebView'inde ayrıca Google/Apple OAuth alan adları.
- Arama WebView'i `MainActivity` içinde 1dp boyutunda, saydam ve tıklanamaz şekilde pencereye bağlı tutulur (JS çalışması için görünür/attach durumda).

### Bileşenler

| Bileşen | Sorumluluk |
|---|---|
| `AuthRepository` | `CookieManager` üzerinden `auth_token` varlığını kontrol eder, çıkışta çerezleri temizler. |
| `SearchQueryBuilder` | Hashtag'i temizler, arama URL'ini üretir. |
| `DomLinkScraper` | Arama sayfasını yükler, JS enjekte eder, kaydırır, link adaylarını toplar. |
| `ApiFallbackScraper` | DOM yetersizse sayfanın GraphQL `SearchTimeline` isteğini çalışma anında yakalayıp tekrar oynatır. |
| `LinkExtractor` | Aday URL'leri filtreler, normalize eder, t.co çözer, tekilleştirir, 20 ile sınırlar. |
| `TcoResolver` | OkHttp ile yönlendirmeleri takip ederek nihai URL'i bulur. |
| `SearchViewModel` | Durum makinesi: Idle / Searching / Success / Empty / Error; aramayı iptal edilebilir yapar. |
| `LinkAdapter` | Numaralı link satırları, D-pad odağı, tıklama. |

### Paket yapısı

```
com.tsport.links
├── auth/        AuthRepository, LoginActivity
├── search/      SearchQueryBuilder, DomLinkScraper, ApiFallbackScraper, SearchViewModel
├── links/       LinkExtractor, TcoResolver, LinkItem
├── ui/          MainActivity, LinkAdapter
└── util/        UrlUtils
```

## 4. Kullanıcı Akışı

1. Açılış: `auth_token` çerezi yoksa `LoginActivity`; varsa `MainActivity`.
2. Login: tam ekran WebView `https://x.com/login`; giriş sonrası çerez kontrol edilir ve ana ekrana geçilir. Üstte "X hesabınızla giriş yapın" bilgisi ve yükleniyor çubuğu.
3. Ana ekran: hashtag alanı (son kullanılanla dolu), `Ara`, `Çıkış`; durum satırı; numaralı link listesi.
4. Ara: buton devre dışı, "Aranıyor..." gösterilir; sonuçta "N link bulundu" veya hata/boş mesajı.
5. Bir linke tıklama/OK: `Intent.ACTION_VIEW` ile varsayılan tarayıcı.
6. Yenile: aynı hashtag ile aramayı baştan çalıştırır.

## 5. Arama ve Link Çıkarma

### 5.1 Sorgu

```
https://x.com/search?q=%23<hashtag>%20%22broadcasts%22%20filter%3Alinks&f=live
```

Hashtag'deki `#`, boşluk ve URL'e özel karakterler temizlenir/kodlanır. Boş hashtag ile arama başlatılmaz.

### 5.2 DOM kazıma (birincil)

1. WebView arama URL'ini yükler; `onPageFinished` sonrası 2 sn beklenir.
2. JS enjekte edilir:
   - `article[data-testid="tweet"]` kartları seçilir.
   - Her kartta `div[data-testid="tweetText"] a[href]` ve `div[data-testid="card.wrapper"] a[href]` adayları toplanır; tweet sahibinin adı `div[data-testid="User-Name"]` içinden alınır.
   - Yalnızca `x.com`/`twitter.com` üzerindeki `/i/broadcasts/<kod>` adresleri ve `t.co` bağlantıları toplanır; diğer X ve twimg adresleri elenir.
   - Sonuç JSON olarak `@JavascriptInterface` ile uygulamaya döner.
3. Yetersiz link varsa JS ile sayfa sonuna kaydırılır; 1,5 sn beklenir; yeni tweet sayısına bakılır.
4. Döngü: 20 tekil link, 10 kaydırma veya 25 sn toplam süre sınırı.
5. Adaylar `LinkExtractor`'a verilir; t.co çözülür; yalnızca `https://x.com/i/broadcasts/<kod>` biçimindeki nihai URL'ler kanonikleştirilip tekilleştirilir; ilk 20 tutulur.

### 5.3 API yedeği (ikincil)

- Sayfa yüklendikten sonra `performance.getEntriesByType('resource')` ile `SearchTimeline` isteğinin tam URL'i (query ID, `variables`, `features`) yakalanır. Buffer boyutu JS ile artırılır.
- DOM 20 linki çıkaramazsa yakalanan istek, sayfa bağlamında `fetch` ile tekrar oynatılır:
  - `variables` JSON'u çözülür; `count=40`, mevcut `cursor` ayarlanır.
  - `x-csrf-token` çerezden (`ct0`) okunur; `authorization` için X'in herkese açık web bearer sabiti kullanılır.
  - Yanıttaki `tweet_results` içinden `legacy.entities.urls[].expanded_url`, `note_tweet...entity_set.urls` ve link kartı URL'leri, tweet sahibinin adıyla birlikte alınır (`core.user_results.result.legacy.name`, yoksa `@screen_name`).
  - Cursor ile en fazla 3 sayfa istenir; 20 linkte durulur.
- Query ID/features koda gömülmez, çalışma anında yakalanır. Bearer sabiti X tarafından değiştirilirse bu yol çalışmaz; DOM yolu birincil kalır.

### 5.4 t.co çözümleme

- `https://t.co/...` adayları OkHttp `HEAD` (gerekirse `GET`) ile en fazla 5 yönlendirme takip edilerek çözülür.
- Zaman aşımı 8 sn; başarısızsa aday elenir. X alan adına ulaşılınca yönlendirme durdurulur; böylece login yönlendirmesi yayın linkini kaybettirmez.
- Aynı nihai URL birden fazla kez görünürse yalnızca ilki listelenir (fragment atılır).

## 6. Arayüz

- Koyu tema (`Theme.Material3.DayNight.NoActionBar` tabanlı özel koyu palet), büyük tipografi.
- Login: WebView + üst bilgi + ilerleme çubuğu.
- Ana ekran:
  - Başlık: "TSport Link Bulucu".
  - Hashtag `EditText` (hint: "Hashtag (örn. GsvBJK)") + `Ara`.
  - Durum satırı.
  - `RecyclerView`: numara, tweet sahibi adı (yoksa `x.com`) büyük başlık, tam URL (küçük, tek satır, sonu üç nokta).
  - Boş durum: "Sonuç yok. Hashtag'i kontrol edip Yenile'ye bas."
  - Hata durumu: Türkçe mesaj + `Yenile`.
- TV: `LEANBACK_LAUNCHER`, `android.software.leanback` gerekli değil, dokunmatik gerekli değil; 320×180 banner. Tüm kontroller `focusable`; odaklanınca belirgin çerçeve/renk; D-pad yukarı/aşağı liste, OK açar. Metin girişi sistem klavyesiyle.
- Telefon: aynı ekranlar, dikey; genişlikte maksimum içerik genişliği.

## 7. Hata ve Kenar Durumları

| Durum | Davranış |
|---|---|
| Oturum yok/expired | Login ekranına yönlendir, "Oturum sona erdi, tekrar giriş yapın." |
| Ağ yok | "İnternet bağlantısı yok." + Yenile |
| 25 sn aşımı | "Arama zaman aşımına uğradı." + Yenile |
| 0 sonuç | Boş durum mesajı |
| X WebView engeli/bozuk sayfa | "X sayfası yüklenemedi." + Yenile |
| t.co çözülemedi | t.co URL'i gösterilir |
| Aynı anda yeni arama | Önceki iş iptal edilir |
| WebView gezinmesi | Gizli WebView yalnızca x.com/twitter.com; login WebView ayrıca Google/Apple OAuth; dış linkler tarayıcıya |

## 8. Güvenlik ve Gizlilik

- Kullanıcı adı/şifre uygulama kodu tarafından okunmaz, saklanmaz, loglanmaz.
- Çerezler yalnızca WebView'in kendi deposunda tutulur; dışa aktarılmaz.
- Cleartext trafik kapalı.
- Loglarda URL veya çerez basılmaz.
- Çıkış, X çerezlerini temizler.

## 9. Build ve Dağıtım

- Gradle wrapper 8.7, AGP 8.5.2, Kotlin 1.9.24, JDK 17 (Android Studio JBR).
- Bağımlılıklar: appcompat, material, recyclerview, core-ktx, lifecycle-viewmodel-ktx, coroutines-android, okhttp; testlerde junit + mockwebserver.
- `assembleDebug` ile imzalı (debug keystore) kurulabilir APK: `app/build/outputs/apk/debug/app-debug.apk`.
- İkon: basit vektör link/oynatma simgesi; TV banner 320×180.
- Release imzalama bu kapsamda değil; istenirse sonradan eklenir.

## 10. Test Planı

Birim testleri:
- `SearchQueryBuilder`: hashtag temizleme, `#`, boşluk, Türkçe karakter ve özel karakter kodlaması; doğru URL.
- `LinkExtractor`: iç/medya/göreli linklerin elenmesi, http(s) dışı şemaların reddi, fragment temizliği, tekilleştirme, 20 sınırı.
- `TcoResolver`: MockWebServer ile 301/302 zinciri, döngü ve zaman aşımı davranışı.
- `FallbackJsonParser`: örnek SearchTimeline JSON'undan link çıkarma.

Derleme/kalite:
- `./gradlew test lint assembleDebug` hatasız.
- APK'nın `aapt dump badging` ile paket adı, minSdk/targetSdk ve leanback özellikleri doğrulanır.

Manuel (kullanıcı):
- Telefon ve TV'de kurulum, X girişi, gerçek hashtag araması, 20 link sınırı, linkin varsayılan tarayıcıda açılması, D-pad ile gezinme.
- X arayüz değişikliği nedeniyle 0 sonuç alınırsa seçicilerin güncellenmesi gerekir.

## 11. Riskler

- X, WebView oturumlarını veya otomasyonu engelleyebilir.
- DOM `data-testid` değerleri değişebilir.
- GraphQL bearer/query parametreleri değişebilir; yedek yol kalıcı değildir.
- TV'de WebView'e uzaktan kumandayla metin girişi zahmetli olabilir.
- X kullanım şartları ihlali riski kullanıcıya aittir.

## 12. Kapsam Dışı (YAGNI)

- Çoklu hashtag, filtre editörü, arama geçmişi, favoriler.
- Arka planda periyodik arama/bildirim.
- İçerik oynatıcı veya indirme.
- Hesap yönetimi/çoklu hesap.
