package com.securelight.secureshellv.vpnservice.connection;

import com.securelight.secureshellv.utility.Utilities;

import android.util.Log;

import com.securelight.secureshellv.vpnservice.listeners.ConnectionStateListener;
import com.securelight.secureshellv.vpnservice.listeners.SocksStateListener;
import com.securelight.secureshellv.vpnservice.v2ray.V2rayCoreManager;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import libv2ray.CoreController;

//import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;

public class SocksHeartbeatTask extends TimerTask {
    private final String TAG = getClass().getName();
    private final ConnectionStateListener connectionStateListener;
    private final SocksStateListener socksStateListener;
    private final AtomicBoolean networkIFaceAvailable = new AtomicBoolean(true);
    private final V2rayCoreManager v2raCoreManager;
    private final AccessChangeListener accessChangeListener;
    private final AtomicBoolean connectionHandlerRunning;
    private int counter = 0;

    public SocksHeartbeatTask(AtomicBoolean running,
                              V2rayCoreManager v2rayCoreManager ,
                              SocksStateListener socksStateListener,
                              AccessChangeListener accessChangeListener,
                              ConnectionStateListener connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
        this.socksStateListener = socksStateListener;
        this.v2raCoreManager = v2rayCoreManager;
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
            connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTING);
            this.accessChangeListener.onNetworkStateChanged(NetworkState.UNAVAILABLE);
            return;
        }

        NetworkState tempType = Utilities.checkAndGetAccessType();
        if (tempType == NetworkState.NO_ACCESS) {
            counter = 0;
            this.accessChangeListener.onNetworkStateChanged(NetworkState.NO_ACCESS);
            return;
        }

        this.accessChangeListener.onNetworkStateChanged(NetworkState.WORLD_WIDE);
        try {
            if (v2raCoreManager.measureDelay("") >= 1) {
                connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTED);
                socksStateListener.onSocksUp();
                Log.d(TAG, "SOCKS UP");
                counter = 0;
            } else {
                Log.d(TAG, "SOCKS DOWN");
                if (counter >= 3) {
                    socksStateListener.onSocksDown();
                    connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTING);
                    counter = -1;
                }
                counter++;
            }
        } catch (Exception ignored) {
            socksStateListener.onSocksDown();
            connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTING);
            counter = -1;
        }
    }

    public void setNetworkIFaceAvailable(boolean available) {
        networkIFaceAvailable.set(available);
    }

    public interface AccessChangeListener {
        void onNetworkStateChanged(NetworkState networkState);
    }
}
