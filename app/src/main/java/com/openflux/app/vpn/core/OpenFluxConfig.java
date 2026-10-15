package com.openflux.app.vpn.core;

import com.openflux.app.vpn.store.VpnProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Неизменяемая конфигурация рантайма OpenFlux, собираемая из {@link VpnProfile}. */
public final class OpenFluxConfig {
    public final String host;
    public final int port;
    public final List<String> transports;
    public final String publicKey;
    public final String privateKey;

    public OpenFluxConfig(String host, int port, List<String> transports,
                          String publicKey, String privateKey) {
        this.host = host;
        this.port = port;
        this.transports = Collections.unmodifiableList(new ArrayList<>(transports));
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public static OpenFluxConfig fromProfile(VpnProfile p) {
        return new OpenFluxConfig(p.host, p.port, p.transports, p.publicKey, p.privateKey);
    }
}
