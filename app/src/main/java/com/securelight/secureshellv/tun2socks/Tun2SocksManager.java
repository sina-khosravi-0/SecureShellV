package com.securelight.secureshellv.tun2socks;

import android.os.ParcelFileDescriptor;

import com.securelight.secureshellv.vpnservice.VpnSettings;
import com.securelight.secureshellv.vpnservice.listeners.Tun2SocksListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.dev7.lib.v2ray.core.Tun2SocksExecutor;
import dev.dev7.lib.v2ray.utils.V2rayConfigs;


public class Tun2SocksManager {

    private static final List<Long> threadIds = new ArrayList<>();
    private static Thread thread;
    private final String TAG = this.getClass().getSimpleName();
    private final ParcelFileDescriptor vpnInterface;
    private final Tun2SocksListener t2SListener;
    private boolean isRunning;

    public Tun2SocksManager(ParcelFileDescriptor vpnInterface, Tun2SocksListener t2SListener) {
        this.vpnInterface = vpnInterface;
        this.t2SListener = t2SListener;
    }

    public void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(() -> {
            // check 20 times if tun2socks can start
            for (int i = 0; i < 20; i++) {
                if (Tun2SocksJni.canStart() == 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    break;
                }
            }
            isRunning = true;
            Tun2SocksJni.runTun2Socks(vpnInterface.getFd(),
                    VpnSettings.interfaceMtu,
                    VpnSettings.iFaceAddress,
                    VpnSettings.iFaceSubnetMask,
                    "127.0.0.1" + ":" + VpnSettings.socksPort,
                    "127.0.0.1" + ":" + VpnSettings.localDnsPort,
                    1,
                    1);
            isRunning = false;
            t2SListener.onTun2SocksStopped();
        }, "Tun2Socks-Starting-Thread");
        threadIds.add(thread.getId());
        thread.start();
    }

    public void stop() {
        new Thread(() -> {
            while (Tun2SocksJni.canStop() == 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            Tun2SocksJni.terminateTun2Socks();
            try {
                vpnInterface.close();
            } catch (IOException e) {
            }
            thread = null;
        }, "Tun2Socks-Stopping-Thread").start();
    }

    public boolean isRunning() {
        return isRunning;
    }
}