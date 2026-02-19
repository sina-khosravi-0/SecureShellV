package com.securelight.secureshellv.vpnservice.connection;

import static com.securelight.secureshellv.statics.Constants.apiHeartbeatPeriod;

import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;

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
import com.securelight.secureshellv.vpnservice.listeners.SocketProtector;
import com.securelight.secureshellv.vpnservice.listeners.SocksStateListener;
import com.securelight.secureshellv.vpnservice.v2ray.V2rayCoreManager;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;


public class ConnectionManager extends Thread {

    private final String TAG = getClass().getSimpleName();
    private final ParcelFileDescriptor vpnInterface;
    private final Context context;
    private final NotificationListener notificationListener;
    private final InterfaceErrorListener interfaceErrorListener;
    private final Timer socksTimer;
    private final V2rayCoreManager v2rayCoreManager;
    private final StatsHandler statsHandler;
    private final Timer sendTrafficTimer;
    private final Timer apiHeartbeatTimer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private SocketProtector socketProtector;
    private SetupListener setupListener;
    private SendTrafficTimeTask sendTrafficTask;
    private APIHeartbeatTask apiHeartbeatTask;
    private SocksHeartbeatTask socksHeartbeatTask;
    private ConnectionState connectionState = ConnectionState.CONNECTING;
    private NetworkState networkState = NetworkState.WORLD_WIDE;
    private boolean setupInProgress = false;
    private boolean tasksScheduled = false;
    private boolean statsStarted = false;
    private boolean stateChanged = true;

    public ConnectionManager(ParcelFileDescriptor vpnInterface, Context context,
                             SocketProtector socketProtector,
                             NotificationListener notificationListener,
                             V2rayCoreManager v2rayCoreManager,
                             StatsHandler statsHandler,
                             InterfaceErrorListener interfaceErrorListener) {
        this.vpnInterface = vpnInterface;
        this.context = context.getApplicationContext();
        this.socketProtector = socketProtector;
        this.notificationListener = notificationListener;
        this.interfaceErrorListener = interfaceErrorListener;
        this.v2rayCoreManager = v2rayCoreManager;
        this.statsHandler = statsHandler;
        socksTimer = new Timer();
        sendTrafficTimer = new Timer();
        apiHeartbeatTimer = new Timer();
        setupListener = () -> {
        };

    }

    @Override
    public void run() {
        setupInProgress = true;
        running.set(true);
        updateConnectionStateUI();
        startV2ray();
            setupListener.onSetupFinished();
        setupInProgress = false;
    }

    private void startV2ray() {
        try {
            vpnInterface.checkError();
        } catch (IOException e) {
            updateConnectionStateUI();
            interfaceErrorListener.onFoundInterfaceError();
            return;
        }
        v2rayCoreManager.initCore();
        boolean restart = v2rayCoreManager.isCoreRunning();
        if (restart) {
            v2rayCoreManager.stopLoop();
        }
        if (!running.get()) {
            return;
        }
        if (!loadV2rayConfig()) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.START_SERVICE_FAILED_ACTION));
            return;
        }

        try {
            v2rayCoreManager.startLoop(DataManager.getInstance().selectedConfig, vpnInterface.getFd());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (setupInProgress) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.START_SERVICE_FAILED_ACTION));
            }
            return;
        }
        stopStatsHandler();
        startStatsHandler();
        cancelTasks();
        scheduleTasks();
    }

    private void stopV2ray() {
        v2rayCoreManager.stopLoop();
        connectionState = ConnectionState.DISCONNECTED;
        updateConnectionStateUI();
        stopStatsHandler();
        cancelTasks();
    }

    private boolean loadV2rayConfig() {
        V2rayConfig config;
        try {
            String preferredLocation = SharedPreferencesSingleton.getInstance(context).getSelectedServerLocation();
            DataManager.getInstance().updateV2rayConfigs(preferredLocation);
            DataManager.getInstance().getV2rayConfigs().forEach(v2rayConfig -> v2rayConfig.checkConfigReachability(socketProtector));
            config = DataManager.getInstance().getBestV2rayConfig();
            if (config == null) {
                return false;
            }
            Utilities.refillV2rayConfig("BestConfig", config.getConfig(), null, true);
            DataManager.getInstance().selectedConfig = Utilities.currentConfig.fullJsonConfig;
        } catch (Exception e) {
            Log.e(TAG, "couldn't load v2ray config", e);
            return false;
        }

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
        scheduleSocksHeartbeatTask();
        scheduleApiHeartbeatTask();
        scheduleSendTrafficTask();
    }

    private void cancelTasks() {
        if (!tasksScheduled) {
            return;
        }
        tasksScheduled = false;
        apiHeartbeatTask.cancel();
        sendTrafficTask.cancel();
        socksHeartbeatTask.cancel();
    }

    private void scheduleSendTrafficTask() {
        sendTrafficTask = new SendTrafficTimeTask(statsHandler, DatabaseHandlerSingleton.getInstance(context), context);
        sendTrafficTimer.schedule(sendTrafficTask, 0, Constants.sendTrafficPeriod);
    }

    private void scheduleSocksHeartbeatTask() {
        socksHeartbeatTask = new SocksHeartbeatTask(running, socketProtector,v2rayCoreManager,
                new SocksStateListener() {
                    @Override
                    public void onSocksDown() {
                        updateConnectionStateUI();
                        stopV2ray();
                        startV2ray();
                    }

                    @Override
                    public void onSocksUp() {
                        updateConnectionStateUI();
                    }
                },
                networkState -> {
                    if (this.networkState != networkState) {
                        stateChanged = true;
                    }
                    this.networkState = networkState;
                },
                connectionState -> {
                    if (connectionState != this.connectionState) {
                        stateChanged = true;
                    }
                    this.connectionState = connectionState;
                });
        socksTimer.schedule(socksHeartbeatTask, 0, Constants.socksHeartbeatPeriod);
    }

    private void scheduleApiHeartbeatTask() {
        apiHeartbeatTask = new APIHeartbeatTask(context);
        apiHeartbeatTimer.schedule(apiHeartbeatTask, 0, apiHeartbeatPeriod);
    }

    @Override
    public void interrupt() {
        if (setupInProgress) {
            DataManager.getInstance().interruptConfigTestThreads();
            setupListener = () -> {
                running.set(false);
                stopV2ray();
            };

        } else {
            stopV2ray();
            running.set(false);
        }
    }

    public void no() {
        try {
            vpnInterface.close();
        } catch (IOException ignored) {
        }
    }

    private void updateConnectionStateUI() {
        if (!stateChanged) {
            return;
        }
        switch (connectionState) {
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
                networkState = null;
                break;
        }
        stateChanged = false;
        notificationListener.updateNotification(networkState, connectionState);
    }

    public void onNetworkAvailable() {
        if (socksHeartbeatTask != null) {
            socksHeartbeatTask.setNetworkIFaceAvailable(true);
        }
    }

    public void onNetworkLost() {
        if (socksHeartbeatTask != null) {
            socksHeartbeatTask.setNetworkIFaceAvailable(false);
        }
        stateChanged = true;
        connectionState = ConnectionState.CONNECTING;
        networkState = NetworkState.UNAVAILABLE;
        updateConnectionStateUI();
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public NetworkState getNetworkState() {
        return networkState;
    }

    private interface SetupListener {
        void onSetupFinished();
    }
}
