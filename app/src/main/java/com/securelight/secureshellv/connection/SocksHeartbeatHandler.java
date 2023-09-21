package com.securelight.secureshellv.connection;

import android.util.Log;

import com.securelight.secureshellv.tun2socks.Tun2SocksJni;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.TimerTask;

public class SocksHeartbeatHandler extends TimerTask {
    private final String TAG = getClass().getName();
    private final ConnectionHandler connectionHandler;
    private int counter = 0;

    public SocksHeartbeatHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void run() {
        if (connectionHandler.getSshManager().isReady()) {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(connectionHandler.getVpnSettings().getIFaceAddress(),
                            connectionHandler.getVpnSettings().getSocksPort()));

            try (Socket socket = new Socket(proxy)) {
                socket.connect(new InetSocketAddress("google.com", 443), 1500);
                counter = 0;
                Log.d(TAG, "SOCKS UP");
            } catch (IOException e) {
                Log.d(TAG, "SOCKS DOWN");
                if (counter >= 3) {
                    connectionHandler.getSshManager().close();
                }
                counter++;
            }
        }
    }
}
