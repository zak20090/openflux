package com.openflux.app.vpn.core;

import android.util.Log;

import com.openflux.sdk.ClientConfig;
import com.openflux.sdk.OpenFluxClient;

import java.io.IOException;

/** Адаптер к OpenFlux SDK (модуль :openflux-sdk). */
public class OpenFluxAdapterImpl implements OpenFluxAdapter {
    private static final String TAG = "OpenFluxAdapterImpl";

    private OpenFluxConfig config;
    private ProtectProvider protectProvider;
    private PacketCallback callback;
    private volatile OpenFluxClient client;

    @Override
    public synchronized void init(OpenFluxConfig config, ProtectProvider protectProvider,
                                  PacketCallback callback) throws IOException {
        if (config == null || protectProvider == null) throw new IOException("bad init args");
        disconnect();
        this.config = config;
        this.protectProvider = protectProvider;
        this.callback = callback;
        Log.i(TAG, "init: " + config.host + ":" + config.port + " (" + config.transport + ")");
    }

    @Override
    public synchronized void connect() throws IOException {
        if (config == null) throw new IOException("init() not called");
        ClientConfig c = new ClientConfig();
        c.host = config.host;
        c.port = config.port;
        c.transport = config.transport;
        c.authToken = config.authToken;
        c.publicKey = config.publicKey;
        c.privateKey = config.privateKey;
        c.sni = config.sni;
        c.alpn = config.alpn;
        c.mtu = config.mtu;
        c.connectTimeoutMs = config.connectTimeoutMs;
        c.readTimeoutMs = config.readTimeoutMs;
        c.keepaliveMs = config.keepaliveMs;

        final PacketCallback cb = callback;
        final ProtectProvider pp = protectProvider;
        OpenFluxClient cl = new OpenFluxClient(c,
                socket -> pp.protect(socket), // защита ДО connect()
                (pkt, len) -> { if (cb != null) cb.onInboundPacket(pkt, len); },
                cause -> Log.w(TAG, "transport dropped: " + cause)); // isConnected() станет false
        cl.connect();
        client = cl;
        Log.i(TAG, "connected");
    }

    @Override
    public synchronized void disconnect() {
        OpenFluxClient c = client;
        client = null;
        if (c != null) c.close();
    }

    @Override
    public void sendPacket(byte[] packet, int length) throws IOException {
        OpenFluxClient c = client;
        if (c == null) throw new IOException("not connected");
        c.send(packet, length);
    }

    @Override
    public boolean isConnected() {
        OpenFluxClient c = client;
        return c != null && c.isConnected();
    }
}
