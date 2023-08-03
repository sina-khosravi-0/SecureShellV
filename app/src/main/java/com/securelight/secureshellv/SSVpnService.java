package com.securelight.secureshellv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.connection.ConnectionHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SSVpnService extends VpnService {
    public final String TAG = this.getClass().getName();
    public static final String VPN_SERVICE_STOP_BR = "com.securelight.secureshellv.STOP";
    private ParcelFileDescriptor vpnInterface;
    private final Set<String> packages = new HashSet<>();
    private ConnectionHandler connectionHandler;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            System.out.println("AVAILABLE");
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            System.out.println("LOST");
        }

        @Override
        public void onUnavailable() {
            System.out.println("N/A");
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
            if (VPN_SERVICE_STOP_BR.equals(intent.getAction())) {
                try {
                    stopVpnService();
                    Log.i(TAG, "VPN service stopped");
                } catch (IOException e) {
                    Log.e(TAG, "error during stopping VPN service ");
                }
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
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        connectivityManager.requestNetwork(networkRequest, networkCallback);

        // ==== Notification Experiment ====
        mBuilder = new NotificationCompat.Builder(this, "notify_001");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher); //notification icon
        mBuilder.setContentTitle("SSV"); //main title
        mBuilder.setContentText("VPN"); //main text when you "haven't expanded" the notification yet
        mBuilder.setPriority(Notification.PRIORITY_HIGH);
        mBuilder.setVibrate(new long[]{0, 100, 100, 100});
        mBuilder.setOngoing(true);
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("notify_001",
                    "VPN state notification",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 100, 100, 100});
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        if (mNotificationManager != null) {
            Notification notification = mBuilder.build();

            mNotificationManager.notify(1, notification);
            startForeground(1, notification);
        }
        // ==== Notification Experiment ====

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(stopBr, new IntentFilter(VPN_SERVICE_STOP_BR));
        lbm.registerReceiver(yesBr, new IntentFilter("yes__"));
        lbm.registerReceiver(noBr, new IntentFilter("no__"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VpnSettings vpnSettings = (VpnSettings) intent.getSerializableExtra("config");
        startVpn(vpnSettings);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
//        } else {
//            //deprecated in API 26
//            v.vibrate(100);
//        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onRevoke() {
        try {
            stopVpnService();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
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
        connectionHandler = new ConnectionHandler(vpnSettings, vpnInterface, this);
        connectionHandler.start();
    }

    private void addPackagesToExclude() {
        packages.add(getPackageName());
        packages.add("com.server.auditor.ssh.client");
//        packages.add("com.android.chrome");
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
        VpnService.Builder builder = this.new Builder();
        builder.setSession("SSVI");
        builder.addAddress(vpnSettings.getHost(), vpnSettings.getPrefix());
        builder.addDnsServer(vpnSettings.getDnsHost());
//        try {
//            builder.addRoute(InetAddress.getByAddress(new byte[]{}), 32);
//        } catch (UnknownHostException e) {
//            throw new RuntimeException(e);
//        }
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
    }

    public void yes() {
//        connectionHandler.yes();
        mBuilder.setContentTitle("SSV"); //main title
        mBuilder.setContentText(connectionHandler.isConnected() ? "Connected" : "Disconnected"); //main text when you "haven't expanded" the notification yet
        mBuilder.setPriority(Notification.PRIORITY_MIN);
        mBuilder.setSilent(true);
        mNotificationManager.notify(1, mBuilder.build());
    }

    public void no() {
        connectionHandler.no();
//        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
//        bigText.bigText("notificationsTextDetailMode"); //detail mode is the "expanded" notification
//        bigText.setBigContentTitle("notificationTitleDetailMode");
//        bigText.setSummaryText("usuallyAppVersionOrNumberOfNotifications"); //small text under notification
//        mBuilder.setStyle(bigText);
//        mNotificationManager.notify(1, mBuilder.build());
    }


    public void sendNotification() {
        //todo

    }
}