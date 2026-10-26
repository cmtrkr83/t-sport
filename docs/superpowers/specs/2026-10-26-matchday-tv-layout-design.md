# TSport — TV Odaklı Maç Günü Paneli (Tasarım)

Tarih: 2026-10-26
Durum: Onaylandı

## 1. Amaç

Uygulama ilk açıldığında sağdaki sonuç alanı boş görünüyor. Bu alan, arama yapılmadan önce **bugünün Süper Lig maçlarını** gösterecek. Böylece TV'de uzaktan kumandayla açan kullanıcı boş bir ekran yerine başlangıç noktası görür ve bir maçı seçerek doğrudan X'te yayın araması başlatabilir.

Bu değişiklik aynı zamanda ekranı Android TV önceliğiyle yeniden düzenler; telefonlarda da dikey ve yatay olarak kullanılabilir kalır.

## 2. Onaylanan kararlar

- Yerleşim **B**: solda arama/seçenek paneli, sağda maç veya sonuç listesi. Telefon dikeyde bu düzen alt alta açılır.
- İlk açılışta sağ panelde **günün Süper Lig maçları** gösterilir.
- Bugün maç yoksa **futbol topu** ve kısa bir "bugün maç yok" mesajı gösterilir.
- TV'de bir maç satırına OK'lanınca arama metni iki takım adıyla doldurulur, `broadcasts` işaretlenir ve arama otomatik başlar. Telefonda aynı davranış dokunmayla çalışır.
- `broadcasts` ve `filter:links` aynı satırda; `Popüler` / `En son` hemen altındaki satırda. Etiketler tek satıra sığması için kısaltılır: `broadcasts`, `filter:links`.
- TV sayfası kazınmayacak; yalnızca resmi ücretsiz fikstür API'si kullanılacak.

## 3. Kullanıcı akışı

1. Kullanıcı giriş yapmış olarak uygulamayı açar.
2. Sol panelde arama alanı, `Ara`, iki seçenek ve iki sekme görünür.
3. Sağ panelde önce `Maçlar yükleniyor…`, sonra:
   - bugün maç varsa tarih başlığı + maç satırları,
   - bugün maç yoksa futbol topu + "Bugün Süper Lig maçı yok",
   - veri alınamadıysa futbol topu + "Maçlar alınamadı" + `Yenile`.
4. TV'de maçlar yüklendiğinde odak ilk maç satırına gider; ancak yalnızca sağ panel gerçekten maç listesini gösteriyorsa (arama durumu `Idle`) ve kullanıcı o sırada arama alanında yazmıyorsa. Bu odak yükleme başına bir kez uygulanır; arama sürerken veya sonuç ekranındayken odak çalınmaz. Maç yoksa/hata varsa odak arama alanında kalır.
5. Kullanıcı bir maçı seçer:
   - arama alanına `Ev Sahibi Deplasman` yazılır (ör. `Galatasaray Fenerbahçe`),
   - `broadcasts` işaretlenir ve tercih kaydedilir,
   - arama başlar; sağ panel sonuç alanına döner.
6. Arama sürerken maç listesi gizlenir; sonuç, boş sonuç veya hata durumu gösterilir.
7. Arama sonrası sağ üstte `Günün maçları` bağlantısı görünür. `Searching`, `Success`, `Empty` ve `Failure` durumlarında görünür; oturum süresi/giriş yönlendirmesinde görünmez. Seçilirse süren arama iptal edilir ve maç paneli gösterilir. Arama metni ve seçenekler olduğu gibi kalır; maç seçiminin yaptığı `broadcasts` değişikliği geri alınmaz. Önbellek eskiyse önce mevcut maçlar gösterilir, yenileme arka planda yapılır.

## 4. Yerleşim

### 4.1 TV / yatay

- Üst başlık: solda `TSport`, sağda `Çıkış`.
- Gövde iki sütun:
  - sol panel yaklaşık **%32** genişlik: arama alanı, `Ara`, seçenekler, sekmeler ve `Bugün 3 maç` gibi tek satırlık bir özet. Maç listesinin ikinci bir kopyası burada yer almaz.
  - sağ panel kalan genişlik: başlık, tarih, maç/sonuç listesi.
