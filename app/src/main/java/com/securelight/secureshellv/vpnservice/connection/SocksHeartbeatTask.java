package com.securelight.secureshellv.vpnservice.connection;

import android.util.Log;

import com.securelight.secureshellv.vpnservice.listeners.ConnectionStateListener;
import com.securelight.secureshellv.vpnservice.listeners.SocksStateListener;

import java.util.TimerTask;

import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;

public class SocksHeartbeatTask extends TimerTask {
    private final String TAG = getClass().getName();
    //    private final SshManager sshManager;
    private final ConnectionStateListener connectionStateListener;
    private final SocksStateListener socksStateListener;
    private final V2rayCoreExecutor v2rayCoreExecutor;
    private int counter = 0;

//    public SocksHeartbeatHandler(SshManager sshManager) {
//        this.sshManager = sshManager;
//    }
    public SocksHeartbeatTask(ConnectionStateListener connectionStateListener,
                              SocksStateListener socksStateListener,
                              V2rayCoreExecutor v2rayCoreExecutor) {
        this.connectionStateListener = connectionStateListener;
        this.socksStateListener = socksStateListener;
        this.v2rayCoreExecutor = v2rayCoreExecutor;
    }

    @Override
    public void run() {
//        if (sshManager.isEstablished()) {
        if (v2rayCoreExecutor.getCurrentServerDelay() >= 1) {
            counter = 0;
            connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTED);
            socksStateListener.onSocksUp();
            Log.d(TAG, "SOCKS UP");
        } else {
            Log.d(TAG, "SOCKS DOWN");
            if (counter >= 3) {
//                    sshManager.close();
                socksStateListener.onSocksDown();
                connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTING);
                counter = -1;
            }
            counter++;
        }
//        }
    }
}
