package com.openflux.app.vpn.core;

import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Позволяет OpenFlux исключать свои сокеты из VPN (иначе будет петля маршрутизации).
 * Обычно реализуется через {@code VpnService.protect(...)}.
 */
public interface ProtectProvider {
    boolean protect(Socket socket);

    boolean protect(DatagramSocket socket);

    /** Для нативных сокетов (SDK на C/Rust/Go) — по файловому дескриптору. */
    boolean protect(int fd);
}
