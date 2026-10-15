package com.openflux.app.vpn.core;

import com.openflux.app.vpn.store.VpnProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Неизменяемая конфигурация рантайма OpenFlux, собираемая из {@link VpnProfile}. */
public final class OpenFluxConfig {
    public final String host;
    public final int port;
    public final String transport;
    public final String authToken;
    public final String publicKey;
    public final String privateKey;
    public final String keyRef;
    public final String sni;
    public final String alpn;
    public final int mtu;
    public final List<String> dns;
    public final List<String> routes;
    public final boolean fullTunnel;
    public final int connectTimeoutMs;
    public final int readTimeoutMs;
    public final int keepaliveMs;

    public OpenFluxConfig(VpnProfile p) {
        this.host = p.host;
        this.port = p.port;
        this.transport = p.transport;
        this.authToken = p.authToken;
        this.publicKey = p.publicKey;
        this.privateKey = p.privateKey;
        this.keyRef = p.keyRef;
        this.sni = p.sni;
        this.alpn = p.alpn;
        this.mtu = p.mtu;
        this.dns = Collections.unmodifiableList(new ArrayList<>(p.dns));
        this.routes = Collections.unmodifiableList(new ArrayList<>(p.routes));
        this.fullTunnel = p.fullTunnel;
        this.connectTimeoutMs = p.connectTimeoutMs;
        this.readTimeoutMs = p.readTimeoutMs;
        this.keepaliveMs = p.keepaliveMs;
    }

    public static OpenFluxConfig fromProfile(VpnProfile p) {
        return new OpenFluxConfig(p);
    }
}
