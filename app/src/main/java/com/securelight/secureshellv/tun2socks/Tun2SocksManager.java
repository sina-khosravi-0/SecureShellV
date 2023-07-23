package com.securelight.secureshellv.tun2socks;

import android.app.Application;
import android.os.ParcelFileDescriptor;

import com.securelight.secureshellv.Constants;
import com.securelight.secureshellv.VpnSettings;


public class Tun2SocksManager {

    private final String TAG = this.getClass().getName();
    private final VpnSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private boolean isRunning;
    private Application application;
    private Thread tun2SocksThread;

    public Tun2SocksManager(final VpnSettings vpnSettings, ParcelFileDescriptor vpnInterface, Application application) {
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
        this.application = application;
    }

    public void start() {
        tun2SocksThread = new Thread(() -> {
            isRunning = true;
            Tun2SocksJni.runTun2Socks(vpnInterface.getFd(), 1500, vpnSettings.getHost(),
                    vpnSettings.getSubnet(), vpnSettings.getHost() + ":" + vpnSettings.getPort(),
                    "127.0.0.1" + ":" + 7300,
                    1, Constants.DEBUG ? 0 : 0);
        });
        tun2SocksThread.start();
    }

    public void stop() {
        isRunning = false;
        new Thread(Tun2SocksJni::terminateTun2Socks).start();
    }

    public boolean isRunning() {
        return isRunning;
    }
}