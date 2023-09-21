package com.securelight.secureshellv.connection;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.securelight.secureshellv.Constants;
import com.securelight.secureshellv.MainActivity;
import com.securelight.secureshellv.SSVpnService;
import com.securelight.secureshellv.StunnelManager;
import com.securelight.secureshellv.Values;
import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.ssh.SshConfigs;
import com.securelight.secureshellv.ssh.SshManager;
import com.securelight.secureshellv.tun2socks.Tun2SocksManager;

import org.apache.sshd.client.session.ClientSession;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler extends Thread {
    private final String TAG = getClass().getSimpleName();
    private final VpnSettings vpnSettings;
    private final ParcelFileDescriptor vpnInterface;
    private final SSVpnService vpnService;
    private final InternetAccessHandler internetAccessHandler;
    private final SocksHeartbeatHandler socksHeartbeatHandler;
    private final AtomicBoolean connected;
    private final AtomicBoolean networkInterfaceAvailable;
    private final ReentrantLock lock;
    private final Condition internetAvailableCondition;
    private Tun2SocksManager tun2SocksManager;
    private SshManager sshManager;
    private StunnelManager stunnelManager;
    private Constants.Protocol connectionMethod = Constants.Protocol.DIRECT_SSH;
    public String connectionStateString = Values.CONNECTING_STRING;
    public String networkStateString = "";
    private boolean running;

    public ConnectionHandler(VpnSettings vpnSettings, ParcelFileDescriptor vpnInterface, SSVpnService vpnService) {
        setName("conn-handler");
        this.vpnSettings = vpnSettings;
        this.vpnInterface = vpnInterface;
        this.vpnService = vpnService;
        lock = new ReentrantLock();
        connected = new AtomicBoolean();
        networkInterfaceAvailable = new AtomicBoolean();
        internetAvailableCondition = lock.newCondition();
        internetAccessHandler = new InternetAccessHandler(this);
        socksHeartbeatHandler = new SocksHeartbeatHandler(this);
        internetAccessHandler.setAccessChangeListener(accessType -> updateNotification());
    }

    @Override
    public void run() {
        running = true;
        vpnService.setConnectionThreadRunning(true);
        connectionStateString = Values.CONNECTING_STRING;
        updateNotification();

        boolean bridge = false;
        switch (connectionMethod) {
            case DIRECT_SSH:
                setupDirect();
                break;
            case TLS_SSH:
                setupTLS();
                break;
            case DUAL_SSH:
                setupDualSsh();
                bridge = true;
                break;
        }

        tun2SocksManager = new Tun2SocksManager(vpnSettings, vpnInterface, this);

        Timer internetTimer = new Timer();
        Timer socksTimer = new Timer();
        internetTimer.schedule(internetAccessHandler, 0, 1000);

        if (bridge) {
            sshManager.connectWithBridge();
        } else {
            sshManager.connect();
        }
        sshManager.createPortForwarding();
        tun2SocksManager.start();

        socksTimer.schedule(socksHeartbeatHandler, 0, 3000);

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
            if (bridge) {
                sshManager.connectWithBridge();
            } else {
                sshManager.connect();
            }
            sshManager.createPortForwarding();
        }

        networkStateString = "";
        connectionStateString = Values.DISCONNECTED_STRING;
        updateNotification();

        // todo: remove
        vpnService.onConnectionFinalized();

        socksTimer.cancel();
        internetTimer.cancel();

        vpnService.setConnectionThreadRunning(false);
        running = false;
    }

    private void setupDirect() {
        sshManager = new SshManager(vpnSettings,
                this,
                // todo: fetch from server
                new SshConfigs("64.226.64.126",
                        22,
                        vpnSettings.getIFaceAddress(),
                        vpnSettings.getSocksPort(),
                        Constants.Protocol.DIRECT_SSH));
    }

    private void setupTLS() {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("no free port");
        }
        stunnelManager = new StunnelManager(vpnService.getApplicationContext());
        // todo: fetch from server
        stunnelManager.open("one.weary.tech", 80, port);
        sshManager = new SshManager(vpnSettings,
                this,
                new SshConfigs("127.0.0.1",
                        port,
                        vpnSettings.getIFaceAddress(),
                        vpnSettings.getSocksPort(),
                        Constants.Protocol.TLS_SSH));
    }

    private void setupDualSsh() {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("no free port");
        }
        sshManager = new SshManager(vpnSettings,
                this,
                // todo: fetch from server
                new SshConfigs("127.0.0.1",
                        port,
                        vpnSettings.getIFaceAddress(),
                        vpnSettings.getSocksPort(),
                        "one.weary.tech",
                        22,
                        Constants.Protocol.DUAL_SSH));
    }

    @Override
    public void interrupt() {
        super.interrupt();

        tun2SocksManager.stop();
        // done in separate thread to avoid hanging the main thread
        new Thread(sshManager::closeAndFinalize).start();
        if (connectionMethod == Constants.Protocol.TLS_SSH) {
            stunnelManager.close();
        }
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
        SharedPreferences preferences = vpnService.getSharedPreferences("tun2socksDEATH", Activity.MODE_PRIVATE);
        Toast.makeText(vpnService, preferences.getString("died", "N/A"), Toast.LENGTH_SHORT).show();
    }

    public void tun2socksDied() {
        SharedPreferences preferences = vpnService.getSharedPreferences("tun2socksDEATH", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("died", "yes");
        editor.apply();
    }

    public void clearTun2socksDeath() {
        SharedPreferences preferences = vpnService.getSharedPreferences("tun2socksDEATH", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("died");
        editor.apply();
    }

    public Tun2SocksManager getTun2SocksManager() {
        return tun2SocksManager;
    }

    public InternetAccessHandler getInternetAccessHandler() {
        return internetAccessHandler;
    }

    public SshManager getSshManager() {
        return sshManager;
    }

    public VpnSettings getVpnSettings() {
        return vpnSettings;
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

    public Constants.Protocol getConnectionMethod() {
        return connectionMethod;
    }

    public void setConnectionMethod(Constants.Protocol connectionMethod) {
        this.connectionMethod = connectionMethod;
    }
}
