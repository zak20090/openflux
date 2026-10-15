package com.openflux.app.vpn.store;

import com.openflux.app.vpn.util.CidrUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/** Проверка профиля. Возвращает список ошибок (пустой — профиль корректен). */
public final class ProfileValidator {
    private ProfileValidator() {}

    public static List<String> validate(VpnProfile p) {
        List<String> e = new ArrayList<>();
        if (p.host == null || p.host.trim().isEmpty() || p.host.contains(" ")) e.add("Host is required");
        if (p.port < 1 || p.port > 65535) e.add("Port must be 1..65535");
        if (!VpnProfile.TRANSPORT_TCP.equals(p.transport)
                && !VpnProfile.TRANSPORT_TLS.equals(p.transport)) e.add("Transport must be tcp or tls");
        if (p.authToken == null || p.authToken.isEmpty()) e.add("Auth token is required");
        if (p.mtu < 576 || p.mtu > 9000) e.add("MTU must be 576..9000");
        if (p.connectTimeoutMs < 1000 || p.readTimeoutMs < 1000 || p.keepaliveMs < 1000) {
            e.add("Timeouts must be >= 1000 ms");
        }
        if (p.keepaliveMs >= p.readTimeoutMs) e.add("Keepalive must be less than read timeout");
        if (!isIp(p.tunAddress, false) || p.tunPrefix < 1 || p.tunPrefix > 32) e.add("Invalid TUN IPv4 address/prefix");
        if (!isIp(p.tunAddress6, true) || p.tunPrefix6 < 1 || p.tunPrefix6 > 128) e.add("Invalid TUN IPv6 address/prefix");
        if (p.dns.isEmpty()) e.add("At least one DNS server is required");
        for (String d : p.dns) if (!isIp(d, null)) e.add("Invalid DNS: " + d);
        if (!p.fullTunnel) {
            if (p.routes.isEmpty()) e.add("Split mode needs at least one route");
            for (String r : p.routes) if (CidrUtil.parse(r) == null) e.add("Invalid route: " + r);
        }
        return e;
    }

    /** @param v6 true = только IPv6, false = только IPv4, null = любой */
    static boolean isIp(String s, Boolean v6) {
        if (s == null) return false;
        s = s.trim();
        boolean has6 = s.contains(":");
        if (v6 != null && v6 != has6) return false;
        if (!has6 && !s.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return false;
        if (has6 && !s.matches("[0-9a-fA-F:.]+")) return false;
        try {
            InetAddress.getByName(s); // литерал, DNS-запрос не выполняется
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
