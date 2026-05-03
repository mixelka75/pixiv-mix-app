# Разработка

<p><a href="DEVELOPMENT.md">English</a> · <strong>Русский</strong></p>

Всё, что нужно чтобы собрать PixMix из исходников, выпустить релиз, поднять прокси и сориентироваться в коде.

---

## Тулчейн

- **JDK 17** (Temurin или OpenJDK). `JAVA_HOME` ставится либо на сессию:
  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
  ```
  либо постоянно через `~/.gradle/gradle.properties`:
  ```
  org.gradle.java.home=/usr/lib/jvm/java-17-openjdk
  ```
- **Android SDK** (только для Android-сборки): `platforms;android-34`, `build-tools;34.0.0`. Переменные:
  ```bash
  export ANDROID_HOME=$HOME/Android/Sdk
  export PATH=$ANDROID_HOME/platform-tools:$PATH
  ```
  `local.properties` хранит `sdk.dir` и в git не коммитится.
- **Закреплённые версии**: AGP 8.5.2 — потолок совместимый с Kotlin 2.0.21, апгрейд AGP даст warning и может развалить KMP-сборку. Coil 3 ↔ Ktor 3 в `gradle/libs.versions.toml` двигаются вместе.

---

## Структура проекта

```
.
├── composeApp/                          точки входа и платформенные манифесты
│   ├── androidMain/                     MainActivity, Application, AndroidManifest
│   └── desktopMain/                     Main.kt с singleWindowApplication
├── shared/                              KMP-модуль — почти вся логика тут
│   └── src/
│       ├── commonMain/                  domain, data, network, UI, navigation, DI
│       ├── androidMain/                 Android actuals (WebView, EncryptedSharedPreferences, Telephoto, OkHttp)
│       └── desktopMain/                 Desktop actuals (системный браузер, java.util.prefs, gesture-based zoom, OkHttp)
├── deploy/nginx/pixmix.mxl.wtf.conf     самодостаточный шаблон reverse-прокси
└── .github/workflows/release.yml        CI/CD — собирает и публикует релиз по тегу
```

Компоненты:
- **Сеть**: один Ktor `HttpClient` с кастомным `HttpSend`-интерсептором, который вешает куки и опционально переписывает URL под прокси. Coil 3 переиспользует тот же клиент через `KtorNetworkFetcherFactory`, поэтому картинки наследуют куки, `Referer`, прокси и дисковый кэш.
- **Навигация**: Decompose. `RootComponent` владеет `StackNavigation<Config>`: `Login → Main → IllustDetail → IllustViewer`. Табы (`home`, `search`, `ranking`, `settings`) живут одновременно — это сохраняет скролл и состояние при переключении.
- **Авторизация**: WebView (Android) или системный браузер (Desktop) → куки кладутся в `PersistentCookiesStorage`, ключ — `https://www.pixiv.net/`, поэтому переключение прокси не сбрасывает сессию.
- **Адаптивный UI**: `MainTabsScreen` сам выбирает `BottomNavigation` (<720dp) или `NavigationRail` (≥720dp). Сетки — `GridCells.Adaptive(140.dp)`; лента кладёт `LazyColumn` в `widthIn(max = 720.dp)` и центрирует.

---

## Сборка и запуск

```bash
# Android
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n wtf.mxl.pixmix.debug/wtf.mxl.pixmix.MainActivity

# Desktop — запуск из исходников
./gradlew :composeApp:run

# Desktop — установщики (только под текущую ОС)
./gradlew :composeApp:packageDeb        # Debian/Ubuntu .deb (Linux)
./gradlew :composeApp:packageAppImage   # директория-бандл рантайма (Linux)
./gradlew :composeApp:packageMsi        # Windows .msi (на Windows-хосте)
./gradlew :composeApp:packageDmg        # macOS .dmg (на macOS-хосте)
```

Таск `packageAppImage` отдаёт **директорию**, не настоящий `.AppImage`. CI оборачивает её в файл через `appimagetool`.

Тестов пока нет.

---

## Релиз

Релизы делаются полностью на GitHub Actions (`.github/workflows/release.yml`).

1. Поднять версии где надо: `composeApp/build.gradle.kts` (Android `versionName`/`versionCode`, Compose `packageVersion`).
2. Поставить тег и запушить:
   ```bash
   git tag v0.1.1
   git push origin v0.1.1
   ```
3. Воркфлоу пускает 4 джобы параллельно (Android, Linux, Windows, macOS), затем релизный джоб собирает все артефакты в один GitHub Release с авто-changelog.

### Секреты для подписи Android

Джоба `android` собирает подписанный release APK, если в репе заданы секреты:

| Секрет | Что это |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Вывод `base64 -w0 keystore.jks`. |
| `ANDROID_KEY_ALIAS` | Алиас ключа в keystore. |
| `ANDROID_KEYSTORE_PASSWORD` | Пароль keystore. |
| `ANDROID_KEY_PASSWORD` | Пароль ключа. **Для PKCS12 (дефолт с JDK 9) обязан совпадать с паролем keystore** — `keytool` игнорирует `-keypass` при создании. |

