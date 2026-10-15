package com.openflux.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.openflux.app.R;
import com.openflux.app.vpn.store.ProfileStore;
import com.openflux.app.vpn.store.ProfileValidator;
import com.openflux.app.vpn.store.VpnProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Редактор профиля. Секреты сохраняются в Keystore-backed хранилище (см. ProfileStore). */
public class ProfileEditorActivity extends Activity {
    private ProfileStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Не показывать секреты в скриншотах/recents.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_profile_editor);
        store = new ProfileStore(this);
        VpnProfile p = store.load();
        set(R.id.f_host, p.host);
        set(R.id.f_port, String.valueOf(p.port));
        set(R.id.f_transport, p.transport);
        set(R.id.f_authToken, p.authToken);
        set(R.id.f_privateKey, p.privateKey);
        set(R.id.f_publicKey, p.publicKey);
        set(R.id.f_keyRef, p.keyRef);
        set(R.id.f_sni, p.sni);
        set(R.id.f_alpn, p.alpn);
        set(R.id.f_mtu, String.valueOf(p.mtu));
        set(R.id.f_dns, String.join(",", p.dns));
        set(R.id.f_routes, String.join(",", p.routes));
        set(R.id.f_tunAddress, p.tunAddress);
        set(R.id.f_tunPrefix, String.valueOf(p.tunPrefix));
        set(R.id.f_tunAddress6, p.tunAddress6);
        set(R.id.f_tunPrefix6, String.valueOf(p.tunPrefix6));
        set(R.id.f_connectTimeoutMs, String.valueOf(p.connectTimeoutMs));
        set(R.id.f_readTimeoutMs, String.valueOf(p.readTimeoutMs));
        set(R.id.f_keepaliveMs, String.valueOf(p.keepaliveMs));
        ((CheckBox) findViewById(R.id.f_fullTunnel)).setChecked(p.fullTunnel);
        ((CheckBox) findViewById(R.id.f_killSwitch)).setChecked(p.killSwitch);
        findViewById(R.id.saveButton).setOnClickListener(v -> save());
    }

    private void save() {
        VpnProfile p = new VpnProfile(); // значения по умолчанию для пустых полей
        p.host = get(R.id.f_host);
        p.port = num(R.id.f_port, p.port);
        p.transport = get(R.id.f_transport, p.transport);
        p.authToken = get(R.id.f_authToken, p.authToken);
        p.privateKey = get(R.id.f_privateKey, p.privateKey);
        p.publicKey = get(R.id.f_publicKey, p.publicKey);
        p.keyRef = get(R.id.f_keyRef, p.keyRef);
        p.sni = get(R.id.f_sni, p.sni);
        p.alpn = get(R.id.f_alpn, p.alpn);
        p.mtu = num(R.id.f_mtu, p.mtu);
        List<String> dns = list(R.id.f_dns);
        if (!dns.isEmpty()) p.dns = dns;
        List<String> routes = list(R.id.f_routes);
        if (!routes.isEmpty()) p.routes = routes;
        p.tunAddress = get(R.id.f_tunAddress, p.tunAddress);
        p.tunPrefix = num(R.id.f_tunPrefix, p.tunPrefix);
        p.tunAddress6 = get(R.id.f_tunAddress6, p.tunAddress6);
        p.tunPrefix6 = num(R.id.f_tunPrefix6, p.tunPrefix6);
        p.connectTimeoutMs = num(R.id.f_connectTimeoutMs, p.connectTimeoutMs);
        p.readTimeoutMs = num(R.id.f_readTimeoutMs, p.readTimeoutMs);
        p.keepaliveMs = num(R.id.f_keepaliveMs, p.keepaliveMs);
        p.fullTunnel = ((CheckBox) findViewById(R.id.f_fullTunnel)).isChecked();
        p.killSwitch = ((CheckBox) findViewById(R.id.f_killSwitch)).isChecked();
        p.transport = p.transport.toLowerCase();

        List<String> errors = ProfileValidator.validate(p);
        if (!errors.isEmpty()) {
            Toast.makeText(this, String.join("\n", errors), Toast.LENGTH_LONG).show();
            return;
        }
        store.save(p);
        Toast.makeText(this, "Saved. Reconnect to apply.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void set(int id, String v) {
        ((EditText) findViewById(id)).setText(v);
    }

    private String get(int id) {
        return ((EditText) findViewById(id)).getText().toString().trim();
    }

    private String get(int id, String def) {
        String s = get(id);
        return s.isEmpty() ? def : s;
    }

    private int num(int id, int def) {
        String s = get(id);
        if (s.isEmpty()) return def;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1; // не пройдёт валидацию
        }
    }

    private List<String> list(int id) {
        List<String> out = new ArrayList<>();
        for (String s : Arrays.asList(get(id).split(","))) {
            if (!s.trim().isEmpty()) out.add(s.trim());
        }
        return out;
    }
}
