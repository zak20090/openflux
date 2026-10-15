package com.openflux.sdk;

/** Receives decapsulated inbound IP packets. */
public interface PacketListener {
    void onPacket(byte[] packet, int length);
}
