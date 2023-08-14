package com.securelight.secureshellv.connection;

import android.app.Application;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;

import com.securelight.secureshellv.MainActivity;
import com.securelight.secureshellv.SSVpnService;
import com.securelight.secureshellv.Values;
import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.ssh.SSHManager;
import com.securelight.secureshellv.tun2socks.Tun2SocksManager;

import org.apache.sshd.client.session.ClientSession;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler extends Thread {
    public final String SSH_DIRECT = "ssh-direct";
    public final String SSH_TLS = "ssh-tls";
    private final String TAG = getClass().getSimpleName();
    private final VpnSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private SSHManager sshManager;
    private final Application application;
    private final SSVpnService vpnService;
    private Tun2SocksManager tun2SocksManager;
    private final InternetAccessHandler internetAccessHandler;
    private final AtomicBoolean connected;
    private final AtomicBoolean networkInterfaceAvailable;
    private final ReentrantLock lock;
    private final Condition internetAvailableCondition;
    public String connectionStateString = Values.CONNECTING_STRING;
    public String networkStateString = "";
    private boolean running;

    public ConnectionHandler(VpnSettings vpnSettings, ParcelFileDescriptor vpnInterface, SSVpnService vpnService) {
        setName("ConnectionHandler");
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
        this.application = vpnService.getApplication();
        this.vpnService = vpnService;
        lock = new ReentrantLock();
        connected = new AtomicBoolean();
        networkInterfaceAvailable = new AtomicBoolean();
        internetAvailableCondition = lock.newCondition();
        internetAccessHandler = new InternetAccessHandler(this);
        internetAccessHandler.setDaemon(true);
    }

    @Override
    public void run() {
        running = true;
        vpnService.setConnectionThreadRunning(true);

        internetAccessHandler.setAccessChangeListener(accessType -> updateNotification());
        internetAccessHandler.start();

        sshManager = new SSHManager(vpnSettings, this);
        tun2SocksManager = new Tun2SocksManager(vpnSettings, vpnInterface, this);
        connectionStateString = Values.CONNECTING_STRING;
        updateNotification();
        sshManager.setupConnection();
        sshManager.startPortForwarding();
        tun2SocksManager.start();

        while (isServiceActive()) {
            connected.set(true);
            connectionStateString = Values.CONNECTED_STRING;
            updateNotification();

            sshManager.getSession().waitFor(Arrays.asList(ClientSession.ClientSessionEvent.CLOSED,
                    ClientSession.ClientSessionEvent.TIMEOUT), 0);

            connected.set(false);
            connectionStateString = Values.CONNECTING_STRING;
            updateNotification();

            // reconnect
            sshManager.setupConnection();
            sshManager.startPortForwarding();
        }

        networkStateString = "";
        connectionStateString = Values.DISCONNECTED_STRING;
        updateNotification();

        // todo: preferably remove
        vpnService.onConnectionFinalized();

        vpnService.setConnectionThreadRunning(false);
        running = false;
    }

    @Override
    public void interrupt() {
        super.interrupt();

        tun2SocksManager.stop();
        // done in separate thread to avoid hanging the main thread
        new Thread(sshManager::closeAndFinalize).start();
    }

    public void no() {

    }

    private void updateNotification() {
        if (!MainActivity.isAppClosing()) {
            NotificationCompat.Builder builder = vpnService.getNotificationBuilder();
            builder.setContentTitle(connectionStateString);
            builder.setContentText(String
                    .format("%s: %s", Values.INTERNET_ACCESS_STATE_STRING, networkStateString));
            vpnService.getNotificationManager().notify(vpnService.getOnGoingNotificationID(), builder.build());
        }
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
        return vpnService.isServiceActive();
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public Condition getInternetAvailableCondition() {
        return internetAvailableCondition;
    }

    public boolean isNetworkIFaceAvailable() {
        return networkInterfaceAvailable.get();
    }

    public void setNetworkIFaceAvailable(boolean isAvailable) {
        networkInterfaceAvailable.set(isAvailable);
    }

    public void onNetworkAvailable() {
        internetAccessHandler.wakeup();
    }

    public boolean isRunning() {
        return running;
    }
}
