package com.securelight.secureshellv.vpnservice.connection;

import static com.securelight.secureshellv.utility.Utilities.checkAndGetAccessType;

import android.util.Log;

import com.securelight.secureshellv.vpnservice.listeners.ConnectionStateListener;
import com.securelight.secureshellv.vpnservice.listeners.SocksStateListener;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;

public class SocksHeartbeatTask extends TimerTask {
    private final String TAG = getClass().getName();
    //    private final SshManager sshManager;
    private final ConnectionStateListener connectionStateListener;
    private final SocksStateListener socksStateListener;
    private final AtomicBoolean networkIFaceAvailable = new AtomicBoolean(true);
    private final V2rayCoreExecutor v2rayCoreExecutor;
    private final AccessChangeListener accessChangeListener;
    private int counter = 0;
    private final AtomicBoolean connectionHandlerRunning;

    public interface AccessChangeListener {
        void onNetworkStateChanged(NetworkState networkState);
    }

    public SocksHeartbeatTask(ConnectionStateListener connectionStateListener,
                              SocksStateListener socksStateListener,
                              V2rayCoreExecutor v2rayCoreExecutor,
                              AccessChangeListener accessChangeListener,
                              AtomicBoolean running) {
        this.connectionStateListener = connectionStateListener;
        this.socksStateListener = socksStateListener;
        this.v2rayCoreExecutor = v2rayCoreExecutor;
        this.accessChangeListener = accessChangeListener;
        this.connectionHandlerRunning = running;
    }

    @Override
    public void run() {
        if (!connectionHandlerRunning.get()) {
            this.cancel();
            connectionStateListener.onConnectionStateListener(ConnectionState.DISCONNECTED);
            this.accessChangeListener.onNetworkStateChanged(NetworkState.NONE);
            return;
        }
        if (!networkIFaceAvailable.get()) {
            this.accessChangeListener.onNetworkStateChanged(NetworkState.NO_ACCESS);
            return;
        }
        NetworkState tempType = checkAndGetAccessType(networkIFaceAvailable.get());
        if (tempType == NetworkState.UNAVAILABLE || tempType == NetworkState.NO_ACCESS) {
            counter = 0;
            this.accessChangeListener.onNetworkStateChanged(NetworkState.NO_ACCESS);
            return;
        }
        this.accessChangeListener.onNetworkStateChanged(NetworkState.WORLD_WIDE);
        if (v2rayCoreExecutor.getCurrentServerDelay() >= 1) {
            counter = 0;
            connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTED);
            socksStateListener.onSocksUp();
            Log.d(TAG, "SOCKS UP");
        } else {
            Log.d(TAG, "SOCKS DOWN");
            if (counter >= 3) {
                socksStateListener.onSocksDown();
                connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTING);
                counter = -1;
            }
            counter++;
        }
    }

    public void setNetworkIFaceAvailable(boolean available) {
        networkIFaceAvailable.set(available);
    }
}
