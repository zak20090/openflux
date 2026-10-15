package com.openflux.app.vpn.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Профиль в SharedPreferences. Секреты (authToken, privateKey) лежат отдельно в
 * EncryptedSharedPreferences (ключ в Android Keystore).
 */
public class ProfileStore {
    private static final String TAG = "ProfileStore";
    private static final String PREFS = "openflux_profile";
    private static final String SECRET_PREFS = "openflux_secrets";
    private final SharedPreferences prefs;
    private final SharedPreferences secrets;

    public ProfileStore(Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        secrets = openSecrets(app);
    }

    private static SharedPreferences openSecrets(Context app) {
        try {
            return create(app);
        } catch (Exception e) {
            // Повреждённое хранилище/ключ (напр. после восстановления): сбрасываем секреты.
            Log.e(TAG, "secret store unreadable, resetting", e);
            app.deleteSharedPreferences(SECRET_PREFS);
            try {
                return create(app);
            } catch (Exception e2) {
                throw new IllegalStateException("Keystore-backed storage unavailable", e2);
            }
        }
    }

    private static SharedPreferences create(Context app) throws Exception {
        MasterKey key = new MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        return EncryptedSharedPreferences.create(app, SECRET_PREFS, key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public VpnProfile load() {
        VpnProfile d = new VpnProfile();
        VpnProfile p = new VpnProfile();
        p.host = prefs.getString("host", d.host);
        p.port = prefs.getInt("port", d.port);
        p.transport = prefs.getString("transport", d.transport);
        p.publicKey = prefs.getString("publicKey", d.publicKey);
        p.keyRef = prefs.getString("keyRef", d.keyRef);
        p.sni = prefs.getString("sni", d.sni);
        p.alpn = prefs.getString("alpn", d.alpn);
        p.fullTunnel = prefs.getBoolean("fullTunnel", d.fullTunnel);
        p.routes = split(prefs.getString("routes", join(d.routes)));
        p.dns = split(prefs.getString("dns", join(d.dns)));
        p.tunAddress = prefs.getString("tunAddress", d.tunAddress);
        p.tunPrefix = prefs.getInt("tunPrefix", d.tunPrefix);
        p.tunAddress6 = prefs.getString("tunAddress6", d.tunAddress6);
        p.tunPrefix6 = prefs.getInt("tunPrefix6", d.tunPrefix6);
        p.mtu = prefs.getInt("mtu", d.mtu);
        p.connectTimeoutMs = prefs.getInt("connectTimeoutMs", d.connectTimeoutMs);
        p.readTimeoutMs = prefs.getInt("readTimeoutMs", d.readTimeoutMs);
        p.keepaliveMs = prefs.getInt("keepaliveMs", d.keepaliveMs);
        p.killSwitch = prefs.getBoolean("killSwitch", d.killSwitch);
        p.authToken = secrets.getString("authToken", "");
        p.privateKey = secrets.getString("privateKey", "");
        return p;
    }

    public void save(VpnProfile p) {
        prefs.edit()
                .putString("host", p.host)
                .putInt("port", p.port)
                .putString("transport", p.transport)
                .putString("publicKey", p.publicKey)
                .putString("keyRef", p.keyRef)
                .putString("sni", p.sni)
                .putString("alpn", p.alpn)
                .putBoolean("fullTunnel", p.fullTunnel)
                .putString("routes", join(p.routes))
                .putString("dns", join(p.dns))
                .putString("tunAddress", p.tunAddress)
                .putInt("tunPrefix", p.tunPrefix)
                .putString("tunAddress6", p.tunAddress6)
                .putInt("tunPrefix6", p.tunPrefix6)
                .putInt("mtu", p.mtu)
                .putInt("connectTimeoutMs", p.connectTimeoutMs)
                .putInt("readTimeoutMs", p.readTimeoutMs)
                .putInt("keepaliveMs", p.keepaliveMs)
                .putBoolean("killSwitch", p.killSwitch)
                .apply();
        secrets.edit()
                .putString("authToken", p.authToken)
                .putString("privateKey", p.privateKey)
                .apply();
    }

    private static String join(List<String> l) {
        return String.join(",", l);
    }

    private static List<String> split(String s) {
        ArrayList<String> out = new ArrayList<>();
        for (String x : Arrays.asList(s.split(","))) {
            if (!x.trim().isEmpty()) out.add(x.trim());
        }
        return out;
    }
}
