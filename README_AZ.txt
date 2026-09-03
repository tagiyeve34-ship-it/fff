NEZARET V4 PREMIUM — MOCKUP-LOCKED BUILD

Bu paket əvvəlki V3 UI-nin restyle versiyası deyil. UI DESIGN_REFERENCE.png və SCREEN_SPEC_AZ.txt əsasında yenidən qurulub.

PIN: 5566
Server API: https://hesabat.site/usaq/webpanel/api/
Firebase Messaging: aktiv, google-services.json app/ qovluğundadır.
ApplicationId: com.ailenezareti.nezaretv3 — hazır Firebase qeydiyyatı ilə uyğunluq üçün. V3 quraşdırılıbsa update kimi əvəz edəcək.

GitHub build:
1. ZIP-in içindəki Nezaret-V4-PREMIUM qovluğunun MƏZMUNUNU repo root-a yüklə.
2. Actions → Build Nezaret V4 APK → Run workflow.
3. Artifact: Nezaret-V4-Premium-APK.

Yoxlamalar:
- 45 Android XML faylı parse yoxlamasından keçir.
- PIN 5566 SHA-256 ilə tətbiq kodunda hash formasında yoxlanır.
- Dashboard yalnız mövcud GPS/batareya/zəng məlumatlarından istifadə edir.
- Xəritə full-screen + draggable bottom sheet-dir.
- Calls date picker serverin from/to filtrlərinə bağlıdır.
- Zone detail və dark-mode əlavə olunub.

Qeyd: Bu mühitdə Android SDK/Gradle compiler olmadığı üçün APK lokal compile edilə bilmədi; GitHub Actions workflow paketə daxildir və compile nəticəsini orada yoxlayacaq.

=== V4 FINAL DASHBOARD + OLD MAP PRINCIPLE UPDATE ===
- Xəritə açılışında yalnız son GPS nöqtəsi göstərilir.
- Marşrut yalnız Marşrut düyməsi ilə açılır/gizlənir.
- Başlanğıc yaşıl, son nöqtə qırmızı, keçmiş GPS nöqtələri bənövşəyi göstərilir.
- GPS sıçrayışları marşrut xəttindən filtrlənir.
- Tarixçə seçilmiş gün üzrə marşrut yükləyir.
- Yenilə son GPS mövqeyini serverdən yenidən alır.
- Aşağı xəritə paneli collapsed/expanded BottomSheet kimi işləyir.
- Dashboard donut və batareya qrafiki yenidən düzəldilib.
- Calls ekranında toggle düymələri MaterialButton olaraq düzəldilib.
