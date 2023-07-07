package com.securelight.secureshellv;

import android.os.ParcelFileDescriptor;

import com.securelight.secureshellv.SSH.SSHConnectionManager;

public class ConnectionHandler implements Runnable {
    private final VPNSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private SSHConnectionManager sshConnectionManager;
    private Tun2SocksManager tun2SocksManager;

    public ConnectionHandler(VPNSettings vpnSettings, ParcelFileDescriptor vpnInterface) {
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
    }

    @Override
    public void run() {
        sshConnectionManager = new SSHConnectionManager(vpnSettings);
        sshConnectionManager.startPortForwarding();
        tun2SocksManager = new Tun2SocksManager(vpnSettings, vpnInterface);
        tun2SocksManager.start();
    }
}
