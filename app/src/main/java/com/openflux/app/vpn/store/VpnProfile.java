package com.openflux.app.vpn.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Профиль VPN. Ключи не хранятся в коде — заполняются пользователем/провижинингом. */
public class VpnProfile {
    public String host = "example.invalid";
    public int port = 443;
    public List<String> transports = new ArrayList<>(Arrays.asList("tcp"));
    public String publicKey = "";
    public String privateKey = "";
    public List<String> routes = new ArrayList<>(Arrays.asList("0.0.0.0/0"));
    public List<String> dns = new ArrayList<>(Arrays.asList("1.1.1.1"));
    public String tunAddress = "10.8.0.2";
    public int tunPrefix = 32;
    public int mtu = 1400;
}
