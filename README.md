# OpenFlux VPN — Android (Java)

Android-приложение с `VpnService`, интеграцией OpenFlux SDK, переподключением, редактором профиля,
шифрованным хранилищем секретов, IPv6 и kill-switch.

> **Важно про SDK.** Публичного артефакта OpenFlux SDK нет, поэтому в репозитории лежит модуль
> `:openflux-sdk` — референсная реализация клиента (кадры поверх TCP/TLS, AUTH-токен, keepalive).
> Приложение обращается к SDK только через `OpenFluxAdapterImpl`, так что замена на настоящий
> артефакт локальна. Протокол референса **не** совместим с реальным сервером OpenFlux, пока
> вы не подставите реальный SDK.

## Структура

```
openflux-sdk/                       # модуль SDK (OpenFluxClient, ClientConfig, SocketProtector…)
app/src/main/java/com/openflux/app/
├── ui/{MainActivity,ProfileEditorActivity}.java
└── vpn/
    ├── MyVpnService.java           # TUN (IPv4+IPv6), маршруты/DNS, NetworkCallback, kill-switch
    ├── tun/TunPump.java            # TUN <-> контроллер (inject = inbound -> TUN)
    ├── core/                       # Controller (reconnect), Adapter(+Impl), Config, ProtectProvider
    ├── store/                      # VpnProfile, ProfileStore (EncryptedSharedPreferences), ProfileValidator
    └── util/{RetryPolicy,CidrUtil}.java
```

## Настройка SDK

1. Локально (по умолчанию): `implementation project(':openflux-sdk')` в `app/build.gradle`,
   модуль подключён в `settings.gradle`.
2. Внешний SDK: удалите модуль, добавьте его Maven-репозиторий в
   `dependencyResolutionManagement.repositories` (`settings.gradle`) и
   `implementation 'group:artifact:version'` в `app/build.gradle`; затем перепишите только
   `OpenFluxAdapterImpl.connect()`.
3. Сокеты SDK защищаются через `ProtectProvider.protect(...)` **до** `connect()`
   (в референсе — `SocketProtector`).
4. Входящие пакеты SDK → `PacketCallback` → `TunPump.inject()`; сбой чтения/отправки/EOF переводит
   `isConnected()` в `false`, контроллер немедленно переподключается (backoff 1с…30с, сбрасывается
   при смене сети).

## Сборка

JDK 17, Android SDK 34, доступ к Google Maven:

```bash
echo "sdk.dir=/путь/к/Android/sdk" > local.properties
./gradlew assembleDebug
```

## Конфигурация профиля

Откройте **Edit profile** на главном экране. Пустые необязательные поля получают значения по умолчанию.

| Поле | По умолчанию | Описание |
|------|--------------|----------|
| host, port | —, 443 | сервер (host обязателен) |
| transport | `tls` | `tcp` или `tls` |
| authToken (секрет) | — | обязателен |
| privateKey (секрет), publicKey, keyRef | пусто | ключи; keyRef — внешний алиас |
| sni, alpn | host, пусто | параметры TLS (alpn через запятую) |
| mtu | 1400 | 576..9000 |
| dns | `1.1.1.1`, `2606:4700:4700::1111` | v4/v6 |
| fullTunnel / routes | вкл / `0.0.0.0/0,::/0` | full: все маршруты v4+v6; split: только `routes` |
| tunAddress(6), tunPrefix(6) | `10.8.0.2/32`, `fd00:8::2/128` | адреса TUN |
| connect/read/keepalive, мс | 10000 / 30000 / 15000 | keepalive < read |
| killSwitch | вкл | см. ниже |

Секреты (`authToken`, `privateKey`) хранятся в `EncryptedSharedPreferences` (AndroidX Security,
мастер-ключ AES-256-GCM в Android Keystore); остальное — в обычных SharedPreferences.
Экран редактора защищён `FLAG_SECURE`. Невалидный профиль не сохраняется и не запускает сервис.

## Разрешение VPN и запуск

1. **Connect** → `VpnService.prepare()`; при необходимости системный диалог.
2. `RESULT_OK` → foreground-сервис `MyVpnService`.
3. Сервис валидирует профиль, строит `Builder` (IPv4+IPv6 адреса, маршруты, DNS), `establish()`,
   запускает `TunPump`, `OpenFluxController` и `NetworkCallback`.
4. **Disconnect** → `ACTION_STOP`; `onRevoke()`/`onDestroy()` освобождают ресурсы.

## Сеть, IPv6, kill-switch

- `registerDefaultNetworkCallback`: при смене default-сети (Wi-Fi↔LTE) вызывается
  `setUnderlyingNetworks` и `requestReconnect()` (без ожидания backoff).
- IPv6: в full-режиме всегда маршрутизируются `0.0.0.0/0` и `::/0`, TUN получает IPv6-адрес,
  DNS могут быть v6 — IPv6-трафик не обходит туннель.
- Kill-switch: TUN остаётся поднятым на всё время переподключений, пакеты при `state != CONNECTED`
  отбрасываются, а не уходят в сеть напрямую. В split-режиме при включённом kill-switch добавляется
  `::/0` (если нет v6-маршрутов), чтобы IPv6 не утекал.

**Ограничения Android:** приложение не может запретить сеть вне своего процесса. Kill-switch
работает, пока жив процесс/сервис. Если процесс убит или VPN отозван, трафик идёт напрямую до
перезапуска. Для надёжной защиты включите в системе **Always-on VPN + «Блокировать соединения без
VPN»** (Настройки → Сеть → VPN → ⚙). Split-режим по определению пропускает не-маршрутизируемый трафик.

## Чек-лист тестирования

Результаты ниже — честный статус: в среде разработки нет Android SDK-репозитория/устройства
(Gradle не смог загрузить Android Gradle Plugin), поэтому проверялась только компиляция модуля
`:openflux-sdk` (`javac`). Остальное нужно пройти на устройстве.

| Проверка | Статус |
|----------|--------|
| Сборка `:openflux-sdk` (javac) | ✅ пройдено |
| Сборка `:app` (`assembleDebug`) | ⏳ не запускалась (нет доступа к Google Maven) |
| Циклы connect/disconnect (×10, нет утечек потоков/уведомления) | ⏳ на устройстве |
| Wi-Fi ↔ LTE: туннель переподключается | ⏳ на устройстве |
| DNS leak (dnsleaktest.com, v4 и v6) | ⏳ на устройстве |
| IPv4 / IPv6 (test-ipv6.com) | ⏳ на устройстве |
| Reconnect после остановки сервера (1с, 2с, 4с … 30с, затем `CONNECTED`) | ⏳ на устройстве |
| Kill-switch: при падении сервера трафик не идёт напрямую; Always-on+Block | ⏳ на устройстве |
| Отзыв VPN-разрешения останавливает сервис | ⏳ на устройстве |

## Известные ограничения / дальше

- Референсный SDK не совместим с настоящим сервером; нужна замена на реальный SDK.
- UDP-транспорт не реализован; ALPN на Android < 10 может игнорироваться.
- Нет unit/instrumented-тестов; нет сохранения нескольких профилей.
- Далее: реальный SDK, тесты `ProfileValidator`/`RetryPolicy`/контроллера, мульти-профили,
  опция kill-switch «жёсткий» (остановка при FAILED).
