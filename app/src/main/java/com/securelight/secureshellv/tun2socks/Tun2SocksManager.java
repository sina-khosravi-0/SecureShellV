package com.securelight.secureshellv.tun2socks;

import android.app.Application;
import android.os.ParcelFileDescriptor;

import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.connection.ConnectionHandler;


public class Tun2SocksManager {

    private final String TAG = this.getClass().getSimpleName();
    private final VpnSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private boolean isRunning;
    private final ConnectionHandler connectionHandler;
    private Thread tun2SocksThread;
    public boolean run = true;

    public Tun2SocksManager(final VpnSettings vpnSettings, ParcelFileDescriptor vpnInterface,
                            ConnectionHandler connectionHandler) {
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
        this.connectionHandler = connectionHandler;
    }

    public void start() {
        tun2SocksThread = new Thread(() -> {
            if (!connectionHandler.isServiceActive()) {
                return;
            }
            isRunning = true;
            Tun2SocksJni.runTun2Socks(vpnInterface.getFd(), 1500, vpnSettings.getHost(),
                    vpnSettings.getSubnet(), vpnSettings.getHost() + ":" + vpnSettings.getPort(),
                    "127.0.0.1" + ":" + 7300,
                    1, -1);
        });
        tun2SocksThread.start();
    }

    public void stop() {
        if (isRunning) {
            new Thread(Tun2SocksJni::terminateTun2Socks).start();
        }
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}