package com.securelight.secureshellv;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.connection.ConnectionHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SSVpnService extends VpnService {
    public static final String VPN_SERVICE_STOP_BR = "com.securelight.secureshellv.STOP";
    public static final String VPN_SERVICE_START_BR = "com.securelight.secureshellv.START";
    private final String TAG = this.getClass().getSimpleName();
    private final String notificationChannelID = "onGoing_001";
    private final Set<String> packages = new HashSet<>();
    private final int onGoingNotificationID = 1;
    private final AtomicBoolean serviceActive = new AtomicBoolean();
    private final AtomicBoolean connectionThreadRunning = new AtomicBoolean();
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private ConnectionHandler connectionHandler;
    private ParcelFileDescriptor vpnInterface;
    private NotificationCompat.Action notifStartAction;
    private NotificationCompat.Action notifStopAction;
    private NotificationCompat.Action notifQuitAction;
    private PendingIntent startPendingIntent;
    private PendingIntent stopPendingIntent;
    private PendingIntent quitPendingIntent;
    private Constants.Protocol connectionMethod = Constants.Protocol.TLS_SSH;

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            System.out.println("AVAILABLE");
            if (connectionHandler != null) {
                connectionHandler.setNetworkIFaceAvailable(true);
                connectionHandler.onNetworkAvailable();
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            System.out.println("LOST");
            if (connectionHandler != null) {
                connectionHandler.setNetworkIFaceAvailable(false);
            }
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            System.out.println("N/A");
            if (connectionHandler != null) {
                connectionHandler.setNetworkIFaceAvailable(false);
            }
        }
    };
    private final MyBroadcastReceiver stopBr = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (VPN_SERVICE_STOP_BR.equals(intent.getAction())) {
                try {
                    System.out.println("HUH?");
                    stopVpnService();
                    Log.i(TAG, "VPN service stopped");
                } catch (IOException e) {
                    Log.e(TAG, "error during stopping VPN service ");
                }
            }
        }
    };

    //TODO: remove after implementing database handling
    private final MyBroadcastReceiver directBr = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            connectionMethod = Constants.Protocol.DIRECT_SSH;
        }
    };

    //TODO: remove after implementing database handling
    private final MyBroadcastReceiver tlsBr = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            connectionMethod = Constants.Protocol.TLS_SSH;
        }
    };

    //TODO: remove after implementing database handling
    private final MyBroadcastReceiver dualBr = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            connectionMethod = Constants.Protocol.DUAL_SSH;
        }
    };

    @Override
    public void onCreate() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(stopBr, new IntentFilter(VPN_SERVICE_STOP_BR));
        lbm.registerReceiver(directBr, new IntentFilter("direct__"));
        lbm.registerReceiver(tlsBr, new IntentFilter("tls__"));
        lbm.registerReceiver(dualBr, new IntentFilter("dual__"));

        setupNotification();
    }

    private void setupNotification() {
        notificationBuilder = new NotificationCompat.Builder(this, notificationChannelID);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        initNotifButtonIntents();

        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher); //notification icon
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setCategory(NotificationCompat.CATEGORY_EVENT);

        notifStartAction = new NotificationCompat.Action(R.drawable.start_vpn_notification, "Start", startPendingIntent);
        notifStopAction = new NotificationCompat.Action(R.drawable.stop_vpn_notification, "Stop", stopPendingIntent);
        notifQuitAction = new NotificationCompat.Action(R.drawable.quit_app_icon, "Quit", quitPendingIntent);
        notificationBuilder.addAction(notifQuitAction);

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                new androidx.media.app.NotificationCompat.MediaStyle();
        mediaStyle.setShowActionsInCompactView(0);

        notificationBuilder.setStyle(mediaStyle);


        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel;
        channel = new NotificationChannel(notificationChannelID,
                "VPN Ongoing Channel",
                NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        if (notificationManager != null) {
            Notification notification = notificationBuilder.build();
//            notificationManager.notify(1, notification);
            startForeground(1, notification);
        }
    }

    private void initNotifButtonIntents() {
        Intent startIntent = new Intent(this, MyBroadcastReceiver.class);
        startIntent.setAction(VPN_SERVICE_START_BR);
        startIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        startPendingIntent = PendingIntent.getBroadcast(
                this, 0, startIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MyBroadcastReceiver.class);
        stopIntent.setAction(VPN_SERVICE_STOP_BR);
        stopIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        stopPendingIntent = PendingIntent.getBroadcast(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent quitIntent = new Intent(this, MyBroadcastReceiver.class);
        quitIntent.setAction(MainActivity.EXIT_APP_BR);
        quitIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        quitPendingIntent = PendingIntent.getBroadcast(
                this, 0, quitIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceActive.set(true);
        // set this instance as ssVpnService instance in main activity
        MainActivity.setSsVpnService(this);
        // set notification action buttons
        notificationBuilder.clearActions().addAction(notifStopAction);
        notificationBuilder.addAction(notifQuitAction);

        VpnSettings vpnSettings = new VpnSettings();
        startVpn(vpnSettings);

        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        connectivityManager.requestNetwork(networkRequest, networkCallback);
        return START_STICKY;
    }

    private void startVpn(VpnSettings vpnSettings) {
        VpnService.prepare(this);
        Log.d(TAG, "VPN service prepared");
        try {
            vpnInterface = configureVPNInterface(vpnSettings);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "name not found exception");
        }

        //start connection thread
        if (!connectionThreadRunning.get() && connectionHandler == null || !connectionHandler.isAlive()) {
            connectionHandler = new ConnectionHandler(vpnSettings, vpnInterface, this);
            connectionHandler.setConnectionMethod(connectionMethod);
            connectionHandler.start();
        }
    }

    private void addFilterPackages(SharedPreferencesSingleton preferences) {
        packages.clear();
        packages.addAll(preferences.getFilteredPackages());
    }

    /**
     * Using Host address and Port provided from VpnSettings, the VpnService builder will be supplied
     * to create the desired VPN interface.
     *
     * @return A ParcelFileDescriptor as the vpn interface.
     */
    private ParcelFileDescriptor configureVPNInterface(VpnSettings vpnSettings)
            throws PackageManager.NameNotFoundException {
        Builder builder = this.new Builder();
        builder.setSession("SSV Interface");
        builder.addAddress(vpnSettings.getIFaceAddress(), vpnSettings.getIFacePrefix());
        builder.addDnsServer(vpnSettings.getDnsHost());
        builder.addRoute("0.0.0.0", 0);

        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(this);
        addFilterPackages(preferences);

        switch (preferences.getAppFilterMode()) {
            case INCLUDE:
                for (String p : packages) {
                    System.out.println(p);
                    builder.addAllowedApplication(p);
                }
                break;
            case EXCLUDE:
                for (String p : packages) {
                    builder.addDisallowedApplication(p);
                }
            case OFF:
                builder.addDisallowedApplication(getPackageName()); // disallow this apps
                break;
        }


        builder.setSession(vpnSettings.getIFaceAddress());
        ParcelFileDescriptor vpnInterface = builder.establish();
        Log.d(TAG, "VPN interface configured: " + vpnInterface);
        return vpnInterface;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            System.out.println("fuck destroyed");
            stopVpnService();
        } catch (IOException e) {
            Log.e(TAG, "onDestroy", e);
        }
    }

    @Override
    public void onRevoke() {
        try {
            System.out.println("FUCK revoke");
            stopVpnService();
        } catch (IOException e) {
            Log.e(TAG, "onRevoke", e);
        }
    }

    /**
     * Called when app is exiting and needs to clear the VPN and connections
     */
    public void finalizeAndStop() throws IOException {
        stopVpnService();
        stopForeground(true);
        notificationManager.cancelAll();
        stopSelf();
    }

    public void stopVpnService() throws IOException {
        MainActivity.setSsVpnService(null);
        serviceActive.set(false);
        connectionHandler.interrupt();
        vpnInterface.close();
        notificationBuilder.clearActions().addAction(notifStartAction);
        notificationBuilder.addAction(notifQuitAction);
        notificationManager.notify(onGoingNotificationID, notificationBuilder.build());
    }

    /**
     * Connection Handler will call this method to signal its thread is stopped
     */
    public void onConnectionFinalized() {

    }

    public void yes() {
        connectionHandler.yes();
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

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public NotificationCompat.Builder getNotificationBuilder() {
        return notificationBuilder;
    }

    public int getOnGoingNotificationID() {
        return onGoingNotificationID;
    }

    public String getNotificationChannelID() {
        return notificationChannelID;
    }

    public boolean isServiceActive() {
        return serviceActive.get();
    }

    public void setConnectionThreadRunning(boolean running) {
        connectionThreadRunning.set(running);
    }

    public Constants.Protocol getConnectionMethod() {
        return connectionMethod;
    }

    public void setConnectionMethod(Constants.Protocol connectionMethod) {
        this.connectionMethod = connectionMethod;
    }
}