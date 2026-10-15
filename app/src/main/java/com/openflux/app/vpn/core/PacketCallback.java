package com.openflux.app.vpn.core;

/** Колбэк для входящих (из туннеля в TUN) IP-пакетов. */
public interface PacketCallback {
    void onInboundPacket(byte[] packet, int length);
}
