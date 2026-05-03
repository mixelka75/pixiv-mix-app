# PixMix

> [English](README.md) · **Русский**

Нативный Android-клиент для pixiv поверх AJAX/web API. Kotlin Multiplatform + Compose Multiplatform. Опционально — собственный nginx-прокси для тех, кто хочет жирный канал без VPN или у кого pixiv режется ISP.

---

## Структура проекта

```
.
├── composeApp/             точка входа Android (MainActivity, Application, manifest)
├── shared/                 KMP-модуль — domain, data, network, UI, экраны
└── deploy/
    └── nginx/
        └── pixmix.mxl.wtf.conf    самодостаточный шаблон reverse-прокси
```

## Сборка приложения

Нужно: JDK 17, Android SDK с `platforms;android-34` и `build-tools;34.0.0`.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

App ID (debug): `wtf.mxl.pixmix.debug`. Запуск:
```bash
adb shell am start -n wtf.mxl.pixmix.debug/wtf.mxl.pixmix.MainActivity
```

---

## Развёртывание прокси

Прокси **опционален**. По умолчанию приложение ходит к pixiv напрямую. Включается в **Settings → Use proxy**, если хочешь гнать трафик через свой сервер (быстрее VPN, обходит блокировки на уровне ISP).

### Что нужно
- VPS, с которого pixiv доступен (Hetzner Helsinki/Falkenstein работает; обычная российская VPS — **не** работает без upstream-туннеля).
- Свободные порты 80/443 (без других веб-серверов на хосте).
- DNS A-запись на твоём поддомене (например `pixmix.твой-домен`), указывающая на IP VPS. Проверь через `dig pixmix.твой-домен +short`.

### Установка одной пачкой (Debian/Ubuntu)

Зайди по SSH под root (или через sudo) и выполняй:

```bash
# 1. ставим всё разом
apt update && apt install -y nginx certbot python3-certbot-nginx ufw curl ssl-cert

# 2. файрвол
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

# 3. забираем шаблон nginx из этого репо
curl -fsSL https://raw.githubusercontent.com/mixelka75/pixiv-mix-app/main/deploy/nginx/pixmix.mxl.wtf.conf \
  -o /etc/nginx/sites-available/pixmix.твой-домен.conf

# 4. подменяем домен на свой (везде где встречается pixmix.mxl.wtf)
sed -i 's|pixmix.mxl.wtf|pixmix.твой-домен|g' /etc/nginx/sites-available/pixmix.твой-домен.conf

# 5. генерируем длинный токен и вшиваем в конфиг
TOKEN=$(openssl rand -hex 32) && echo "TOKEN: $TOKEN"     # сохрани!
sed -i "s|REPLACE_ME_WITH_LONG_RANDOM_STRING|$TOKEN|" /etc/nginx/sites-available/pixmix.твой-домен.conf

# 6. включаем сайт, проверяем синтаксис, перечитываем
ln -sf /etc/nginx/sites-available/pixmix.твой-домен.conf /etc/nginx/sites-enabled/
systemctl enable --now nginx
nginx -t && systemctl reload nginx

# 7. получаем настоящий Let's Encrypt сертификат (затрёт snakeoil-пути)
certbot --nginx -d pixmix.твой-домен --redirect --non-interactive --agree-tos -m ты@example.com

# 8. проверка — без токена должно вернуть 403, с токеном — 200/4xx от pixiv
curl -sI "https://pixmix.твой-домен/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
curl -sI -H "X-Pixmix-Token: $TOKEN" "https://pixmix.твой-домен/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
```

### Подключение приложения

В приложении: **Settings → Use proxy** (включить свич)
- **Proxy base URL**: `https://pixmix.твой-домен`
- **Proxy token**: значение `$TOKEN` из шага 5

Сессия pixiv сохраняется при переключении proxy — перелогиниваться не надо.

### Подводные камни

- **На VPS уже есть какой-то веб-сервер?** Если 80/443 заняты (docker-proxy, apache, другой nginx) — этот шаблон в лоб не встанет. Либо отдельный VPS под PixMix, либо вмерживай два `location` блока в свой существующий reverse-proxy.
- **Pixiv заблокирован с VPS?** `curl -I https://www.pixiv.net/` должен вернуть `200`. Если на запросе через прокси ловишь `502` — VPS сам не может достучаться до pixiv. Бери сервер в регионе без РКН-подобных блокировок (Hetzner Helsinki/Falkenstein, OVH, Vultr Tokyo).
- **Авто-обновление сертификата**: certbot ставит systemd-таймер (`systemctl status certbot.timer`). Больше делать ничего не нужно.

### Если что-то сломалось

```bash
nginx -t                                   # синтаксис конфига
journalctl -u nginx -n 50 --no-pager       # ошибки старта
tail -f /var/log/nginx/error.log           # рантайм-ошибки (auth, upstream, TLS)
adb logcat -s Ktor:V *:F                   # сетевые логи на стороне клиента
```

---

## Лицензия

Лицензия не выбрана — все права защищены.
