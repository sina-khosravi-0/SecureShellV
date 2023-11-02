package com.securelight.secureshellv.vpnservice;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static com.securelight.secureshellv.statics.Constants.CREDIT_EXPIRED_CODE_STRING;
import static com.securelight.secureshellv.statics.Constants.apiHeartbeatPeriod;

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
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.MainActivity;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.statics.Values;
import com.securelight.secureshellv.utility.NotificationBroadcastReceiver;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;
import com.securelight.secureshellv.vpnservice.connection.APIHeartbeatHandler;
import com.securelight.secureshellv.vpnservice.connection.ConnectionHandler;
import com.securelight.secureshellv.vpnservice.connection.ConnectionState;
import com.securelight.secureshellv.vpnservice.connection.NetworkState;
import com.securelight.secureshellv.vpnservice.listeners.NotificationListener;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

public class SSVpnService extends VpnService {
    public static final String STOP_VPN_SERVICE_ACTION = "com.securelight.secureshellv.STOP";
    public static final String START_VPN_ACTION = "com.securelight.secureshellv.START";
    public static final String CONNECTED_ACTION = "com.securelight.secureshellv.CONNECTED";
    public static final String CONNECTING_ACTION = "com.securelight.secureshellv.CONNECTING";
    public static final String DISCONNECTED_ACTION = "com.securelight.secureshellv.DISCONNECTED";
    private final String TAG = this.getClass().getSimpleName();
    static final String vpnServiceAction = "android.net.VpnService";
    private final String notificationChannelID = "onGoing_001";
    private final Set<String> packages = new HashSet<>();
    private final int onGoingNotificationID = 1;
    private final AtomicBoolean serviceActive = new AtomicBoolean();
    private long startedAt;
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
    private Timer apiHeartbeatTimer;
    private Constants.Protocol connectionMethod = Constants.Protocol.DIRECT_SSH;
    private final NotificationListener notificationListener = (networkState, connectionState) -> {
        if (!MainActivity.isAppClosing()) {
            NotificationCompat.Builder builder = getNotificationBuilder();
            builder.setContentTitle(connectionState.value);
            builder.setContentText(String
                    .format("%s: %s", Values.INTERNET_ACCESS_STATE_STRING, networkState.value));
            getNotificationManager().notify(getOnGoingNotificationID(), builder.build());
            builder.setSilent(true);
        }
    };
    private final IBinder binder = new VpnServiceBinder();

    public class VpnServiceBinder extends Binder {

        public SSVpnService getService() {
            return SSVpnService.this;
        }

        public void stopService() {
            finalizeAndStop();
        }

        public ConnectionState getConnectionState() {
            try {
                return connectionHandler.getConnectionState();
            } catch (NullPointerException e) {
                return ConnectionState.DISCONNECTED;
            }

        }

