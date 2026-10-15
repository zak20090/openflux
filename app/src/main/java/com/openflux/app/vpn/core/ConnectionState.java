package com.openflux.app.vpn.core;

/** Состояние соединения с OpenFlux. */
public enum ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}
