package com.securelight.secureshellv.vpnservice.connection;

import com.securelight.secureshellv.utility.NetTools;
import com.securelight.secureshellv.utility.Utilities;

import android.util.Log;

import com.securelight.secureshellv.vpnservice.VpnSettings;
import com.securelight.secureshellv.vpnservice.listeners.ConnectionStateListener;
import com.securelight.secureshellv.vpnservice.listeners.SocketProtector;
import com.securelight.secureshellv.vpnservice.listeners.SocksStateListener;
import com.securelight.secureshellv.vpnservice.v2ray.V2rayCoreManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

//import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;

public class SocksHeartbeatTask extends TimerTask {
    private final String TAG = getClass().getName();
    private final ConnectionStateListener connectionStateListener;
    private final SocksStateListener socksStateListener;
    private SocketProtector socketProtector;
    private final AtomicBoolean networkIFaceAvailable = new AtomicBoolean(true);
    private final V2rayCoreManager v2raCoreManager;
    private final AccessChangeListener accessChangeListener;
    private final AtomicBoolean connectionHandlerRunning;
    private int counter = 0;

    public SocksHeartbeatTask(AtomicBoolean running,
                              SocketProtector socketProtector,
                              V2rayCoreManager v2rayCoreManager,
                              SocksStateListener socksStateListener,
                              AccessChangeListener accessChangeListener,
                              ConnectionStateListener connectionStateListener) {
        this.socketProtector = socketProtector;
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

        NetworkState tempType = NetTools.checkAndGetAccessType(socketProtector);
        if (tempType == NetworkState.NO_ACCESS) {
            counter = 0;
            this.accessChangeListener.onNetworkStateChanged(NetworkState.NO_ACCESS);
            return;
        }

        this.accessChangeListener.onNetworkStateChanged(NetworkState.WORLD_WIDE);
        try {
            long delay = v2raCoreManager.measureDelay("");
            if (delay >= 0) {

                connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTED);
                socksStateListener.onSocksUp(delay);
                Log.d(TAG, "SOCKS UP");
                counter = 0;
            } else {
                Log.d(TAG, "SOCKS DOWN");
                if (counter >= 3) {
                    if (!pingWithSocket()) {
                        socksStateListener.onSocksDown();
                        connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTING);
                    }
                    counter = -1;
                }
                counter++;
            }
        } catch (Exception ignored) {
            if (!pingWithSocket()) {
                socksStateListener.onSocksDown();
                connectionStateListener.onConnectionStateListener(ConnectionState.CONNECTING);
            }
            counter = -1;
        }
    }

    public void setNetworkIFaceAvailable(boolean available) {
        networkIFaceAvailable.set(available);
    }

    public boolean pingWithSocket() {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                new InetSocketAddress("127.0.0.1", VpnSettings.socksPort));

        try (Socket socket = new Socket(proxy)) {
            socket.connect(new InetSocketAddress("https://www.google.com/", 443),
                    5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public interface AccessChangeListener {
        void onNetworkStateChanged(NetworkState networkState);
    }
}