- Güvenli alan TV'de **48dp**, telefonda **24dp**. Sistem çubukları ayrıca eklenir.
- TV tipografisi: başlık 28sp, gövde 20sp, ikincil metin 16sp; kontrol yüksekliği en az 56dp.
- Telefon tipografisi: başlık 24sp, gövde 16sp, ikincil metin 14sp; dokunma hedefi en az 48dp.
- Maç satırı: saat, ev sahibi, deplasman, stadyum/hafta; varsa skor. Odaklı satırda 3dp camgöbeği çerçeve.
- Sol paneldeki seçenekler tek satırda; sekmeler hemen altında. `Ara` TV'de arama alanının altında, telefonda yanında.

### 4.2 Telefon / dikey

- Üstte başlık ve `Çıkış`.
- Arama alanı + `Ara` yan yana.
- `broadcasts` ve `filter:links` aynı satırda; `Popüler` / `En son` hemen altında.
- Altında maç paneli: başlık, tarih, maç kartları veya futbol topu durumu.
- Klavye açıldığında maç paneli aşağıda kalır; arama başlayınca sonuçlara geçilir.

## 5. Maç günü verisi

### 5.1 Kaynak

- TheSportsDB ücretsiz API, test anahtarı `123`.
- Günlük Süper Lig fikstürü:
  `https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=YYYY-MM-DD&l=4339`
- Lig kimliği `4339` = Turkish Super Lig. Yanıt ayrıca `idLeague` ile doğrulanır.
- Tarih **Europe/Istanbul** saat dilimine göre üretilir; böylece cihaz başka saat diliminde olsa da "bugün" Türkiye'ye göre seçilir.

### 5.2 Ayrıştırma

Kullanılan alanlar:

| Alan | Kullanım |
|---|---|
| `idEvent` | Tekil anahtar |
| `strHomeTeam`, `strAwayTeam` | Takım adları |
| `strEvent` | Takım adları eksikse yedek gösterim / arama metni |
| `strTimeLocal` | `HH:mm` saat |
| `dateEventLocal` | Teşhis/gece maçı bilgisi; katı filtre değil |
| `strVenue` | Stadyum |
| `intRound` | Hafta |
| `intHomeScore`, `intAwayScore` | Varsa skor |
| `strStatus` | `NS`, `FT`, canlı durumlar |

Kurallar:

- Yalnızca `idLeague == "4339"` olan kayıtlar alınır.
- Zorunlu alanlar: `idEvent`, ev sahibi ve deplasman. Takım adları `strHomeTeam` / `strAwayTeam` alanlarından alınır; biri eksikse `strEvent` içindeki `A vs B` biçiminden türetilir. İki kaynaktan da tamamlanamazsa kayıt atlanır; yarım maç satırı gösterilmez.
- İsteğe bağlı alanlar: saat, stadyum, hafta, skor, durum. Saat yoksa `--:--` gösterilir; maç yine listelenir.
- Saat `strTimeLocal` ilk 5 karakterinden alınır; yoksa `strTime`.
- Arama metni normalde `"$homeTeam $awayTeam"` (ör. `Galatasaray Fenerbahçe`). Takım adlarından biri yoksa `strEvent` kullanılır ve `" vs "` boşluğa çevrilir. Normal durumda `strEvent` içindeki `vs` aramaya dahil edilmez.
- Maçlar saate göre artan sıralanır.
- Skor varsa saat yerine `2 - 1` gösterilir; durum etiketi eklenir. Skor yoksa saat gösterilir.
- Durum eşlemesi:
  - `NS`: saat gösterilir.
  - `FT`: `Bitti` + skor.
  - `1H`, `HT`, `2H`, `ET`, `P`, `BT`: `Canlı` + skor.
  - `POST`, `PST` veya `strPostponed == "yes"`: `Ertelendi`, saat gösterilir.
  - `CANC`, `ABD`: `İptal`, saat gösterilir.
  - Bilinmeyen durum: iki skor da varsa skor, yoksa saat gösterilir.
