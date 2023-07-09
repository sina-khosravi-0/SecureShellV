package com.securelight.secureshellv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
    ParcelFileDescriptor iFace;
    private final AtomicReference<Thread> mConnectingThread = new AtomicReference<>();
    private final AtomicReference<Connection> mConnection = new AtomicReference<>();
    private final AtomicInteger mNextConnectionId = new AtomicInteger(1);

    private final BroadcastReceiver stopBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("stop_kill".equals(intent.getAction())) {
                try {
                    disconnect();
                    Log.i(getClass().getName(), "VPN service stopped");
                } catch (IOException e) {
                    Log.e(getClass().getName(), "Error during stopping VPN service ", e);
                }
                stopSelf();
            }
        }
    };

    private static class Connection extends Pair<Thread, ParcelFileDescriptor> {
        public Connection(Thread thread, ParcelFileDescriptor pfd) {
            super(thread, pfd);
        }
    }

    public interface OnEstablishListener {
        void onEstablish(ParcelFileDescriptor tunInterface);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        iFace = configure();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(stopBr, new IntentFilter("stop_kill"));
    }

    private ParcelFileDescriptor configure() throws PackageManager.NameNotFoundException {
        VpnService.Builder builder = mService.new Builder();
        // Todo: Address is used in port forwarding
        builder.addAddress(config.getHost(), 24);
        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer(config.getDnsHost());

        for (String p : mPackages) {
            builder.addDisallowedApplication(p);
        }
        final ParcelFileDescriptor vpnInterface;
        builder.setSession(config.getHost());
        synchronized (mService) {
            vpnInterface = builder.establish();
            if (mOnEstablishListener != null) {
                mOnEstablishListener.onEstablish(vpnInterface);
            }
        }
        Log.i(TAG, "New interface: " + vpnInterface);
        return vpnInterface;
    }


    public void setOnEstablishListener(OnEstablishListener listener) {
        mOnEstablishListener = listener;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VPNSettings config = (VPNSettings) intent.getSerializableExtra("config");
        startVpn(config);
        return super.onStartCommand(intent, flags, startId);
    }

    SockConnection sockConnection;

    private void startVpn(VPNSettings config) {
        Set<String> packages = new HashSet<>();
        packages.add(getPackageName());
        // Termius
//        packages.add("com.server.auditor.ssh.client");
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
                System.out.println("fuck");
                oldConnection.first.interrupt();
                oldConnection.second.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing VPN interface", e);
            }
        }
    }

    private void disconnect() throws IOException {
        setConnectingThread(null);
        setConnection(null);
        sockConnection.disconnect();
        stopForeground(true);
    }
}