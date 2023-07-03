package com.example.secureshellv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SSVpnService extends VpnService {

    public final String TAG = this.getClass().getName();

    public static final String ACTION_CONNECT = "com.example.secureshellv.action.START";
    public static final String ACTION_DISCONNECT = "com.example.secureshellv.action.STOP";
    public static final String CHANNEL_ID = "";

    private final AtomicReference<Thread> mConnectingThread = new AtomicReference<>();
    private final AtomicReference<Connection> mConnection = new AtomicReference<>();
    private final AtomicInteger mNextConnectionId = new AtomicInteger(1);

    private final BroadcastReceiver stopBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("stop_kill".equals(intent.getAction())) {
                System.out.println("Stop KILL");
                disconnect();
                stopSelf();
            }
        }
    };

    private static class Connection extends Pair<Thread, ParcelFileDescriptor> {
        public Connection(Thread thread, ParcelFileDescriptor pfd) {
            super(thread, pfd);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(stopBr, new IntentFilter("stop_kill"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ServerConfig config = (ServerConfig) intent.getSerializableExtra("config");
//        PendingIntent mConfigureIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
//                PendingIntent.FLAG_UPDATE_CURRENT);
        startVpn(config);
        return super.onStartCommand(intent, flags, startId);
    }

    SockConnection sockConnection;

    private void startVpn(ServerConfig config) {
        Set<String> packages = new HashSet<>();
        packages.add(getPackageName());
        packages.add("com.server.auditor.ssh.client");
        sockConnection = new SockConnection(this, config, packages);
        VpnService.prepare(this);
        Log.d(TAG, "Prepared");
        startConnection(sockConnection);

    }

    private void startConnection(final SockConnection connection) {
        final Thread thread = new Thread(connection);
        setConnectingThread(thread);
        connection.setOnEstablishListener(tunInterface -> {
            mConnectingThread.compareAndSet(thread, null);
            setConnection(new Connection(thread, tunInterface));
        });
        thread.start();
    }

    private void setConnectingThread(final Thread thread) {
        final Thread oldThread = mConnectingThread.getAndSet(thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setConnection(final Connection connection) {
        final Connection oldConnection = mConnection.getAndSet(connection);
        if (oldConnection != null) {
            try {
                oldConnection.first.interrupt();
                oldConnection.second.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing VPN interface", e);
            }
        }
    }

    private void disconnect() {
        setConnectingThread(null);
        setConnection(null);
        sockConnection.disconnect();
        stopForeground(true);
    }


}
//public class SSVpnService extends VpnService {
//    public static final String ACTION_CONNECT = "com.example.secureshellv.START";
//    public static final String ACTION_DISCONNECT = "com.example.secureshellv.STOP";
//    private PendingIntent mConfigureIntent;
//
//    @Override
//    public void onCreate() {
//        mConfigureIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
////        connect("shit");
//        return START_STICKY;
//    }
//
//    private void connect() {
//        // Extract information from the shared preferences.
//        new Thread(() -> {
//            final SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 2000);
//            try {
//                run(serverAddress);
//            } catch (InterruptedException | IOException e) {
//                throw new RuntimeException(e);
//            }
//        }).start();
//    }
//
//    private void connect(String shit){
//        new Thread(() -> {
//            final SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 2000);
//            try {
//                DatagramChannel tunnel = DatagramChannel.open();
//                protect(tunnel.socket());
//                tunnel.connect(serverAddress);
//                tunnel.configureBlocking(false);
//                Builder builder = this.new Builder();
//                builder.addAddress("198.18.0.1", 32);
//                builder.addRoute("0.0.0.0", 0);
//                builder.addDisallowedApplication(getApplicationContext().getPackageName());
//                builder.setSession("IPv4/Global");
//                builder.establish();
//                builder.allowFamily(OsConstants.AF_INET);
//            } catch (IOException | PackageManager.NameNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        }).start();
//
//    }
//
//    private void run(SocketAddress server)
//            throws InterruptedException, IllegalArgumentException, IllegalStateException, IOException {
//        ParcelFileDescriptor iface = null;
//        DatagramChannel tunnel = DatagramChannel.open();
//        protect(tunnel.socket());
//        tunnel.connect(server);
//        tunnel.configureBlocking(false);
//        configureVirtualInterface();
//    }
//
//    private ParcelFileDescriptor configureVirtualInterface() throws IllegalArgumentException {
//        // Configure a builder while parsing the parameters.
//        VpnService.Builder builder = this.new Builder();
//        try {
//            builder.addAddress("192.168.2.2", 24);
//            builder.addRoute("0.0.0.0", 0);
//            builder.addDnsServer("192.168.1.1");
//
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("Bad parameter: " + e);
//        }
//        builder.setSession("127.0.0.1").setConfigureIntent(mConfigureIntent);
//
//        // Create a new interface using the builder and save the parameters.
//        return builder.establish();
//    }
//}
