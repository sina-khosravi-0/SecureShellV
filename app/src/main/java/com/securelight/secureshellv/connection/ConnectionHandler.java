package com.securelight.secureshellv.connection;

import android.app.Application;
import android.os.ParcelFileDescriptor;

import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.connection.InternetAccessHandler;
import com.securelight.secureshellv.ssh.SSHManager;
import com.securelight.secureshellv.tun2socks.Tun2SocksManager;

import org.apache.sshd.client.session.ClientSession;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionHandler extends Thread {
    private final String TAG = getClass().getName();
    private final VpnSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private SSHManager sshManager;
    private Tun2SocksManager tun2SocksManager;
    private Application application;
    private final InternetAccessHandler internetAccessHandler;

    private AtomicBoolean isServiceOn = new AtomicBoolean(false);

    public boolean isServiceOn() {
        return isServiceOn.get();
    }

    public ConnectionHandler(VpnSettings vpnSettings, ParcelFileDescriptor vpnInterface, Application application) {
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
        this.application = application;
        internetAccessHandler = new InternetAccessHandler(this);
    }

    @Override
    public void run() {
        isServiceOn.set(true);
        internetAccessHandler.start();

        sshManager = new SSHManager(vpnSettings, this);
        tun2SocksManager = new Tun2SocksManager(vpnSettings, vpnInterface, application);

        sshManager.setupConnection();
        sshManager.startPortForwarding();
        tun2SocksManager.start();

        while (isServiceOn()) {
            sshManager.getSession().waitFor(Arrays.asList(ClientSession.ClientSessionEvent.CLOSED,
                    ClientSession.ClientSessionEvent.TIMEOUT), 0);
            tun2SocksManager.stop();
            // reconnect
            sshManager.setupConnection();
            sshManager.startPortForwarding();
            tun2SocksManager.start();
        }
    }

    @Override
    public void interrupt() {
        isServiceOn.set(false);
        no();
    }

    /**
     * @param ignored is there to differentiate method from Thread.stop()
    * */
    public void stop(boolean ignored) {
        isServiceOn.set(false);
    }

    public void no() {

    }

    public void yes() {

    }

    public Application getApplication() {
        return application;
    }

    public Tun2SocksManager getTun2SocksManager() {
        return tun2SocksManager;
    }

    public InternetAccessHandler getInternetAccessHandler() {
        return internetAccessHandler;
    }

    public SSHManager getSshManager() {
        return sshManager;
    }
}
