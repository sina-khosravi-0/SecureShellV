package com.securelight.secureshellv;

import android.app.Application;
import android.os.ParcelFileDescriptor;

import com.securelight.secureshellv.connection.BroadCastSampleActivity;
import com.securelight.secureshellv.ssh.SSHConnectionManager;
import com.securelight.secureshellv.tun2socks.Tun2SocksManager;

import org.apache.sshd.client.session.ClientSession;

import java.util.Collections;

public class ConnectionHandler extends Thread {
    private final String TAG = getClass().getName();
    private final VpnSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private SSHConnectionManager sshConnectionManager;
    private Tun2SocksManager tun2SocksManager;
    private Application application;
    public boolean vpnIsOn = false;

    public ConnectionHandler(VpnSettings vpnSettings, ParcelFileDescriptor vpnInterface, Application application) {
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
        this.application = application;
    }

    @Override
    public void run() {
//        new Thread(() -> {
        vpnIsOn = true;
        while (vpnIsOn) {
            BroadCastSampleActivity.isInternetAvailable();
            try {
                sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        }).start();
        sshConnectionManager = new SSHConnectionManager(vpnSettings, this);
        tun2SocksManager = new Tun2SocksManager(vpnSettings, vpnInterface, application);

        if (!sshConnectionManager.setupConnection()) {
            throw new RuntimeException();
        }
        sshConnectionManager.startPortForwarding();
        tun2SocksManager.start();

        while (vpnIsOn) {
            sshConnectionManager.getSession().waitFor(Collections.singleton(
                    ClientSession.ClientSessionEvent.CLOSED), -1);
            System.out.println("CLOSED");

            sshConnectionManager.setupConnection();
            sshConnectionManager.startPortForwarding();
            System.out.println("AUTHED");
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        vpnIsOn = false;
        disconnect();

    }

    public void disconnect() {
//        try {
//            sshConnectionManager.close();
//        } catch (IOException e) {
//            Log.e(TAG, "Couldn't close ssh connection");
//        }
        tun2SocksManager.stop();
    }

    public void reconnect() {
//        new Thread(() -> sshConnectionManager.startPortForwarding()).start();
        tun2SocksManager.start();
    }

    public Application getApplication() {
        return application;
    }

    public Tun2SocksManager getTun2SocksManager() {
        return tun2SocksManager;
    }
}
