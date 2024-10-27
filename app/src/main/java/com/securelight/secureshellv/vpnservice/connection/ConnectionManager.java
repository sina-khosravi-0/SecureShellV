package com.securelight.secureshellv.vpnservice.connection;

import static com.securelight.secureshellv.statics.Constants.apiHeartbeatPeriod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.backend.SendTrafficTimeTask;
import com.securelight.secureshellv.backend.V2rayConfig;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;
import com.securelight.secureshellv.utility.Utilities;
import com.securelight.secureshellv.vpnservice.StatsHandler;
import com.securelight.secureshellv.vpnservice.listeners.InterfaceErrorListener;
import com.securelight.secureshellv.vpnservice.listeners.NotificationListener;
import com.securelight.secureshellv.vpnservice.listeners.SocksStateListener;

import org.json.JSONException;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;
import dev.dev7.lib.v2ray.utils.V2rayConfigs;
import dev.dev7.lib.v2ray.utils.V2rayConstants;

public class ConnectionManager extends Thread {

    private final String TAG = getClass().getSimpleName();
    private final ParcelFileDescriptor vpnInterface;
    private final Context context;
    private final AtomicBoolean networkInterfaceAvailable;
    private final ReentrantLock lock;
    private final Condition internetAvailableCondition;
    private final NotificationListener notificationListener;
    private final InterfaceErrorListener interfaceErrorListener;
    private final Timer internetTimer;
    private final Timer socksTimer;
    private final V2rayCoreExecutor v2rayCoreExecutor;
    private final StatsHandler statsHandler;
    private final Timer sendTrafficTimer;
    private final Timer apiHeartbeatTimer;
    private SendTrafficTimeTask sendTrafficTask;
    private APIHeartbeatTask apiHeartbeatTask;
    private SocksHeartbeatTask socksHeartbeatTask;
    private InternetAccessTask internetAccessTask;
    //    private SshManager sshManager;
//    private StunnelManager stunnelManager;
//    private Constants.Protocol connectionMethod = Constants.Protocol.DIRECT_SSH;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private NetworkState networkState = NetworkState.NO_ACCESS;
    private boolean running;
    private boolean tasksScheduled = false;
    private boolean statsStarted = false;
    private boolean socksDown = false;

    public ConnectionManager(ParcelFileDescriptor vpnInterface, Context context,
                             NotificationListener notificationListener,
                             V2rayCoreExecutor v2rayCoreExecutor,
                             StatsHandler statsHandler,
                             InterfaceErrorListener interfaceErrorListener) {
        this.vpnInterface = vpnInterface;
        this.context = context.getApplicationContext();
        this.notificationListener = notificationListener;
        this.interfaceErrorListener = interfaceErrorListener;
        this.v2rayCoreExecutor = v2rayCoreExecutor;
        this.statsHandler = statsHandler;
        lock = new ReentrantLock();
        networkInterfaceAvailable = new AtomicBoolean();
        internetAvailableCondition = lock.newCondition();
        internetTimer = new Timer();
        socksTimer = new Timer();
        sendTrafficTimer = new Timer();
        apiHeartbeatTimer = new Timer();
    }

    @Override
    public void run() {
        running = true;
        updateConnectionStateUI(ConnectionState.CONNECTING);
        boolean isLoaded = loadV2rayConfig();
        if (!isLoaded) {
            return;
        }
        startV2ray();
    }

    private void startV2ray() {
        try {
            vpnInterface.checkError();
        } catch (IOException e) {
            interfaceErrorListener.onFoundInterfaceError();
            return;
        }

        boolean restart = v2rayCoreExecutor.getCoreState() == V2rayConstants.CORE_STATES.IDLE ||
                v2rayCoreExecutor.getCoreState() == V2rayConstants.CORE_STATES.RUNNING;
        if (restart) {
            v2rayCoreExecutor.stopCore(false);
        }
        if (!running) {
            return;
        }
        v2rayCoreExecutor.startCore(V2rayConfigs.currentConfig);
        stopStatsHandler();
        startStatsHandler();
        cancelTasks();
        scheduleTasks();
    }

