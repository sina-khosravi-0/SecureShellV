package com.securelight.secureshellv.connection;

import android.app.Application;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.ssh.SSHManager;
import com.securelight.secureshellv.tun2socks.Tun2SocksManager;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;

import java.io.IOException;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
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
            Log.v(TAG, "reconnecting");
            sshManager.setupConnection();
            Log.v(TAG, "reconnected?");
            sshManager.startPortForwarding();
            Log.v(TAG, "reconnected");
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
     */
    public void stop(boolean ignored) {
        isServiceOn.set(false);
    }

    public void no() {
        String string = String.format("session: open:%s, authed:%s\n", sshManager.getSession().isOpen(),
                sshManager.getSession().isAuthenticated());
        for (PortForwardingTracker portForwardingTracker : sshManager.getPortForwardingTrackers()) {
            string = String.format(string + "\t%s\n", portForwardingTracker.toString());
        }
        Log.v(TAG, string);
    }

    public void yes() {
        System.out.println(getSshManager().getSession().getIdleTimeout());
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
