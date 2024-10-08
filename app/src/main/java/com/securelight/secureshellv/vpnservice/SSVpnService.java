package com.securelight.secureshellv.vpnservice;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.ui.homepage.HomepageActivity;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.statics.Values;
import com.securelight.secureshellv.utility.NotificationBroadcastReceiver;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;
import com.securelight.secureshellv.vpnservice.connection.ConnectionHandler;
import com.securelight.secureshellv.vpnservice.connection.ConnectionState;
import com.securelight.secureshellv.vpnservice.connection.NetworkState;
import com.securelight.secureshellv.vpnservice.listeners.NotificationListener;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.dev7.lib.v2ray.core.Tun2SocksExecutor;
import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;
import dev.dev7.lib.v2ray.interfaces.Tun2SocksListener;
import dev.dev7.lib.v2ray.interfaces.V2rayServicesListener;
import dev.dev7.lib.v2ray.utils.V2rayConstants;

public class SSVpnService extends VpnService implements V2rayServicesListener, Tun2SocksListener {
    static final String vpnServiceAction = "android.net.VpnService";
    private final String TAG = this.getClass().getSimpleName();
    private final String notificationChannelID = "onGoing_001";
    private final Set<String> packages = new HashSet<>();
    private final int onGoingNotificationID = 1;
    private final AtomicBoolean serviceActive = new AtomicBoolean();
    private final IBinder binder = new VpnServiceBinder();
    private boolean serviceCreated = false;
    private V2rayCoreExecutor v2rayCoreExecutor;
    private Tun2SocksExecutor tun2SocksExecutor;
    private StatsHandler statsHandler;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final NotificationListener notificationListener = (networkState, connectionState) -> {
        if (!HomepageActivity.isAppClosing()) {
            NotificationCompat.Builder builder = getNotificationBuilder();
            builder.setContentTitle(connectionState.value);
            if (networkState != null) {
                builder.setContentText(String
                        .format("%s: %s", Values.INTERNET_ACCESS_STATE_STRING, networkState.value));

            } else {
                builder.setContentText("");
            }
            getNotificationManager().notify(getOnGoingNotificationID(), builder.build());
            builder.setSilent(true);
        }
    };
    private ConnectionHandler connectionHandler;
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
    private ParcelFileDescriptor vpnInterface;
    private NotificationCompat.Action notifStartAction;
    private NotificationCompat.Action notifStopAction;
    private NotificationCompat.Action notifQuitAction;
    private PendingIntent startPendingIntent;
    private PendingIntent stopPendingIntent;
    private PendingIntent quitPendingIntent;
    private Constants.Protocol connectionMethod = Constants.Protocol.DIRECT_SSH;

    @Override
    public void onCreate() {
        if (!serviceCreated) {
            v2rayCoreExecutor = new V2rayCoreExecutor(this);
            tun2SocksExecutor = new Tun2SocksExecutor(this);
            statsHandler = new StatsHandler(v2rayCoreExecutor);
            serviceCreated = true;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        boolean allowStart = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                allowStart = false;
            }
            NetworkCapabilities activeNetwork = connectivityManager.getNetworkCapabilities(network);
            // if active network is null or if active network doesn't have either cellular or wifi
            // wifi transport
            if (!(activeNetwork != null && (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)))) {
                allowStart = false;
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            // if network info is null or networkInfo is disconnected
            if (!(networkInfo != null && networkInfo.isConnected())) {
                allowStart = false;
            }
        }
        if (!allowStart) {
            Toast.makeText(getApplicationContext(),
                    Values.NETWORK_UNAVAILABLE_STRING,
                    Toast.LENGTH_SHORT).show();

            return START_REDELIVER_INTENT;
        }

