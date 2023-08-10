package com.securelight.secureshellv;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    VpnSettings vpnSettings = new VpnSettings();
    ApplicationInfo packageInfo;
    public static final String EXIT_APP_BR = "com.securelight.secureshellv.EXIT_APP";
    private static SSVpnService ssVpnService;

    private static boolean appClosing = false;
    private final VpnBroadcastReceiver startBr = new VpnBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startVpn();
        }
    };

    private final VpnBroadcastReceiver exitBr = new VpnBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            exitApp();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Values.CONNECTED_STRING = getString(R.string.connected);
        Values.DISCONNECTED_STRING = getString(R.string.disconnected);
        Values.CONNECTING_STRING = getString(R.string.connecting);
        Values.FULL_INTERNET_ACCESS_STRING = getString(R.string.full_internet_access);
        Values.RESTRICTED_INTERNET_ACCESS_STRING = getString(R.string.restricted_internet_access);
        Values.NO_INTERNET_ACCESS_STRING = getString(R.string.no_internet_access);
        Values.NETWORK_UNAVAILABLE_STRING = getString(R.string.network_unavailable);
        Values.INTERNET_ACCESS_STATE_STRING = getString(R.string.internet_access_state);

        checkAndAddPermissions();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(startBr, new IntentFilter(SSVpnService.VPN_SERVICE_START_BR));
        lbm.registerReceiver(exitBr, new IntentFilter(EXIT_APP_BR));
        try {
            //set app package info
            packageInfo = this.getPackageManager().getApplicationInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndAddPermissions() {
        List<String> requiredPermissions = new ArrayList<>(Arrays.asList(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.VIBRATE));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> missingPermissions = new ArrayList<>();
        requiredPermissions.forEach(perm -> {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        });
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, missingPermissions/*requiredPermissions*/.toArray(new String[0]), 0);
        }
    }

    public void onCheckClicked(View view) throws MalformedURLException {
        Toast myToast = Toast.makeText(this, "connected", Toast.LENGTH_SHORT);

        String urlString = "https://checkip.amazonaws.com/";
        URL url = new URL(urlString);
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                myToast.setText(br.readLine());
                myToast.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void onStartClicked(View view) {
        // ssVpnService being null means it's not started since its instance is passed when it's started
        if (ssVpnService == null) {
            startVpn();
        }

    }

    private void startVpn() {
        Intent intent = VpnService.prepare(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    public void onStopClicked(View view) {
        if (ssVpnService != null && ssVpnService.isServiceActive()) {
            try {
                ssVpnService.stopVpnService();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Intent intent = getServiceIntent();
            startService(intent);
        }
        super.onActivityResult(request, result, data);
    }


    private Intent getServiceIntent() {
        return new Intent(this, SSVpnService.class).putExtra("config", vpnSettings)
                .setAction(Intent.ACTION_VIEW)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setPackage(getPackageName());
    }

    /**
     * REMOVE
     * only for debugging.
     */
    public void onDestroyClicked(View view) {
        exitApp();
    }

    // todo: implement app exit sequence
    private void exitApp() {
        appClosing = true;
        ssVpnService.stopForeground(Service.STOP_FOREGROUND_REMOVE);
        try {
            ssVpnService.finalizeAndStop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finishAndRemoveTask();
        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
    }

    public void onYesClicked(View view) {
        ssVpnService.yes();
    }

    public void onNoClicked(View view) {
        ssVpnService.no();
    }

    //todo: implement on low memory
    @Override
    protected void onResume() {
        System.out.println("resume");
        super.onResume();
    }

    public static void setSsVpnService(SSVpnService ssVpnService) {
        MainActivity.ssVpnService = ssVpnService;
    }

    public static boolean isAppClosing() {
        return appClosing;
    }

}