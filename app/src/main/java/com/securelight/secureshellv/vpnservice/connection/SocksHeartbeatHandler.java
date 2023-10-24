package com.securelight.secureshellv.vpnservice.connection;

import android.util.Log;

import com.securelight.secureshellv.ssh.SshManager;
import com.securelight.secureshellv.vpnservice.VpnSettings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.TimerTask;

public class SocksHeartbeatHandler extends TimerTask {
    private final String TAG = getClass().getName();
    private final SshManager sshManager;
    private int counter = 0;

    public SocksHeartbeatHandler(SshManager sshManager) {
        this.sshManager = sshManager;
    }

    @Override
    public void run() {
        if (sshManager.isEstablished()) {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(VpnSettings.iFaceAddress, VpnSettings.socksPort));

            try (Socket socket = new Socket(proxy)) {
                // TODO: fetch from server

                socket.connect(new InetSocketAddress("google.com", 443),
                        3000);
                counter = 0;
                Log.d(TAG, "SOCKS UP");
            } catch (IOException e) {
                Log.d(TAG, "SOCKS DOWN", e);
                if (counter >= 3) {
                    sshManager.close();
                    counter = -1;
                }
                counter++;
            }
        }
    }
}
