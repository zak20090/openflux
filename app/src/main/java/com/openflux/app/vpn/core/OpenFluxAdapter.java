package com.openflux.app.vpn.core;

import java.io.IOException;

/** Абстракция над OpenFlux SDK. Приложение зависит только от этого интерфейса. */
public interface OpenFluxAdapter {
    void init(OpenFluxConfig config, ProtectProvider protectProvider, PacketCallback callback)
            throws IOException;

    void connect() throws IOException;

    void disconnect();

    /** Отправить исходящий IP-пакет в туннель. */
    void sendPacket(byte[] packet, int length) throws IOException;

    boolean isConnected();
}
