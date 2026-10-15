package com.openflux.sdk;

/** Notified once when the transport drops (read failure, EOF, keepalive timeout). */
public interface DisconnectListener {
    void onDisconnected(Throwable cause);
}
