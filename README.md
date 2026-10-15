# OpenFlux VPN — базовый Android-каркас (Java)

Компилируемый скелет Android-приложения с `VpnService` и заготовками интеграции OpenFlux.
**Реального SDK OpenFlux здесь нет** — `OpenFluxAdapterImpl` это заглушка с маркерами `TODO(OpenFlux)`.

## Структура проекта

```
app/src/main/java/com/openflux/app/
├── ui/MainActivity.java            # Connect / Disconnect, текст состояния
└── vpn/
    ├── MyVpnService.java           # VpnService: establish(), маршруты/DNS, foreground
    ├── VpnNotifFactory.java        # канал и уведомление
    ├── tun/TunPump.java            # TUN <-> контроллер
    ├── core/
    │   ├── OpenFluxController.java # start/stop, reconnect с exponential backoff
    │   ├── OpenFluxAdapter.java    # интерфейс над SDK
    │   ├── OpenFluxAdapterImpl.java# ЗАГЛУШКА (место для SDK)
    │   ├── ProtectProvider.java    # protect() сокетов
    │   ├── OpenFluxConfig.java     # конфиг рантайма из профиля
    │   ├── ConnectionState.java
    │   └── PacketCallback.java     # входящие пакеты
    ├── store/{ProfileStore,VpnProfile}.java
    └── util/{RetryPolicy,CidrUtil}.java
```

## Сборка и запуск

Требования: JDK 17, Android SDK (platform 34), доступ к Google Maven.

```bash
echo "sdk.dir=/путь/к/Android/sdk" > local.properties
./gradlew assembleDebug
./gradlew installDebug      # устройство/эмулятор с Android 7.0+ (minSdk 24)
```

Или откройте проект в Android Studio и нажмите Run.

## Поток разрешения VPN

1. Нажатие **Connect** → `VpnService.prepare(context)`.
2. Если вернулся `Intent` — запускается системный диалог (`startActivityForResult`).
3. При `RESULT_OK` стартует `MyVpnService` как foreground-сервис (канал уведомлений `openflux_vpn`).
4. Сервис строит `Builder` (адрес, маршруты, DNS из профиля), вызывает `establish()`,
   запускает `TunPump` и `OpenFluxController`.
5. **Disconnect** отправляет `ACTION_STOP`; `onDestroy()`/`onRevoke()` безопасно освобождают ресурсы.

## Куда подключать реальный OpenFlux SDK

Только в `vpn/core/OpenFluxAdapterImpl.java` (места помечены `TODO(OpenFlux)`):

- `init` — инициализация рантайма и конфига;
- `connect` / `disconnect` — установка и закрытие сессии;
- `sendPacket` — отправка исходящего IP-пакета;
- inbound — вызывать `PacketCallback.onInboundPacket(...)`;
- сокеты SDK защищать через `ProtectProvider.protect(...)` **до** `connect()`.

Зависимость добавьте в `app/build.gradle` (раздел `dependencies`).

## Поля профиля (`VpnProfile`)

| Поле | Описание |
|------|----------|
| `host`, `port` | адрес и порт сервера |
| `transports` | список транспортов (напр. `tcp`, `udp`) |
| `publicKey`, `privateKey` | ключи (по умолчанию пустые; в коде секретов нет) |
| `routes` | CIDR-маршруты, по умолчанию `0.0.0.0/0` |
| `dns` | DNS-серверы, по умолчанию `1.1.1.1` |
| `tunAddress`, `tunPrefix`, `mtu` | параметры TUN-интерфейса |

Профиль хранится в `SharedPreferences` (`ProfileStore`). Для ключей позже используйте Android Keystore.

## Чек-лист тестирования

- [ ] **Reconnect**: обрыв соединения → состояние `RECONNECTING`, паузы 1с, 2с, 4с … до 30с, затем `CONNECTED`.
- [ ] **DNS leak**: при включённом VPN DNS-запросы идут только на серверы из профиля (dnsleaktest.com).
- [ ] **Wi-Fi ↔ LTE**: переключение сети не оставляет «зависший» туннель, происходит переподключение.
- [ ] Отзыв разрешения VPN из системы корректно останавливает сервис.
- [ ] Disconnect убирает уведомление и ключ VPN в статус-баре.

## Известные ограничения каркаса

- SDK-вызовов нет: пакеты из TUN отбрасываются, трафик не туннелируется.
- Только IPv4-адрес TUN; IPv6 не настроен.
- Профиль не редактируется из UI, ключи не шифруются.
- Нет отслеживания смены сети (`ConnectivityManager`) и kill-switch.
- Нет unit/instrumented-тестов.

## План интеграции OpenFlux (по шагам)

1. Добавить SDK в `app/build.gradle`.
2. Расширить `OpenFluxConfig` полями, нужными SDK.
3. Реализовать `init` в `OpenFluxAdapterImpl`.
4. Реализовать `connect`/`disconnect` с `protect()` сокетов.
5. Реализовать `sendPacket` и inbound-callback → `TunPump.inject`.
6. Сообщать о потере соединения через `isConnected()`, чтобы сработал reconnect.
7. Добавить редактор профиля и безопасное хранение ключей.
8. Добавить `NetworkCallback` для смены сети, IPv6, kill-switch.
9. Пройти чек-лист тестирования.
