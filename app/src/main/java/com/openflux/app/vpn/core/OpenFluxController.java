package com.openflux.app.vpn.core;

import android.util.Log;

import com.openflux.app.vpn.util.RetryPolicy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Управляет жизненным циклом адаптера и переподключением с экспоненциальным backoff. */
public class OpenFluxController {
    private static final String TAG = "OpenFluxController";

    /** Слушатель смены состояния (вызывается из фонового потока). */
    public interface StateListener {
        void onStateChanged(ConnectionState state);
    }

    private final OpenFluxAdapter adapter;
    private final OpenFluxConfig config;
    private final ProtectProvider protectProvider;
    private final PacketCallback inboundCallback;
    private final RetryPolicy retryPolicy;
    private final StateListener listener;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread worker;
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;

    public OpenFluxController(OpenFluxAdapter adapter, OpenFluxConfig config,
                              ProtectProvider protectProvider, PacketCallback inboundCallback,
                              RetryPolicy retryPolicy, StateListener listener) {
        this.adapter = adapter;
        this.config = config;
        this.protectProvider = protectProvider;
        this.inboundCallback = inboundCallback;
        this.retryPolicy = retryPolicy;
        this.listener = listener;
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) return;
        worker = new Thread(this::run, "openflux-controller");
        worker.start();
    }

    public void stop() {
        Thread t;
        synchronized (this) {
            if (!running.compareAndSet(true, false)) return;
            t = worker;
            worker = null;
        }
        if (t != null) t.interrupt();
        try {
            adapter.disconnect();
        } catch (RuntimeException e) {
            Log.w(TAG, "disconnect failed", e);
        }
        if (t != null && t != Thread.currentThread()) {
            try {
                t.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        setState(ConnectionState.DISCONNECTED);
    }

    public ConnectionState getState() {
        return state;
    }

    /** Отправить исходящий пакет; при отсутствии соединения пакет отбрасывается. */
    public void sendPacket(byte[] packet, int length) {
        if (state != ConnectionState.CONNECTED) return;
        try {
            adapter.sendPacket(packet, length);
        } catch (IOException e) {
            Log.w(TAG, "sendPacket failed: " + e.getMessage());
        }
    }

    private void run() {
        int attempt = 0;
        boolean first = true;
        while (running.get()) {
            setState(first ? ConnectionState.CONNECTING : ConnectionState.RECONNECTING);
            try {
                adapter.init(config, protectProvider, inboundCallback);
                adapter.connect();
                setState(ConnectionState.CONNECTED);
                attempt = 0;
                first = false;
                // Ждём потери соединения (в заглушке — до остановки).
                while (running.get() && adapter.isConnected()) {
                    Thread.sleep(1000);
                }
                if (!running.get()) break;
                adapter.disconnect();
                first = false;
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "connect failed: " + e.getMessage());
                first = false;
            } catch (InterruptedException e) {
                break;
            }
            if (!running.get()) break;
            setState(ConnectionState.RECONNECTING);
            long delay = retryPolicy.nextDelayMs(attempt++);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void setState(ConnectionState s) {
        state = s;
        if (listener != null) listener.onStateChanged(s);
    }
}