- `dateEventLocal` katı bir filtre değildir; istek zaten `d=<tarih>&l=4339` ile gün bazlıdır. Gece maçlarında alan farklı gün gösterebilir; bu yüzden yalnızca `idLeague` ve istek tarihi esas alınır.
- Takım rozetleri için ağdan görsel yüklenmez; takım adının ilk harfiyle basit yuvarlak monogram kullanılır. Bu, yeni görsel yükleme bağımlılığı eklememek için bilinçli bir tercihtir.

### 5.3 Önbellek ve hata

- Başarılı yanıt bellekte **15 dakika** tutulur.
- İlk açılışta önbellek taze ise ağa çıkmadan gösterilir.
- `onCreate` sonrası ilk yükleme yapılır. `onResume` sırasında maç paneli görünüyorsa ve önbellek 15 dakikadan eskiyse önce mevcut maçlar gösterilir, yenileme arka planda yapılır. Europe/Istanbul tarihi değişmişse önbellek geçersiz sayılır ve zorunlu yenileme yapılır.
- Ağ isteği 10 sn zaman aşımına sahiptir; hata aramayı engellemez.
- Başarılı ama boş yanıt `Empty`; ağ/ayrıştırma hatası `Failure` durumudur.
- `Empty` ve `Failure` durumlarında `Yenile` butonu görünür; aynı günü yeniden çeker. `Success` durumunda ayrı yenile butonu yoktur.
- Kaynak atfı sağ panelin altında küçük metinle gösterilir: `Veri: TheSportsDB`.

## 6. TV sayfası değerlendirmesi

Kullanıcı `https://www.thesportsdb.com/browse_tv?c=turkey` adresini önerdi. Bulgular:

- Sayfa ücretsiz görüntülenebiliyor ve bugünün TV karşılaşmalarını gösteriyor.
- TheSportsDB kullanım şartları açıkça **"web sitemizi kazımayın"** diyor; yalnızca resmi API uç noktalarından veri alınmasına izin veriyor.
- Ücretsiz test anahtarıyla `eventstv.php` TV takvimi boş dönüyor; bu veri pratikte ücretli aboneliğe bağlı.
- Bu nedenle TV sayfası kazınmayacak ve TV kanalı bilgisi gösterilmeyecek. Maç listesi fikstür API'sinden gelecek.
- İleride ücretli API alınırsa kanal bilgisi aynı maç satırına eklenebilir; bu tasarımın kapsamı dışındadır.

## 7. Teknik tasarım

Yeni parçalar:

- `com.tsport.links.matchday.TodayMatch`
  - `id`, `homeTeam`, `awayTeam`, `time`, `venue`, `round`, `homeScore`, `awayScore`, `status`, `query`
- `MatchDayParser`
  - Saf fonksiyon: JSON metni -> `List<TodayMatch>`.
  - Lig filtresi, saat/skor ayrıştırma, sıralama burada; birim testleri kolay.
- `MatchDaySource` (arayüz) ve `TheSportsDbSource`
  - `suspend fun loadToday(): Result<List<TodayMatch>>`
  - OkHttp ile HTTPS istek; 10 sn zaman aşımı.
- `MatchDayViewModel`
  - `StateFlow<MatchDayState>`: `Loading`, `Success(list)`, `Empty`, `Failure`.
  - `load(force: Boolean = false)`, `retry()`.
  - 15 dk bellek önbelleği; `viewModelScope`.
- `MatchAdapter` + `item_match.xml`
  - Satır tıklaması `TodayMatch` döner.
  - TV odak çerçevesi `focus_background`/`button_focus_foreground` ile.
- `MainActivity`
  - İkinci ViewModel: `MatchDayViewModel`.
  - `renderMatchDay(state)` ve mevcut `renderSearch(state)`.
  - `startMatchSearch(match)`: metni doldur, `broadcastsCheck.isChecked = true`, `startSearch()`.
  - Arama sonrası `Günün maçları` bağlantısı `viewModel.reset()` + maç panelini gösterir.
  - TV'de ilk maç yüklendiğinde `matchList`/ilk satır odaklanır.
