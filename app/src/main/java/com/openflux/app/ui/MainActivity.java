package com.openflux.app.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.openflux.app.R;
import com.openflux.app.vpn.MyVpnService;

public class MainActivity extends Activity {
    private static final int REQ_VPN = 1;
    private TextView stateText;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            stateText.setText("State: " + i.getStringExtra(MyVpnService.EXTRA_STATE));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stateText = findViewById(R.id.stateText);
        findViewById(R.id.connectButton).setOnClickListener(v -> connect());
        findViewById(R.id.editProfileButton).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileEditorActivity.class)));
        findViewById(R.id.disconnectButton).setOnClickListener(v -> disconnect());
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 2);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(MyVpnService.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, f);
        }
    }

    @Override
    protected void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
    }

    private void connect() {
        Intent prepare = VpnService.prepare(this);
        if (prepare != null) {
            startActivityForResult(prepare, REQ_VPN);
        } else {
            startVpnService();
        }
    }

    private void disconnect() {
        startService(new Intent(this, MyVpnService.class).setAction(MyVpnService.ACTION_STOP));
    }

    private void startVpnService() {
        startForegroundService(new Intent(this, MyVpnService.class)
                .setAction(MyVpnService.ACTION_START));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VPN && resultCode == RESULT_OK) startVpnService();
    }
}