    private void stopV2ray() {
        v2rayCoreExecutor.stopCore(false);
        stopStatsHandler();
        cancelTasks();
    }

    private void restartV2ray() {
        loadV2rayConfig();
        startV2ray();
    }

    private boolean loadV2rayConfig() {
        V2rayConfig config;
        try {
            String preferredLocation = SharedPreferencesSingleton.getInstance(context).getSelectedServerLocation();
            config = Utilities.getBestV2rayConfig(DataManager.getInstance().updateV2rayConfigs(preferredLocation));
            if (config == null) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.START_SERVICE_FAILED_ACTION));
                return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, "couldn't load v2ray config", e);
            throw new RuntimeException(e);
        }

        Utilities.refillV2rayConfig("BestConfig", config.getJson(), null, true);
        return true;
    }

    private void startStatsHandler() {
        if (statsStarted) {
            return;
        }
        statsStarted = true;
        statsHandler.start();
    }

    private void stopStatsHandler() {
        if (!statsStarted) {
            return;
        }
        statsStarted = false;
        statsHandler.stop();
    }

    private void scheduleTasks() {
        if (tasksScheduled) {
            return;
        }
        tasksScheduled = true;
        scheduleInternetAccessTask();
        scheduleSocksHeartbeatTask();
        scheduleApiHeartbeatTask();
        scheduleSendTrafficTask();
    }

    private void scheduleSendTrafficTask() {
        sendTrafficTask = new SendTrafficTimeTask(statsHandler, DatabaseHandlerSingleton.getInstance(context), context);
        sendTrafficTimer.schedule(sendTrafficTask, 0, Constants.sendTrafficPeriod);
    }

    private void scheduleInternetAccessTask() {
        internetAccessTask = new InternetAccessTask(lock, internetAvailableCondition);
        internetAccessTask.setAccessChangeListener(networkState -> {
            this.networkState = networkState;
            notificationListener.updateNotification(networkState, connectionState);
        });
        internetTimer.schedule(internetAccessTask, 0, Constants.internetAccessPeriod);
    }

    private void scheduleSocksHeartbeatTask() {
        socksHeartbeatTask = new SocksHeartbeatTask(this::updateConnectionStateUI,
                new SocksStateListener() {
                    @Override
                    public void onSocksDown() {
                        switch (Utilities.checkAndGetAccessType(networkInterfaceAvailable.get())) {
                            case RESTRICTED:
                            case WORLD_WIDE:
                                if (running) {
                                    restartV2ray();
                                }
                                break;
                            case NONE:
                            case UNAVAILABLE:
                            case NO_ACCESS:
                                stopV2ray();
                                break;
                        }
                        socksDown = true;
                    }

                    @Override
                    public void onSocksUp() {
//                        if (running && socksDown) {
//                            System.out.println("hello");
//                            startV2ray();
//                        }
//                        socksDown = false;
                    }
                }, v2rayCoreExecutor);
        socksTimer.schedule(socksHeartbeatTask, 0, Constants.socksHeartbeatPeriod);
    }

    private void scheduleApiHeartbeatTask() {
        apiHeartbeatTask = new APIHeartbeatTask(context);
        apiHeartbeatTimer.schedule(apiHeartbeatTask, 0, apiHeartbeatPeriod);
    }

    private void cancelTasks() {
        if (!tasksScheduled) {
            return;
        }
        tasksScheduled = false;
        internetAccessTask.cancel();
        socksHeartbeatTask.cancel();
        apiHeartbeatTask.cancel();
        sendTrafficTask.cancel();
    }

    @Override
    public void interrupt() {
        running = false;
        stopV2ray();

    }

