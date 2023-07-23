package com.securelight.secureshellv;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.ssh.SSHConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends AppCompatActivity {


    VpnSettings vpnSettings = new VpnSettings();
    boolean connected = false;
    ApplicationInfo packageInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            //set app package info
            packageInfo = this.getPackageManager().getApplicationInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static long initialTx = 0;
    public static long newTx = 0;
    public long received = 0;

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
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }

    }

    public void onStopClicked(View view) {
        Intent intent = new Intent("stop_vpn_service");
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
        return new Intent(this, SSVpnService.class).putExtra("config", vpnSettings);
    }

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