package com.securelight.secureshellv.vpnservice.connection;

import static com.securelight.secureshellv.statics.Constants.internetAccessPeriod;
import static com.securelight.secureshellv.statics.Constants.sendTrafficPeriod;
import static com.securelight.secureshellv.statics.Constants.socksHeartbeatPeriod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.MainActivity;
import com.securelight.secureshellv.StunnelManager;
import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.backend.SendTrafficTimeTask;
import com.securelight.secureshellv.ssh.SshConfigs;
import com.securelight.secureshellv.ssh.SshManager;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.tun2socks.Tun2SocksManager;
import com.securelight.secureshellv.vpnservice.SSVpnService;
import com.securelight.secureshellv.vpnservice.VpnSettings;
import com.securelight.secureshellv.vpnservice.listeners.NotificationListener;
import com.securelight.secureshellv.vpnservice.listeners.Tun2SocksListener;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler extends Thread {

    private final String TAG = getClass().getSimpleName();
    private final ParcelFileDescriptor vpnInterface;
    private final Context context;
    private final InternetAccessHandler internetAccessHandler;
    private final AtomicBoolean connected;
    private final AtomicBoolean networkInterfaceAvailable;
    private final ReentrantLock lock;
    private final Condition internetAvailableCondition;
    private final NotificationListener notificationListener;
    private final Timer internetTimer;
    private final Timer socksTimer;
    private Timer sendTrafficTimer;
    private Timer apiHeartbeatTimer;
    private Tun2SocksManager tun2SocksManager;
    private SendTrafficTimeTask sendTrafficHandler;
    private APIHeartbeatHandler apiHeartbeatHandler;
    private SshManager sshManager;
    private StunnelManager stunnelManager;
    private Constants.Protocol connectionMethod = Constants.Protocol.DIRECT_SSH;
    private ConnectionState connectionState = ConnectionState.CONNECTING;
    private NetworkState networkState = NetworkState.NO_ACCESS;
    private SharedPreferences sharedPreferences;
    private boolean running;
    private boolean interrupted = false;
    private final Tun2SocksListener t2SListener = new Tun2SocksListener() {
        @Override
        public void onTun2SocksStopped() {
            if (!interrupted) {
                tun2SocksManager.start();
            }
        }
    };

    public ConnectionHandler(ParcelFileDescriptor vpnInterface, Context context,
                             NotificationListener notificationListener) {
        setName("conn-handler");
        this.vpnInterface = vpnInterface;
        this.context = context.getApplicationContext();
        this.notificationListener = notificationListener;
        lock = new ReentrantLock();
        connected = new AtomicBoolean();
        networkInterfaceAvailable = new AtomicBoolean();
        internetAvailableCondition = lock.newCondition();
        internetTimer = new Timer();
        socksTimer = new Timer();
        sendTrafficTimer = new Timer();
        sendTrafficHandler = new SendTrafficTimeTask(context);
        internetAccessHandler = new InternetAccessHandler(lock, internetAvailableCondition);
        internetAccessHandler.setAccessChangeListener(networkState -> {
            this.networkState = networkState;
            notificationListener.updateNotification(networkState, connectionState);
        });
        sharedPreferences = context.getSharedPreferences(MainActivity.CONNECTION_INFO_PREF, Context.MODE_PRIVATE);
    }

    @Override
    public void run() {
        running = true;
        int retryCount = 0;
        boolean reset = false;
        updateConnectionStateUI(ConnectionState.CONNECTING);

        // start internet access timer
        internetTimer.schedule(internetAccessHandler, 0, internetAccessPeriod);

        // calculate the best server and put it in DataManager.bestServer
        if (!DataManager.getInstance().calculateBestServer()) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SSVpnService.STOP_VPN_SERVICE_ACTION));
            updateConnectionStateUI(ConnectionState.DISCONNECTED);
            return;
        }

        boolean bridge = false;
        switch (DataManager.getInstance().getBestServer().getType()) {
            case D:
                setupDirectSsh();
                break;
            case TD:
                break;
            case TH:
                setupTLSSsh();
                break;
            case DH:
                setupDualSsh();
                bridge = true;
                break;

        }

        tun2SocksManager = new Tun2SocksManager(vpnInterface, t2SListener);

        while (!sshManager.isEstablished() && !interrupted) {
            try {
                if (bridge) {
                    sshManager.connectWithBridge(String.valueOf(DataManager.getInstance().getSshPassword(reset)));
                } else {
                    sshManager.connect(String.valueOf(DataManager.getInstance().getSshPassword(reset)));
                }
            } catch (SshException sE) {
                if (Objects.equals(sE.getMessage(), "No more authentication methods available")) {
                    // retry 3 times and reset the password
                    if (retryCount > 2) {
                        retryCount = 0;
                        reset = true;
                    } else {
                        reset = false;
                        retryCount++;
                    }
                }
                Log.d("SshManager", sE.getMessage(), sE);
            } catch (IOException ioE) {
                Log.d("SshManager", ioE.getMessage(), ioE);
            }
        }
        if (!interrupted) {
            sshManager.createPortForwarding();
            sendTrafficTimer.scheduleAtFixedRate(sendTrafficHandler, 0, sendTrafficPeriod);
        }
        tun2SocksManager.start();

        SocksHeartbeatHandler socksHeartbeatHandler = new SocksHeartbeatHandler(sshManager);
        // start socks heartbeat timer
        socksTimer.schedule(socksHeartbeatHandler, 0, socksHeartbeatPeriod);

        while (!interrupted) {
            updateConnectionStateUI(ConnectionState.CONNECTED);

            sshManager.getSession().waitFor(Arrays.asList(ClientSession.ClientSessionEvent.CLOSED,
                    ClientSession.ClientSessionEvent.TIMEOUT), 0);
            sshManager.setEstablished(false);

            sendTrafficTimer.cancel();
            sendTrafficTimer = new Timer();
            sendTrafficHandler = new SendTrafficTimeTask(context);

            updateConnectionStateUI(ConnectionState.CONNECTING);

            // try reconnecting till SshManager.established is true
            while (!sshManager.isEstablished() && !interrupted) {
                try {
                    if (bridge) {
                        sshManager.connectWithBridge(String.valueOf(DataManager.getInstance().getSshPassword(reset)));
                    } else {
                        sshManager.connect(String.valueOf(DataManager.getInstance().getSshPassword(reset)));
                    }
                } catch (SshException sE) {
                    if (Objects.equals(sE.getMessage(), "No more authentication methods available")) {
                        if (retryCount == 2) {
                            retryCount = 0;
                            reset = true;
                        } else {
                            reset = false;
                            retryCount++;
                        }
                    }
                    Log.d("SshManager", sE.getMessage(), sE);
                } catch (IOException ioE) {
                    Log.d("SshManager", ioE.getMessage(), ioE);
                }
            }
            if (!interrupted) {
                sshManager.createPortForwarding();
                sendTrafficTimer.scheduleAtFixedRate(sendTrafficHandler, 0, sendTrafficPeriod);
            }
        } // while (!interrupted)

        updateConnectionStateUI(ConnectionState.DISCONNECTED);

        sendTrafficTimer.cancel();
        socksTimer.cancel();
        internetTimer.cancel();
        try {
            sshManager.getSshClient().close();
        } catch (IOException ignored) {
        }
        running = false;
    }

    private void setupDirectSsh() {
        sshManager = new SshManager(lock,
                internetAvailableCondition,
                new SshConfigs(DataManager.getInstance().getBestServer().getIp(),
                        DataManager.getInstance().getBestServer().getPort(),
                        VpnSettings.iFaceAddress,
                        VpnSettings.socksPort,
                        Constants.Protocol.DIRECT_SSH));
    }

    private void setupTLSSsh() {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("no free port");
        }
        stunnelManager = new StunnelManager(context.getApplicationContext());
        stunnelManager.open(DataManager.getInstance().getBestServer().getIp(),
                DataManager.getInstance().getBestServer().getPort(),
                port);

        sshManager = new SshManager(lock,
                internetAvailableCondition,
                new SshConfigs("127.0.0.1",
                        port,
                        VpnSettings.iFaceAddress,
                        VpnSettings.socksPort,
                        Constants.Protocol.TLS_SSH));
    }

    private void setupDualSsh() {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("no free port");
        }
        sshManager = new SshManager(lock,
                internetAvailableCondition,
                new SshConfigs("127.0.0.1",
                        port,
                        VpnSettings.iFaceAddress,
                        VpnSettings.socksPort,
                        DataManager.getInstance().getBestServer().getIp(),
                        DataManager.getInstance().getBestServer().getPort(),
                        Constants.Protocol.DUAL_SSH));
    }

    @Override
    public void interrupt() {
        interrupted = true;
        super.interrupt();
        try {
            tun2SocksManager.stop();
        } catch (NullPointerException ignored) {
        }
        try {
            sshManager.getSshClient().stop();
        } catch (NullPointerException ignored) {
        }
        // ensure no stuckage
        new Thread(() -> {
            try {
                while (running) {
                    sshManager.clearLock();
                }
            } catch (NullPointerException ignored) {
            }
        }).start();
        try {
            if (connectionMethod == Constants.Protocol.TLS_SSH) {
                stunnelManager.close();
            }
        } catch (NullPointerException ignored) {
        }
    }

    public void no() {
        try {
            vpnInterface.close();
        } catch (IOException ignored) {
        }
    }

    public void yes() {
        SharedPreferences preferences = context.getSharedPreferences("tun2socksDEATH", Activity.MODE_PRIVATE);
        Toast.makeText(context, preferences.getString("died", "N/A"), Toast.LENGTH_SHORT).show();
    }

    private void updateConnectionStateUI(ConnectionState state) {
        switch (state) {
            case CONNECTED:
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        new Intent(SSVpnService.CONNECTED_ACTION));
                break;
            case CONNECTING:
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        new Intent(SSVpnService.CONNECTING_ACTION));
                break;
            case DISCONNECTED:
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        new Intent(SSVpnService.DISCONNECTED_ACTION));
                networkState = NetworkState.NONE;
                break;
        }
        connectionState = state;
        notificationListener.updateNotification(networkState, state);
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

    public boolean isConnected() {
        return connected.get();
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
        internetAccessHandler.setNetworkIFaceAvailable(true);
        internetAccessHandler.wakeup();
    }

    public void onNetworkLost() {
        internetAccessHandler.setNetworkIFaceAvailable(false);
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public Constants.Protocol getConnectionMethod() {
        return connectionMethod;
    }

    public void setConnectionMethod(Constants.Protocol connectionMethod) {
        this.connectionMethod = connectionMethod;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public NetworkState getNetworkState() {
        return networkState;
    }
}
