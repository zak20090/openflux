package com.openflux.app.vpn.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Профиль VPN. Секреты (privateKey, authToken) хранятся только в зашифрованном хранилище. */
public class VpnProfile {
    public static final String TRANSPORT_TCP = "tcp";
    public static final String TRANSPORT_TLS = "tls";

    public String host = "";
    public int port = 443;
    public String transport = TRANSPORT_TLS;

    /** Секрет: токен аутентификации. */
    public String authToken = "";
    /** Секрет: приватный ключ. */
    public String privateKey = "";
    public String publicKey = "";
    /** Ссылки (алиасы) на ключи, хранящиеся вне приложения; не секрет. */
    public String keyRef = "";

    public String sni = "";
    public String alpn = "";

    /** true — весь трафик (0.0.0.0/0 и ::/0); false — только {@link #routes}. */
    public boolean fullTunnel = true;
    public List<String> routes = new ArrayList<>(Arrays.asList("0.0.0.0/0", "::/0"));
    public List<String> dns = new ArrayList<>(Arrays.asList("1.1.1.1", "2606:4700:4700::1111"));

    public String tunAddress = "10.8.0.2";
    public int tunPrefix = 32;
    public String tunAddress6 = "fd00:8::2";
    public int tunPrefix6 = 128;
    public int mtu = 1400;

    public int connectTimeoutMs = 10000;
    public int readTimeoutMs = 30000;
    public int keepaliveMs = 15000;

    /** Блокировать трафик (держать TUN), пока туннель не работает. */
    public boolean killSwitch = true;
}
