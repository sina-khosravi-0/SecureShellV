package com.securelight.secureshellv.connection;

import android.app.Application;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.SSVpnService;
import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.ssh.SSHManager;
import com.securelight.secureshellv.tun2socks.Tun2SocksManager;

import org.apache.sshd.client.session.ClientSession;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler extends Thread {
    private final String TAG = getClass().getName();
    private final VpnSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private SSHManager sshManager;
    private final Application application;
    private final SSVpnService vpnService;
    private Tun2SocksManager tun2SocksManager;
    private final InternetAccessHandler internetAccessHandler;
    private boolean serviceActive;
    private final AtomicBoolean connected;
    private final ReentrantLock lock;
    private final Condition internetAccessCondition;


    public ConnectionHandler(VpnSettings vpnSettings, ParcelFileDescriptor vpnInterface, SSVpnService vpnService) {
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
        this.application = vpnService.getApplication();
        this.vpnService = vpnService;
        connected = new AtomicBoolean();
        lock = new ReentrantLock();
        internetAccessCondition = lock.newCondition();
        internetAccessHandler = new InternetAccessHandler(this);
    }

    @Override
    public void run() {
        serviceActive = true;
        Intent intent = new Intent("yes__");
        internetAccessHandler.setAccessChangeListener(accessType -> {
            LocalBroadcastManager.getInstance(application).sendBroadcast(intent);
        });
        internetAccessHandler.start();

        sshManager = new SSHManager(vpnSettings, this);
        tun2SocksManager = new Tun2SocksManager(vpnSettings, vpnInterface, application);
        sshManager.setupConnection();
        sshManager.startPortForwarding();
        tun2SocksManager.start();

        while (serviceActive) {
            connected.set(true);
            LocalBroadcastManager.getInstance(application).sendBroadcast(intent);
            sshManager.getSession().waitFor(Arrays.asList(ClientSession.ClientSessionEvent.CLOSED,
                    ClientSession.ClientSessionEvent.TIMEOUT), 0);
            connected.set(false);
            LocalBroadcastManager.getInstance(application).sendBroadcast(intent);
            // reconnect
            sshManager.setupConnection();
            sshManager.startPortForwarding();
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        serviceActive = false;
        // done in separate thread to avoid hanging the main thread
        tun2SocksManager.stop();
        new Thread(sshManager::close).start();
    }

    public void no() {

    }

    public void yes() {
        sshManager.close();
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

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isServiceActive() {
        return serviceActive;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public Condition getInternetAccessCondition() {
        return internetAccessCondition;
    }
}