        public NetworkState getNetworkState() {
            return Objects.requireNonNullElse(connectionHandler.getNetworkState(),
                    NetworkState.NONE);
        }
    }

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            if (connectionHandler != null) {
                connectionHandler.setNetworkIFaceAvailable(true);
                connectionHandler.onNetworkAvailable();
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            if (connectionHandler != null) {
                connectionHandler.setNetworkIFaceAvailable(false);
                connectionHandler.onNetworkLost();
            }
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            if (connectionHandler != null) {
                connectionHandler.setNetworkIFaceAvailable(false);
                connectionHandler.onNetworkLost();
            }
        }
    };
    private final BroadcastReceiver stopBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (STOP_VPN_SERVICE_ACTION.equals(intent.getAction())) {
                stopVpnService(intent.getBooleanExtra(Constants.OUT_OF_TRAFFIC_CODE_STRING, false),
                        intent.getBooleanExtra(Constants.CREDIT_EXPIRED_CODE_STRING, false));
                Log.i(TAG, "VPN service stopped");
            }
        }
    };

    private final BroadcastReceiver connectedBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private final BroadcastReceiver connectingBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private final BroadcastReceiver disconnectedBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    @Override
    public void onCreate() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(stopBr, new IntentFilter(STOP_VPN_SERVICE_ACTION));
        lbm.registerReceiver(connectedBr, new IntentFilter(CONNECTED_ACTION));
        lbm.registerReceiver(connectingBr, new IntentFilter(CONNECTING_ACTION));
        lbm.registerReceiver(disconnectedBr, new IntentFilter(DISCONNECTED_ACTION));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!serviceActive.get()) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "SecureShellV::MyWakelockTag");
            wakeLock.acquire(20 * 60 * 1000L /*10 minutes*/);
            startedAt = System.currentTimeMillis();
            serviceActive.set(true);

            setupNotification();
            // set notification action buttons
            notificationBuilder.clearActions().addAction(notifStopAction);
            notificationBuilder.addAction(notifQuitAction);

            startVpn();

            ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
            connectivityManager.requestNetwork(networkRequest, networkCallback);
        }
        return START_STICKY;
    }

    private void startVpn() {
        VpnService.prepare(this);
        Log.d(TAG, "VPN service prepared");

        vpnInterface = establishVPNInterface();

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(vpnServiceAction)
                .putExtra("vpn_interface", vpnInterface));

        //start connection thread
        apiHeartbeatTimer = new Timer();
        apiHeartbeatTimer.schedule(new APIHeartbeatHandler(this), 0, apiHeartbeatPeriod);
        connectionHandler = new ConnectionHandler(vpnInterface, this, notificationListener);
        connectionHandler.setConnectionMethod(connectionMethod);
        connectionHandler.start();
    }

    /**
     * Using Host address and Port provided from VpnSettings, the VpnService builder will create
     * a VPN interface.
     *
     * @return A ParcelFileDescriptor as the vpn interface.
     */
    private ParcelFileDescriptor establishVPNInterface() {
        Builder builder = this.new Builder();
        builder.setSession("SSV Interface");
        builder.addAddress(VpnSettings.iFaceAddress, VpnSettings.iFacePrefix);
        builder.addDnsServer(VpnSettings.dnsHost);
        builder.addRoute("0.0.0.0", 0);

        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(this);
        addFilterPackages(preferences);

        try {
            switch (preferences.getAppFilterMode()) {
                case INCLUDE:
                    for (String p : packages) {
                        builder.addAllowedApplication(p);
                    }
                    break;
                case EXCLUDE:
                    for (String p : packages) {
                        builder.addDisallowedApplication(p);
                    }
                case OFF:
                    builder.addDisallowedApplication(getPackageName()); // disallow self
                    break;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "name not found exception");
        }


        builder.setSession(VpnSettings.iFaceAddress);
        ParcelFileDescriptor vpnInterface = builder.establish();
        Log.d(TAG, "VPN interface configured: " + vpnInterface);
        return vpnInterface;
    }

    private void addFilterPackages(SharedPreferencesSingleton preferences) {
        packages.clear();
        packages.addAll(preferences.getFilteredPackages());
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
            startForeground(1, notification);
        }
    }

    private void initNotifButtonIntents() {
        Intent startIntent = new Intent(this, NotificationBroadcastReceiver.class);
        startIntent.setAction(START_VPN_ACTION);
        startIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        startPendingIntent = PendingIntent.getBroadcast(
                this, 0, startIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, NotificationBroadcastReceiver.class);
        stopIntent.setAction(STOP_VPN_SERVICE_ACTION);
        stopIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        stopPendingIntent = PendingIntent.getBroadcast(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent quitIntent = new Intent(this, NotificationBroadcastReceiver.class);
        quitIntent.setAction(MainActivity.EXIT_APP_ACTION);
        quitIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        quitPendingIntent = PendingIntent.getBroadcast(
                this, 0, quitIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        stopVpnService();
    }

    @Override
    public void onRevoke() {
        stopVpnService();
    }

    @Override
    public void onLowMemory() {
        System.out.println("LOW MEMORY");
    }

    /**
     * Called when app is exiting and needs to clear the VPN and connections
     */
    public void finalizeAndStop() {
        stopVpnService();
        stopForeground(true);
        notificationManager.cancelAll();
        stopSelf();
    }


    private void stopVpnService(boolean insufficient_traffic, boolean credit_expired) {
        if (insufficient_traffic) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                    new Intent(Intents.INSUFFICIENT_TRAFFIC_INTENT));
        }
        if (credit_expired) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                    new Intent(Intents.CREDIT_EXPIRED_INTENT));
        }
        try {
            apiHeartbeatTimer.cancel();
            serviceActive.set(false);
            connectionHandler.interrupt();
            connectionHandler.join();
            notificationBuilder.clearActions().addAction(notifStartAction);
            notificationBuilder.addAction(notifQuitAction);
            notificationManager.notify(onGoingNotificationID, notificationBuilder.build());
        } catch (NullPointerException | InterruptedException e) {
            Log.e(TAG, "Fuck Null");
        }
    }

    public void stopVpnService() {
        stopVpnService(false, false);
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

    public Constants.Protocol getConnectionMethod() {
        return connectionMethod;
    }

    public void setConnectionMethod(Constants.Protocol connectionMethod) {
        this.connectionMethod = connectionMethod;
    }
}