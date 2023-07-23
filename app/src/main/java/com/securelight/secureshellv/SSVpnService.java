package com.securelight.secureshellv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SSVpnService extends VpnService {
    public final String TAG = this.getClass().getName();
    public static boolean isInternetAccessible = true;
    private ParcelFileDescriptor vpnInterface;
    private SSVpnService vpnService;
    private final Set<String> packages = new HashSet<>();

    private ConnectionHandler connectionHandler;

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            isInternetAccessible = true;
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            isInternetAccessible = false;
        }

        @Override
        public void onUnavailable() {
            isInternetAccessible = false;
            super.onUnavailable();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
        }
    };
    private final BroadcastReceiver stopBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("stop_vpn_service".equals(intent.getAction())) {
                try {
                    stopVpnService();
                    Log.i(TAG, "VPN service stopped");
                } catch (IOException e) {
                    Log.e(TAG, "Error during stopping VPN service ");
                }
                stopSelf();
            }
        }
    };

    private final BroadcastReceiver yesBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("yes__".equals(intent.getAction())) {
                yes();
            }
        }
    };

    private final BroadcastReceiver noBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("no__".equals(intent.getAction())) {
                no();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        vpnService = this;
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        connectivityManager.requestNetwork(networkRequest, networkCallback);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(stopBr, new IntentFilter("stop_vpn_service"));
        lbm.registerReceiver(yesBr, new IntentFilter("yes__"));
        lbm.registerReceiver(noBr, new IntentFilter("no__"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VpnSettings vpnSettings = (VpnSettings) intent.getSerializableExtra("config");
        startVpn(vpnSettings);

        return super.onStartCommand(intent, flags, startId);
    }

    private void startVpn(VpnSettings vpnSettings) {
        addPackagesToExclude();
        VpnService.prepare(this);
        Log.d(TAG, "VPN service prepared");
        try {
            vpnInterface = configureVPNInterface(vpnSettings);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "name not found exception");
        }
        startConnectionHandler(vpnSettings);
    }

    private void startConnectionHandler(VpnSettings vpnSettings) {
        connectionHandler = new ConnectionHandler(vpnSettings, vpnInterface, getApplication());
        connectionHandler.start();
    }

    private void addPackagesToExclude() {
        packages.add(getPackageName());
        packages.add("com.server.auditor.ssh.client");
        // todo: add other packages to exclude

    }

    /**
     * Using Host address and Port provided from VpnSettings, the VpnService builder will be supplied
     * to create the desired VPN interface.
     *
     * @return A ParcelFileDescriptor as the vpn interface.
     */
    private ParcelFileDescriptor configureVPNInterface(VpnSettings vpnSettings)
            throws PackageManager.NameNotFoundException {
        VpnService.Builder builder = vpnService.new Builder();
        builder.addAddress(vpnSettings.getHost(), 8);
        builder.addDnsServer(vpnSettings.getDnsHost());
        builder.addRoute("0.0.0.0", 0);
        for (String p : packages) {
            builder.addDisallowedApplication(p);
        }
        builder.setSession(vpnSettings.getHost());
        ParcelFileDescriptor vpnInterface = builder.establish();
        Log.d(TAG, "VPN interface configured: " + vpnInterface);
        return vpnInterface;
    }

    private void stopVpnService() throws IOException {
        connectionHandler.interrupt();
        vpnInterface.close();
        stopForeground(true);
    }

    public void yes() {
        connectionHandler.reconnect();
    }

    public void no() {
        connectionHandler.disconnect();
    }

}