        if (!serviceActive.get()) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "SecureShellV::VpnService");
            wakeLock.acquire(20 * 60 * 1000L /*10 minutes*/);

            setupNotification();
            // set notification action buttons
            notificationBuilder.clearActions().addAction(notifStopAction);
            notificationBuilder.addAction(notifQuitAction);

            startVpn();

            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
            connectivityManager.requestNetwork(networkRequest, networkCallback);
        }
        return START_NOT_STICKY;
    }

    private void startVpn() {
        VpnService.prepare(this);
        Log.d(TAG, "VPN service prepared");

        vpnInterface = establishVPNInterface();
        serviceActive.set(true);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(vpnServiceAction)
//                .putExtra("vpn_interface", vpnInterface));

        connectionHandler = new ConnectionHandler(vpnInterface,
                this,
                notificationListener,
                v2rayCoreExecutor,
                statsHandler);

        connectionHandler.start();

        tun2SocksExecutor.run(this,
                VpnSettings.socksPort,
                VpnSettings.localDnsPort);

        sendFileDescriptor();
    }


    private void sendFileDescriptor() {
        String localSocksFile = new File(getApplicationContext().getFilesDir(), "sock_path").getAbsolutePath();
        FileDescriptor tunFd = vpnInterface.getFileDescriptor();
        new Thread(() -> {
            boolean isSendFDSuccess = false;
            for (int sendFDTries = 0; sendFDTries < 5; sendFDTries++) {
                try {
                    Thread.sleep(50L * sendFDTries);
                    LocalSocket clientLocalSocket = new LocalSocket();
                    clientLocalSocket.connect(new LocalSocketAddress(localSocksFile, LocalSocketAddress.Namespace.FILESYSTEM));
                    if (!clientLocalSocket.isConnected()) {
                        Log.i("SOCK_FILE", "Unable to connect to localSocksFile [" + localSocksFile + "]");
                    } else {
                        Log.i("SOCK_FILE", "connected to sock file [" + localSocksFile + "]");
                    }
                    OutputStream clientOutStream = clientLocalSocket.getOutputStream();
                    clientLocalSocket.setFileDescriptorsForSend(new FileDescriptor[]{tunFd});
                    clientOutStream.write(42);
                    clientLocalSocket.shutdownOutput();
                    clientLocalSocket.close();
                    isSendFDSuccess = true;
                    break;
                } catch (Exception ignore) {
                }
            }
            if (!isSendFDSuccess) {
                Log.w("SendFDFailed", "Couldn't send file descriptor !");
            }
        }, "sendFd_Thread").start();
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
        builder.addDnsServer("26.26.26.2");
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
            Log.e(TAG, "name not found exception", e);
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
        Intent notificationIntent = new Intent(this, HomepageActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        initNotificationButtonIntents();

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

    private void initNotificationButtonIntents() {
        Intent startIntent = new Intent(this, NotificationBroadcastReceiver.class);
        startIntent.setAction(Intents.START_VPN_SERVICE_ACTION);
        startIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        startPendingIntent = PendingIntent.getBroadcast(
                this, 0, startIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, NotificationBroadcastReceiver.class);
        stopIntent.setAction(Intents.STOP_VPN_SERVICE_ACTION);
        stopIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        stopPendingIntent = PendingIntent.getBroadcast(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent quitIntent = new Intent(this, NotificationBroadcastReceiver.class);
        quitIntent.setAction(Intents.EXIT_APP_ACTION);
        quitIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        quitPendingIntent = PendingIntent.getBroadcast(
                this, 0, quitIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        stopVpnService();
    }

    @Override
    public void onLowMemory() {
        System.out.println("LOW MEMORY");
    }

    /**
     * Called when app is exiting and needs to clear the VPN and connections and services
     */
    public void finalizeAndStop() {
        stopVpnService();
        stopForeground(true);
        notificationManager.cancelAll();
        stopSelf();
    }

    public void stopVpnService() {
        if (!this.isServiceActive()) {
            return;
        }
        try {
            serviceActive.set(false);
            connectionHandler.interrupt();
            notificationBuilder.clearActions().addAction(notifStartAction);
            notificationBuilder.addAction(notifQuitAction);
            notificationManager.notify(onGoingNotificationID, notificationBuilder.build());
            notificationListener.updateNotification(null, ConnectionState.DISCONNECTED);
            tun2SocksExecutor.stopTun2Socks();
            try {
                vpnInterface.close();
            } catch (IOException ignored) {
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Intents.STOP_VPN_SERVICE_ACTION));
            if (!SharedPreferencesSingleton.getInstance(this).isPersistentNotification()) {
                stopForeground(STOP_FOREGROUND_REMOVE);
                notificationManager.cancelAll();
            }
        } catch (NullPointerException ignored) {
        }
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

    //    V2rayServicesListener implementations
    @Override
    public boolean onProtect(int socket) {
        return true;
    }

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void startService() {

    }

    @Override
    public void stopService() {

    }

    @Override
    public void OnTun2SocksHasMassage(V2rayConstants.CORE_STATES tun2SocksState, String newMessage) {
        System.out.println("V2RAY MESSAGE:" + newMessage);
    }
//    V2rayServicesListener implementations

    public class VpnServiceBinder extends Binder {

        public SSVpnService getService() {
            return SSVpnService.this;
        }

        public void stopService() {
            finalizeAndStop();
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
        throws RemoteException
        {
            // see Implementation of android.net.VpnService.Callback.onTransact()
            if ( code == IBinder.LAST_CALL_TRANSACTION )
            {
                onRevoke();
                return true;
            }
            return super.onTransact( code, data, reply, flags );
        }

        private void onRevoke()
        {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Intents.STOP_VPN_SERVICE_ACTION));
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

}

