package com.securelight.secureshellv;

import android.app.admin.NetworkEvent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.SSH.SSHConnectionManager;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
import org.apache.sshd.common.Service;
import org.apache.sshd.common.future.CancelOption;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionDisconnectHandler;
import org.apache.sshd.common.session.helpers.TimeoutIndicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {


    VPNSettings vpnSettings = new VPNSettings();
    boolean connected = false;
    ApplicationInfo packageInfo;
    private SSHConnectionManager sshConnectionManager;

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
}