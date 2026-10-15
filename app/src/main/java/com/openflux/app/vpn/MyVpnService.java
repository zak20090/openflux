package com.openflux.app.vpn;

import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.openflux.app.vpn.core.ConnectionState;
import com.openflux.app.vpn.core.OpenFluxAdapterImpl;
import com.openflux.app.vpn.core.OpenFluxConfig;
import com.openflux.app.vpn.core.OpenFluxController;
import com.openflux.app.vpn.core.ProtectProvider;
import com.openflux.app.vpn.store.ProfileStore;
import com.openflux.app.vpn.store.VpnProfile;
import com.openflux.app.vpn.tun.TunPump;
import com.openflux.app.vpn.util.CidrUtil;
import com.openflux.app.vpn.util.RetryPolicy;

import java.net.DatagramSocket;
import java.net.Socket;

public class MyVpnService extends VpnService {
    private static final String TAG = "MyVpnService";
    public static final String ACTION_START = "com.openflux.app.START";
    public static final String ACTION_STOP = "com.openflux.app.STOP";
    public static final String ACTION_STATE = "com.openflux.app.STATE";
    public static final String EXTRA_STATE = "state";

    private ParcelFileDescriptor tun;
    private TunPump pump;
    private OpenFluxController controller;

    private final ProtectProvider protectProvider = new ProtectProvider() {
        @Override public boolean protect(Socket s) { return MyVpnService.this.protect(s); }
        @Override public boolean protect(DatagramSocket s) { return MyVpnService.this.protect(s); }
        @Override public boolean protect(int fd) { return MyVpnService.this.protect(fd); }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn();
            stopSelf();
            return START_NOT_STICKY;
        }
        VpnNotifFactory.ensureChannel(this);
        startForeground(VpnNotifFactory.NOTIFICATION_ID,
                VpnNotifFactory.build(this, "Connecting…"));
        startVpn();
        return START_STICKY;
    }

    private synchronized void startVpn() {
        if (tun != null) return;
        VpnProfile profile = new ProfileStore(this).load();

        Builder b = new Builder().setSession("OpenFlux").setMtu(profile.mtu);
        try {
            b.addAddress(profile.tunAddress, profile.tunPrefix);
            for (String r : profile.routes) {
                CidrUtil.Cidr c = CidrUtil.parse(r);
                if (c == null) {
                    Log.w(TAG, "skip invalid route: " + r);
                    continue;
                }
                b.addRoute(c.address, c.prefix);
            }
            for (String d : profile.dns) b.addDnsServer(d);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "invalid profile", e);
            stopVpn();
            stopSelf();
            return;
        }

        tun = b.establish();
        if (tun == null) {
            Log.e(TAG, "establish() returned null (permission revoked?)");
            stopSelf();
            return;
        }

        controller = new OpenFluxController(new OpenFluxAdapterImpl(),
                OpenFluxConfig.fromProfile(profile), protectProvider,
                (pkt, len) -> {
                    TunPump p = pump;
                    if (p != null) p.inject(pkt, len);
                },
                RetryPolicy.defaultPolicy(), this::onState);
        pump = new TunPump(tun, profile.mtu, (pkt, len) -> controller.sendPacket(pkt, len));
        pump.start();
        controller.start();
    }

    private void onState(ConnectionState s) {
        sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName())
                .putExtra(EXTRA_STATE, s.name()));
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(VpnNotifFactory.NOTIFICATION_ID, VpnNotifFactory.build(this, s.name()));
        }
    }

    /** Идемпотентная очистка всех ресурсов. */
    private synchronized void stopVpn() {
        if (controller != null) {
            controller.stop();
            controller = null;
        }
        if (pump != null) {
            pump.stop();
            pump = null;
        }
        if (tun != null) {
            try {
                tun.close();
            } catch (Exception ignored) {
            }
            tun = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override
    public void onRevoke() {
        stopVpn();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName())
                .putExtra(EXTRA_STATE, ConnectionState.DISCONNECTED.name()));
        super.onDestroy();
    }
}
