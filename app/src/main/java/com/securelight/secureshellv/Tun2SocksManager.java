package com.securelight.secureshellv;

import android.os.ParcelFileDescriptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import engine.Engine;
import engine.Key;


public class Tun2SocksManager {

    public final String TAG = this.getClass().getName();
    private final VPNSettings vpnSettings;
    private final ExecutorService executors = Executors.newFixedThreadPool(1);
    private final ParcelFileDescriptor vpnInterface;
    private Key key;

    public Tun2SocksManager(final VPNSettings vpnSettings, ParcelFileDescriptor vpnInterface) {
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
    }

    public void start() {
        key = new Key();
        key.setMark(0);
        key.setMTU(0);
        key.setDevice("fd://" + vpnInterface.getFd());
        key.setInterface("");
        //[debug|info|warning|error|silent]
        key.setLogLevel("info");
        key.setProxy("socks5://" + vpnSettings.getHost() + ":" + vpnSettings.getPort());
        key.setRestAPI("");
        key.setTCPSendBufferSize("");
        key.setTCPReceiveBufferSize("");
        key.setTCPModerateReceiveBuffer(false);
        Engine.insert(key);
        executors.submit(Engine::start);

    }

    public void stop() {
        executors.submit(Engine::stop);
    }
}