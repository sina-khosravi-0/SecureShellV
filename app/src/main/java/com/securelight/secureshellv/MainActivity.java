package com.securelight.secureshellv;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.VpnService;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndAddPermissions();

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

//        List<String> missingPermissions = new ArrayList<>();
//        requiredPermissions.forEach(perm -> {
//            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
//                missingPermissions.add(perm);
//            }
//        });

        ActivityCompat.requestPermissions(
                this, /*missingPermissions*/requiredPermissions.toArray(new String[0]), 0);
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
        Intent intent = VpnService.prepare(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    public void onStopClicked(View view) {
        Intent intent = new Intent(SSVpnService.VPN_SERVICE_STOP_BR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            startService(getServiceIntent());
        }
        super.onActivityResult(request, result, data);
    }

    private Intent getServiceIntent() {
        return new Intent(this, SSVpnService.class).putExtra("config", vpnSettings)
                /*.setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setPackage(getPackageName())*/;
    }

    // todo: implement app exit sequence
    public void onDestroyClicked(View view) {
        System.exit(0);
    }

    public void onYesClicked(View view) {
        Intent intent = new Intent("yes__");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void onNoClicked(View view) {
        Intent intent = new Intent("no__");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}