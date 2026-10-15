package com.openflux.app.vpn.core;

import android.util.Log;

import java.io.IOException;

/**
 * Заглушка адаптера. Реальных вызовов SDK нет; сеть не используется.
 * Все места интеграции помечены TODO(OpenFlux).
 */
public class OpenFluxAdapterImpl implements OpenFluxAdapter {
    private static final String TAG = "OpenFluxAdapterImpl";

    private OpenFluxConfig config;
    private ProtectProvider protectProvider;
    private PacketCallback callback;
    private volatile boolean connected;

    @Override
    public void init(OpenFluxConfig config, ProtectProvider protectProvider, PacketCallback callback)
            throws IOException {
        this.config = config;
        this.protectProvider = protectProvider;
        this.callback = callback;
        // TODO(OpenFlux): инициализировать рантайм SDK и передать ему config.
        Log.i(TAG, "init (stub): " + config.host + ":" + config.port);
    }

    @Override
    public void connect() throws IOException {
        if (config == null) throw new IOException("init() not called");
        // TODO(OpenFlux): создать сокет(ы) транспорта и вызвать protectProvider.protect(socket/fd)
        //   ДО connect(), затем выполнить handshake через SDK.
        // TODO(OpenFlux): зарегистрировать callback входящих пакетов в SDK и вызывать
        //   callback.onInboundPacket(buf, len) для каждого пакета из туннеля.
        connected = true;
        Log.i(TAG, "connect (stub)");
    }

    @Override
    public void disconnect() {
        // TODO(OpenFlux): закрыть сессию и освободить ресурсы SDK.
        connected = false;
        Log.i(TAG, "disconnect (stub)");
    }

    @Override
    public void sendPacket(byte[] packet, int length) throws IOException {
        if (!connected) throw new IOException("not connected");
        // TODO(OpenFlux): передать пакет в SDK (инкапсуляция/шифрование/отправка).
        // Заглушка: пакет отбрасывается.
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
