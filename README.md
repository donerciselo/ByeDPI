# ByeDPI Android - DPI Bypass Engine 🚀

ByeDPI Android, internet servis sağlayıcılarının uyguladığı **DPI (Deep Packet Inspection - Derin Paket İnceleme)** sansür ve kısıtlama mekanizmalarını aşmak amacıyla geliştirilmiş, Android tabanlı yerel bir paket manipülasyon aracıdır.

Bu proje, uzak bir proxy veya harici bir VPN sunucusu kullanmaz. Tüm trafiği cihaz üzerinde oluşturulan yerel bir tünel (**TUN Interface**) üzerinden geçirerek paket seviyesinde modifikasyonlar uygular. Bu sayede internet hızınızda düşüş (ping/bandwidth kaybı) yaşanmaz.

---

## 🛠️ Çalışma Mantığı ve Bypass Teknikleri

DPI cihazları, genellikle bağlantı başlangıcındaki **TLS Client Hello** paketinin içindeki **SNI (Server Name Indication)** alanını inceleyerek hangi siteye gittiğinizi tespit eder ve engeller. Bu uygulama şu yöntemlerle sansürü yanıltır:

* **TCP Segmentation (Split):** TLS ve HTTP paketlerini çok küçük parçalara (örn: 1-2 byte) bölerek gönderir. DPI sistemleri bu parçalanmış verileri birleştirip analiz edemediği için engelleme yapamaz.
* **Host Header Modification:** Standart HTTP isteklerindeki `Host: example.com` başlığını `hOsT: example.com` şeklinde manipüle ederek DPI filtrelerini devre dışı bırakır.
* **Secure DNS (DoH/DoT):** Sistem DNS'ini tamamen devre dışı bırakarak DNS sorgularını 443 portu üzerinden şifreli (**DNS over HTTPS**) olarak iletir. Servis sağlayıcı tabanlı DNS zehirlenmelerini engeller.

---

## 📱 Ekran Görüntüleri & Arayüz

Uygulama, kullanıcı deneyimini en üst seviyede tutmak için klasik ve şık bir VPN arayüzü sunar. Tek bir dokunuşla yerel tüneli başlatabilir veya durdurabilirsiniz.

---

## 🚀 Kurulum ve Geliştirme

### Gereksinimler
* Android Studio (Koala veya daha yeni bir sürüm)
* Android SDK 24 (Android 7.0 Nougat) veya üzeri
* CMake & Android NDK (C++ tabanlı paket motoru için)

### Kurulum

Yükle: Releases kısmındaki APK dosyasını telefonuna indir.

Telefona Yükle: "Yükle" seçeneğine bas.

İzin Ver: Eğer telefonun "Bilinmeyen kaynaklardan uygulama yükleme" uyarısı verirse, ayarlara yönlendirildiğinde geçici olarak izin ver.

Uygulamayı Aç: Yükleme bittiğinde uygulamayı başlat, tasarladığın frontend üzerindeki "Connect" butonuna bas.

VPN İznini Onayla: Ekrana gelecek olan standart Android VPN bağlantı talebine Tamam de.
