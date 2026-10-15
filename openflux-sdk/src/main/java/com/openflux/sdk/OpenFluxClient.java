package com.openflux.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SNIHostName;

/**
 * Reference OpenFlux client: length-prefixed frames over TCP/TLS.
 * Frame = 1 byte type + 2 byte big-endian length + payload.
 * Types: 1 = AUTH (token), 2 = DATA (IP packet), 3 = KEEPALIVE.
 * Replace this module with the vendor artifact once published; the app only
 * touches it through OpenFluxAdapterImpl.
 */
public final class OpenFluxClient {
    private static final int T_AUTH = 1, T_DATA = 2, T_KEEPALIVE = 3;

    private final ClientConfig cfg;
    private final SocketProtector protector;
    private final PacketListener listener;
    private final DisconnectListener disconnectListener;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Object writeLock = new Object();

    private volatile Socket socket;
    private DataOutputStream out;
    private Thread reader;
    private Thread keepalive;

    public OpenFluxClient(ClientConfig cfg, SocketProtector protector,
                          PacketListener listener, DisconnectListener disconnectListener) {
        this.cfg = cfg;
        this.protector = protector;
        this.listener = listener;
        this.disconnectListener = disconnectListener;
    }

    public synchronized void connect() throws IOException {
        if (connected.get()) return;
        Socket raw = new Socket();
        try {
            // The socket must be protected before connect(), otherwise it loops into the VPN.
            if (protector != null && !protector.protect(raw)) {
                throw new IOException("protect() failed");
            }
            raw.setTcpNoDelay(true);
            raw.connect(new InetSocketAddress(cfg.host, cfg.port), cfg.connectTimeoutMs);
            raw.setSoTimeout(cfg.readTimeoutMs);
            Socket s = raw;
            if ("tls".equalsIgnoreCase(cfg.transport)) {
                SSLSocket ssl = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                        .createSocket(raw, cfg.host, cfg.port, true);
                SSLParameters p = ssl.getSSLParameters();
                String sni = cfg.sni == null || cfg.sni.isEmpty() ? cfg.host : cfg.sni;
                try {
                    p.setServerNames(java.util.Collections.singletonList(new SNIHostName(sni)));
                } catch (IllegalArgumentException ignored) {
                    // e.g. IP literal: no SNI
                }
                if (cfg.alpn != null && !cfg.alpn.isEmpty()) {
                    try {
                        p.setApplicationProtocols(cfg.alpn.split(","));
                    } catch (Throwable ignored) {
                        // not available on older Android releases
                    }
                }
                p.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(p);
                ssl.startHandshake();
                s = ssl;
            }
            DataOutputStream o = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            socket = s;
            out = o;
            writeFrame(T_AUTH, cfg.authToken.getBytes(StandardCharsets.UTF_8), -1);
            connected.set(true);
            reader = new Thread(() -> readLoop(in), "openflux-sdk-reader");
            reader.start();
            keepalive = new Thread(this::keepaliveLoop, "openflux-sdk-keepalive");
            keepalive.start();
        } catch (IOException | RuntimeException e) {
            connected.set(false);
            closeQuietly(raw);
            socket = null;
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void send(byte[] packet, int length) throws IOException {
        if (!connected.get()) throw new IOException("not connected");
        try {
            writeFrame(T_DATA, packet, length);
        } catch (IOException e) {
            fail(e);
            throw e;
        }
    }

    public synchronized void close() {
        if (connected.getAndSet(false) || socket != null) {
            closeQuietly(socket);
            socket = null;
        }
        Thread k = keepalive;
        if (k != null) k.interrupt();
    }

    private void writeFrame(int type, byte[] payload, int length) throws IOException {
        int len = length < 0 ? payload.length : length;
        if (len > 0xFFFF) throw new IOException("frame too large");
        synchronized (writeLock) {
            DataOutputStream o = out;
            if (o == null) throw new IOException("closed");
            o.writeByte(type);
            o.writeShort(len);
            o.write(payload, 0, len);
            o.flush();
        }
    }

    private void readLoop(DataInputStream in) {
        byte[] buf = new byte[0xFFFF];
        try {
            while (connected.get()) {
                int type = in.readUnsignedByte();
                int len = in.readUnsignedShort();
                in.readFully(buf, 0, len);
                if (type == T_DATA && listener != null) listener.onPacket(buf, len);
                // keepalive frames only reset the read timeout
            }
        } catch (IOException e) {
            fail(e);
        }
    }

    private void keepaliveLoop() {
        try {
            while (connected.get()) {
                Thread.sleep(Math.max(1000, cfg.keepaliveMs));
                if (connected.get()) writeFrame(T_KEEPALIVE, new byte[0], 0);
            }
        } catch (IOException e) {
            fail(e);
        } catch (InterruptedException ignored) {
        }
    }

    private void fail(Throwable cause) {
        if (!connected.getAndSet(false)) return;
        closeQuietly(socket);
        socket = null;
        if (disconnectListener != null) disconnectListener.onDisconnected(cause);
    }

    private static void closeQuietly(Socket s) {
        if (s == null) return;
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }
}