//    @Override
//    public void run() {
//        running = true;
//        updateConnectionStateUI(ConnectionState.CONNECTING);
//
//        // run internet access timer
//        internetTimer.schedule(internetAccessHandler, 0, Constants.internetAccessPeriod);
//
//        // calculate the best server and put it in DataManager.bestServer
//        if (!DataManager.getInstance().calculateBestServer()) {
//            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SSVpnService.STOP_VPN_SERVICE_ACTION));
//            updateConnectionStateUI(ConnectionState.DISCONNECTED);
//            return;
//        }
//
//        boolean bridge = false;
//        switch (DataManager.getInstance().getBestServer().getType()) {
//            case M:
//                setupDirectSsh();
//                break;
//            case N:
//                setupTLSSsh();
//                break;
//        }
//
//        tun2SocksManager = new Tun2SocksManager(vpnInterface, t2SListener);
//
//        connect(bridge);
//        if (!interrupted) {
//            sshManager.createPortForwarding();
//            sendTrafficTimer.scheduleAtFixedRate(sendTrafficHandler, 0, Constants.sendTrafficPeriod);
//        }
//        tun2SocksManager.run();
//
//        SocksHeartbeatHandler socksHeartbeatHandler = new SocksHeartbeatHandler(sshManager);
//        // run socks heartbeat timer
//        socksTimer.schedule(socksHeartbeatHandler, 0, Constants.socksHeartbeatPeriod);
//
//        // try to keep the connection alive till the thread is interrupted
//        while (!interrupted) {
//            updateConnectionStateUI(ConnectionState.CONNECTED);
//
//            sshManager.getSession().waitFor(Arrays.asList(ClientSession.ClientSessionEvent.CLOSED,
//                    ClientSession.ClientSessionEvent.TIMEOUT), 0);
//            sshManager.setEstablished(false);
//
//            sendTrafficTimer.cancel();
//            sendTrafficTimer = new Timer();
//            sendTrafficHandler = new SendTrafficTimeTask(context);
//
//            updateConnectionStateUI(ConnectionState.CONNECTING);
//
//            connect(bridge);
//            if (!interrupted) {
//                sshManager.createPortForwarding();
//                sendTrafficTimer.scheduleAtFixedRate(sendTrafficHandler, 0, Constants.sendTrafficPeriod);
//            }
//        } // while (!interrupted)
//
//        updateConnectionStateUI(ConnectionState.DISCONNECTED);
//
//        sendTrafficTimer.cancel();
//        socksTimer.cancel();
//        internetTimer.cancel();
//        try {
//            sshManager.getSshClient().close();
//        } catch (IOException ignored) {
//        }
//        running = false;
//    }

    /**
     * blocks until connection is successful
     */
//    private void connect(boolean bridge) {
//        boolean reset = false;
//        while (!sshManager.isEstablished() && !interrupted) {
//            try {
//                if (bridge) {
//                    sshManager.connectWithBridge(String.valueOf(DataManager.getInstance().getSshPassword(reset)));
//                } else {
//                    sshManager.connect(String.valueOf(DataManager.getInstance().getSshPassword(reset)));
//                }
//            } catch (SshException sE) {
//                reset = Objects.equals(sE.getMessage(), "No more authentication methods available");
//                Log.d("SshManager", sE.getMessage(), sE);
//            } catch (IOException ioE) {
//                Log.d("SshManager", ioE.getMessage(), ioE);
//            }
//        }
//    }
//
//    private void setupDirectSsh() {
//        sshManager = new SshManager(lock,
//                internetAvailableCondition,
//                new SshConfigs(DataManager.getInstance().getBestServer().getIp(),
//                        DataManager.getInstance().getBestServer().getPingPort(),
//                        VpnSettings.iFaceAddress,
//                        VpnSettings.socksPort,
//                        Constants.Protocol.DIRECT_SSH));
//    }
//
//    private void setupTLSSsh() {
//        int port;
//        try (ServerSocket serverSocket = new ServerSocket(0)) {
//            port = serverSocket.getLocalPort();
//        } catch (IOException e) {
//            // noter options left we're fucked
//            throw new RuntimeException("no free port");
//        }
//        stunnelManager = new StunnelManager(context.getApplicationContext());
//        stunnelManager.open(DataManager.getInstance().getBestServer().getIp(),
//                DataManager.getInstance().getBestServer().getPingPort(),
//                port);
//
//        sshManager = new SshManager(lock,
//                internetAvailableCondition,
//                new SshConfigs("127.0.0.1",
//                        port,
//                        VpnSettings.iFaceAddress,
//                        VpnSettings.socksPort,
//                        Constants.Protocol.TLS_SSH));
//    }
//
//    private void setupDualSsh() {
//        int port;
//        try (ServerSocket serverSocket = new ServerSocket(0)) {
//            port = serverSocket.getLocalPort();
//        } catch (IOException e) {
//            // we're done
//            throw new RuntimeException("no free port");
//        }
//        sshManager = new SshManager(lock,
//                internetAvailableCondition,
//                new SshConfigs("127.0.0.1",
//                        port,
//                        VpnSettings.iFaceAddress,
//                        VpnSettings.socksPort,
//                        DataManager.getInstance().getBestServer().getIp(),
//                        DataManager.getInstance().getBestServer().getPingPort(),
//                        Constants.Protocol.DUAL_SSH));
//    }