Если секретов нет, воркфлоу падает на debug APK, чтобы PR из форков всё равно собирались.

Сгенерировать новый keystore:
```bash
keytool -genkeypair -v -keystore ~/.android-keystores/pixmix-release.jks \
  -alias pixmix -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass <PWD> -keypass <PWD> -dname "CN=PixMix"

# залить как секреты репы
gh secret set ANDROID_KEYSTORE_BASE64 --body "$(base64 -w0 ~/.android-keystores/pixmix-release.jks)"
gh secret set ANDROID_KEY_ALIAS --body "pixmix"
gh secret set ANDROID_KEYSTORE_PASSWORD --body "<PWD>"
gh secret set ANDROID_KEY_PASSWORD --body "<PWD>"
```

> **Сделай бэкап keystore.** Потеряешь — не сможешь обновлять приложение под тем же `applicationId` в Google Play.

---

## Свой прокси

Приложение по умолчанию ходит к pixiv напрямую. Прокси нужен тем, кому ISP режет/блокирует pixiv (быстрее VPN, один порт, на клиенте только URL + токен).

### Что нужно

- VPS, с которого pixiv доступен. Hetzner Helsinki/Falkenstein работает; обычная российская VPS — **не** работает без upstream-туннеля.
- Свободные порты 80/443 (без других веб-серверов на хосте).
- DNS A-запись на твоём поддомене (например `pixmix.твой-домен`), указывающая на IP VPS. Проверь: `dig pixmix.твой-домен +short`.

### Установка одной пачкой (Debian/Ubuntu)

Под root (или через `sudo`):

```bash
# 1. ставим всё разом
apt update && apt install -y nginx certbot python3-certbot-nginx ufw curl ssl-cert

# 2. файрвол
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

# 3. забираем шаблон nginx из репы
curl -fsSL https://raw.githubusercontent.com/mixelka75/pixiv-mix-app/main/deploy/nginx/pixmix.mxl.wtf.conf \
  -o /etc/nginx/sites-available/pixmix.твой-домен.conf

# 4. подменяем домен на свой
sed -i 's|pixmix.mxl.wtf|pixmix.твой-домен|g' /etc/nginx/sites-available/pixmix.твой-домен.conf

# 5. генерим длинный токен и вшиваем в конфиг
TOKEN=$(openssl rand -hex 32) && echo "TOKEN: $TOKEN"     # сохрани!
sed -i "s|REPLACE_ME_WITH_LONG_RANDOM_STRING|$TOKEN|" /etc/nginx/sites-available/pixmix.твой-домен.conf

# 6. включаем сайт, проверяем, перечитываем
ln -sf /etc/nginx/sites-available/pixmix.твой-домен.conf /etc/nginx/sites-enabled/
systemctl enable --now nginx
nginx -t && systemctl reload nginx

# 7. получаем настоящий Let's Encrypt сертификат
certbot --nginx -d pixmix.твой-домен --redirect --non-interactive --agree-tos -m ты@example.com

# 8. проверка — без токена 403, с токеном 200/4xx от pixiv
curl -sI "https://pixmix.твой-домен/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
curl -sI -H "X-Pixmix-Token: $TOKEN" "https://pixmix.твой-домен/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
```

В приложении: **Settings → Use proxy** → впиши `https://pixmix.твой-домен` и токен.

### Подводные камни

- **На VPS уже стоит веб-сервер?** Если 80/443 заняты — этот шаблон в лоб не встанет. Либо отдельный VPS под PixMix, либо вмерживай два `location`-блока в свой существующий reverse-proxy.
- **Pixiv заблокирован с VPS?** `curl -I https://www.pixiv.net/` должен вернуть `200`. Если на запросе через прокси ловишь `502` — VPS сам не достаёт pixiv. Бери сервер в регионе без РКН-подобных блокировок (Hetzner Helsinki/Falkenstein, OVH, Vultr Tokyo).
- **Авто-обновление сертификата**: certbot ставит systemd-таймер (`systemctl status certbot.timer`).

### Если что-то сломалось

```bash
nginx -t                                   # синтаксис конфига
journalctl -u nginx -n 50 --no-pager       # ошибки старта
tail -f /var/log/nginx/error.log           # рантайм-ошибки (auth, upstream, TLS)
adb logcat -s Ktor:V *:F                   # сетевые логи на стороне клиента
```

---

## Полезные команды на каждый день

```bash
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am force-stop wtf.mxl.pixmix.debug
adb shell am start -n wtf.mxl.pixmix.debug/wtf.mxl.pixmix.MainActivity
adb logcat -s Ktor:V *:F                   # фокус на сети
adb shell screencap -p /sdcard/p.png && adb pull /sdcard/p.png /tmp/

./gradlew :composeApp:compileKotlinDesktop # быстрый type-check для desktop
```