- Yerleşim dosyaları:
  - `res/layout/activity_main.xml`: telefon dikey akış.
  - `res/layout-land/activity_main.xml`: TV ve telefon yatay iki sütun.
  - `res/layout/item_match.xml`.
  - `res/values/dimens.xml` ve `res/values-television/dimens.xml`.
  - `res/drawable/ic_football.xml` (vektör).
  - Ortak bölümler için `include` kullanılabilir; iki yerleşimde de aynı ID'ler korunur.
- Metinler `strings.xml`:
  - `matchday_title`: `Bugünün Süper Lig maçları`
  - `matchday_loading`: `Maçlar yükleniyor…`
  - `matchday_empty_title`: `Bugün Süper Lig maçı yok`
  - `matchday_empty_copy`: `Yeni maçlar açıklandığında burada görünecek.`
  - `matchday_error_title`: `Maçlar alınamadı`
  - `matchday_retry`: `Yenile`
  - `matchday_select`: `Maçı seç`
  - `matchday_attribution`: `Veri: TheSportsDB`
  - `matchday_back`: `Günün maçları`
- Yeni bağımlılık yok; mevcut OkHttp ve Android `org.json` kullanılır.

## 8. TV erişilebilirliği ve odak

- Tüm arama, seçenek, sekme, maç ve `Yenile` kontrolleri D-pad ile odaklanabilir.
- Odak halkası 3dp camgöbeği; seçili radyo ile odaklı radyo farklı görünür.
- TV'de başlangıç odağı: maç paneli görünür ve `Success` ise ilk maç; aksi halde arama alanı. Odak yalnızca yükleme başına bir kez ve arama durumu `Idle` iken uygulanır; kullanıcı yazıyorsa çalınmaz.
- Arama başarılıysa mevcut davranış korunur: bağlantı listesi odaklanır.
- Arama hatasında `Ara` odaklanır.
- `Günün maçları` ile dönüldüğünde maç paneli `Success` ise ilk maç odaklanır; önbellek yenilemesi odağı çalmaz.
- Sol panelden sağ panele yatay geçişte odak maç/sonuç listesine; geri dönüşte sol panele gider. Gerekirse `nextFocusRight` / `nextFocusLeft` ile desteklenir.
- Telefonda otomatik odak/klavye açılmaz.
- Yükleniyor, boş ve hata durumları ekran okuyucuya metinle duyurulur.

## 9. Testler

Birim testleri:

- `MatchDayParserTest`
  - 3 maçlı gerçek örnek; yalnızca 4339.
  - Saat/skor/venue/round ayrıştırma.
  - Boş `events`, `null`, bozuk JSON.
  - `idEvent` eksikse kaydı atlama; takım adları eksikse `strEvent` yedeğinden türetme, yine eksikse atlama; opsiyonel alan eksikse `--:--` ile gösterme.
  - Durum eşlemesi: `NS`, `FT`, canlı, `POST`, `CANC`, bilinmeyen.
  - Saate göre sıralama.
  - `dateEventLocal` farklı olsa da istek tarihine göre kaydı koruma.
- `MatchDayViewModelTest`
  - Loading -> Success.
  - Boş liste -> Empty.
  - Hata -> Failure; `retry()` tekrar dener.
  - 15 dk önbellek: ikinci `load()` ağa çıkmaz; `force` çıkar.
  - Tarih değişince önbellek geçersizleşir.
- `MatchQueryTest`
  - `Galatasaray Fenerbahçe` + `broadcasts` üretimi; mevcut `SearchQueryBuilder` ile uyum.
  - Takım adı eksikse `strEvent` yedeği ve `vs` temizliği.
- Mevcut 58 test bozulmadan geçer.

Manuel doğrulama:

- Telefon dikey: maçlar görünür, seçenekler taşmaz, maç seçimi aramayı başlatır.
- Telefon yatay: B düzeni iki sütun.
- Android TV (emülatör veya cihaz): 48dp güvenli alan, D-pad odak sırası, ilk maç odağı, maç OK -> arama.
- Bugün maç yok senaryosu (tarih enjekte edilerek) futbol topu.
- Ağ kapalı: hata + `Yenile`; arama yine çalışır.
- `lint assembleDebug` ve `adb install` ile cihazda doğrulama.

