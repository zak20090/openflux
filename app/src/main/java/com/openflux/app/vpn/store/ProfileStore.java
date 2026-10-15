package com.openflux.app.vpn.store;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Хранилище профиля в SharedPreferences.
 * TODO: для приватных ключей использовать Android Keystore / EncryptedSharedPreferences.
 */
public class ProfileStore {
    private static final String PREFS = "openflux_profile";
    private final SharedPreferences prefs;

    public ProfileStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public VpnProfile load() {
        VpnProfile d = new VpnProfile();
        VpnProfile p = new VpnProfile();
        p.host = prefs.getString("host", d.host);
        p.port = prefs.getInt("port", d.port);
        p.transports = split(prefs.getString("transports", join(d.transports)));
        p.publicKey = prefs.getString("publicKey", "");
        p.privateKey = prefs.getString("privateKey", "");
        p.routes = split(prefs.getString("routes", join(d.routes)));
        p.dns = split(prefs.getString("dns", join(d.dns)));
        p.tunAddress = prefs.getString("tunAddress", d.tunAddress);
        p.tunPrefix = prefs.getInt("tunPrefix", d.tunPrefix);
        p.mtu = prefs.getInt("mtu", d.mtu);
        return p;
    }

    public void save(VpnProfile p) {
        prefs.edit()
                .putString("host", p.host)
                .putInt("port", p.port)
                .putString("transports", join(p.transports))
                .putString("publicKey", p.publicKey)
                .putString("privateKey", p.privateKey)
                .putString("routes", join(p.routes))
                .putString("dns", join(p.dns))
                .putString("tunAddress", p.tunAddress)
                .putInt("tunPrefix", p.tunPrefix)
                .putInt("mtu", p.mtu)
                .apply();
    }

    private static String join(java.util.List<String> l) {
        return String.join(",", l);
    }

    private static java.util.List<String> split(String s) {
        ArrayList<String> out = new ArrayList<>();
        for (String x : Arrays.asList(s.split(","))) {
            if (!x.trim().isEmpty()) out.add(x.trim());
        }
        return out;
    }
}
