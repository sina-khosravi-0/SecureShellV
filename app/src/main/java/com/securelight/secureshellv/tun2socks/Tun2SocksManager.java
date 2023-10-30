package com.securelight.secureshellv.tun2socks;

import android.os.ParcelFileDescriptor;

import com.securelight.secureshellv.vpnservice.VpnSettings;
import com.securelight.secureshellv.vpnservice.listeners.Tun2SocksListener;

import java.io.IOException;


public class Tun2SocksManager {

    private final String TAG = this.getClass().getSimpleName();
    private final ParcelFileDescriptor vpnInterface;
    private boolean isRunning;
    private Tun2SocksListener t2SListener;

    public Tun2SocksManager(ParcelFileDescriptor vpnInterface, Tun2SocksListener t2SListener) {
        this.vpnInterface = vpnInterface;
        this.t2SListener = t2SListener;
    }

    public void start() {
        new Thread(() -> {
            isRunning = true;
            Tun2SocksJni.runTun2Socks(vpnInterface.getFd(), 1500, VpnSettings.iFaceAddress,
                    VpnSettings.iFaceSubnet, VpnSettings.iFaceAddress + ":" + VpnSettings.socksPort,
                    "127.0.0.1" + ":" + 7300,
                    0, -1);
            isRunning = false;
            t2SListener.onTun2SocksStopped();
        }).start();
    }

    public void stop() {
        if (isRunning) {
            new Thread(() -> {
                Tun2SocksJni.terminateTun2Socks();
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                }
            }).start();
        }
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}