//    @Override
//    public void interrupt() {
//        interrupted = true;
//        super.interrupt();
//        try {
//            tun2SocksManager.stop();
//        } catch (NullPointerException ignored) {
//        }
//        try {
//            sshManager.getSshClient().stop();
//        } catch (NullPointerException ignored) {
//        }
//        // ensure no stuckage
//        new Thread(() -> {
//            try {
//                while (running) {
//                    sshManager.clearLock();
//                }
//            } catch (NullPointerException ignored) {
//            }
//        }).run();
//        try {
//            if (connectionMethod == Constants.Protocol.TLS_SSH) {
//                stunnelManager.close();
//            }
//        } catch (NullPointerException ignored) {
//        }
//    }

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
        if (state != connectionState) {
            switch (state) {
                case CONNECTED:
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            new Intent(Intents.CONNECTED_ACTION));
                    break;
                case CONNECTING:
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            new Intent(Intents.CONNECTING_ACTION));
                    break;
                case DISCONNECTED:
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            new Intent(Intents.DISCONNECTED_ACTION));
                    networkState = NetworkState.NONE;
                    break;
            }
            connectionState = state;
            notificationListener.updateNotification(networkState, state);
        }
    }

//    public Tun2SocksManager getTun2SocksManager() {
//        return tun2SocksManager;
//    }

//    public InternetAccessHandler getInternetAccessHandler() {
//        return internetAccessHandler;
//    }

//    public SshManager getSshManager() {
//        return sshManager;
//    }

//    public boolean isConnected() {
//        return connected.get();
//    }

//    public ReentrantLock getLock() {
//        return lock;
//    }

//    public Condition getInternetAvailableCondition() {
//        return internetAvailableCondition;
//    }

//    public boolean isNetworkIFaceAvailable() {
//        return networkInterfaceAvailable.get();
//    }

    public void setNetworkIFaceAvailable(boolean isAvailable) {
        networkInterfaceAvailable.set(isAvailable);
    }

    public void onNetworkAvailable() {
        if (internetAccessTask != null) {
            internetAccessTask.setNetworkIFaceAvailable(true);
            internetAccessTask.wakeup();
        }
        startV2ray();
    }

    public void onNetworkLost() {
        internetAccessTask.setNetworkIFaceAvailable(false);
        stopV2ray();
        updateConnectionStateUI(ConnectionState.CONNECTING);
        tasksScheduled = false;
    }

//    public boolean isRunning() {
//        return running;
//    }

//    public Constants.Protocol getConnectionMethod() {
//        return connectionMethod;
//    }

//    public void setConnectionMethod(Constants.Protocol connectionMethod) {
//        this.connectionMethod = connectionMethod;
//    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public NetworkState getNetworkState() {
        return networkState;
    }
}