## 10. Kapsam dışı

- TV kanalı/kanal listesi (ücretsiz API'de yok; site kazınmayacak).
- Canlı skor güncellemesi, dakika dakika takip.
- Süper Lig dışındaki ligler.
- Maç detayı, kadro, istatistik.
- Maç listesinden otomatik yayın linki açma (önce arama yapılır).
- Uygulama mağazası yayını. TheSportsDB ücretsiz API şartları, mağaza yayını için ücretli abonelik gerektiriyor; bu yerel geliştirme sürümü için geçerli değil.
- Gezinme çubuğu/çoklu ekran; tek ekran korunur.

## 11. Riskler

- TheSportsDB ücretsiz anahtarı hız sınırına takılabilir veya kapanabilir; hata durumu ve `Yenile` ile karşılanır.
- API verisi eksik/gecikmeli olabilir; uygulama maç garantisi vermez.
- TV ve telefon için iki yerleşim bakım yükü getirir; ID'ler aynı tutulmalı ve iki yönelimde test edilmeli.
- `eventsday.php?l=4339` bugün 3 maç döndürdü; TV sayfasında yalnızca 1 Süper Lig maçı görünüyordu. Fikstür API'si esas alınır, TV sayfası veri kaynağı değildir.
- Maç seçiminde `broadcasts` otomatik işaretlenir; bu, kullanıcının önceki tercihini değiştirir. Checkbox görünür olduğu için kullanıcı fark edebilir; arama öncesi değiştirebilir.

## 12. Yaklaşımlar ve seçim

- **Yerleşim A:** tek satır seçenek çubuğu + tam genişlik liste. Daha basit, ama TV'de kontrol paneli zayıf.
- **Yerleşim B (seçildi):** sol kontrol paneli + sağ liste. TV'de daha okunaklı ve odak yönetimi daha net; linkler için daha az yatay alan.
- **Veri kaynağı 1:** TV sayfasını kazımak. Reddedildi; kullanım şartları yasaklıyor ve kırılgan.
- **Veri kaynağı 2:** ücretli TV takvimi API'si. Reddedildi; ücretsiz değil.
- **Veri kaynağı 3 (seçildi):** ücretsiz fikstür API'si; kanal bilgisi yok.
- **Liste uygulaması:** maçlar için ayrı `RecyclerView` + `MatchAdapter`; mevcut `linkList` korunur. Tek adaptere iki tür koymaktan daha yalıtılmış ve test edilebilir.
- **Yerleşim uygulaması:** `layout/` ve `layout-land/` varyantları + TV dimens. Tek layout'u programatik olarak yeniden düzenlemekten daha okunaklı.

## 13. Builder'a bırakılanlar

- `include` ile ortak panel kullanımı veya iki layout'ta açık tekrar; aynı ID'ler korunmak şartıyla.
- Monogram rozetin tam görsel detayı.
- 15 dk önbellek süresinin sabit olarak kodlanması.
- `Yenile` butonunun tam yerleşimi.
- D-pad `nextFocus` ayarlarının cihazda test edilerek netleştirilmesi.
- `Günün maçları` bağlantısının sağ üstte mi, sol panelde mi duracağı.

## 14. Kabul kriterleri

- İlk açılışta sağ panel boş değil; bugünün Süper Lig maçları veya futbol topu durumu görünür.
- Bugünkü gerçek veriyle 3 maç listelenir (tarih 2026-10-26).
- TV'de 48dp güvenli alan, okunaklı tipografi ve görünür D-pad odağı vardır.
- Telefon dikeyde kontrol satırları taşmaz; yatayda B düzeni görünür.
- Maç seçimi arama metnini doldurur, `broadcasts` işaretler ve aramayı başlatır.
- Maç yoksa futbol topu ve doğru metin görünür; hata durumunda `Yenile` çalışır.
- Mevcut arama davranışı, 50 link sınırı ve 58 test korunur; yeni testler geçer.
- TV sayfası kazınmaz; yalnızca resmi API kullanılır.
