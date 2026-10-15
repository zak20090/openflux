package com.openflux.app.vpn.tun;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** Двунаправленная перекачка пакетов: TUN -> sink (исходящие) и inject() -> TUN (входящие). */
public class TunPump {
    private static final String TAG = "TunPump";

    public interface OutboundSink {
        void onOutboundPacket(byte[] packet, int length);
    }

    private final ParcelFileDescriptor tun;
    private final FileInputStream in;
    private final FileOutputStream out;
    private final OutboundSink sink;
    private final int mtu;
    private volatile boolean running;
    private Thread thread;

    public TunPump(ParcelFileDescriptor tun, int mtu, OutboundSink sink) {
        this.tun = tun;
        this.in = new FileInputStream(tun.getFileDescriptor());
        this.out = new FileOutputStream(tun.getFileDescriptor());
        this.mtu = mtu;
        this.sink = sink;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::readLoop, "tun-pump");
        thread.start();
    }

    public void stop() {
        Thread t;
        synchronized (this) {
            running = false;
            t = thread;
            thread = null;
        }
        // Закрытие дескриптора прерывает блокирующий read().
        try {
            in.close();
        } catch (IOException ignored) {
        }
        try {
            out.close();
        } catch (IOException ignored) {
        }
        try {
            tun.close();
        } catch (IOException ignored) {
        }
        if (t != null && t != Thread.currentThread()) {
            t.interrupt();
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Записать входящий пакет в TUN. */
    public void inject(byte[] packet, int length) {
        if (!running) return;
        try {
            synchronized (out) {
                out.write(packet, 0, length);
            }
        } catch (IOException e) {
            Log.w(TAG, "inject failed: " + e.getMessage());
        }
    }

    private void readLoop() {
        byte[] buf = new byte[Math.max(mtu, 1500) + 100];
        while (running) {
            try {
                int n = in.read(buf);
                if (n > 0) sink.onOutboundPacket(buf, n);
                else if (n < 0) break;
            } catch (IOException e) {
                if (running) Log.w(TAG, "read failed: " + e.getMessage());
                break;
            }
        }
    }
}
