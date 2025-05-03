package com.securelight.secureshellv.vpnservice.connection;

import static com.securelight.secureshellv.statics.Constants.apiHeartbeatPeriod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
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

import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;
import dev.dev7.lib.v2ray.utils.V2rayConfigs;
import dev.dev7.lib.v2ray.utils.V2rayConstants;

public class ConnectionManager extends Thread {

    private final String TAG = getClass().getSimpleName();
    private final ParcelFileDescriptor vpnInterface;
    private final Context context;
    private final NotificationListener notificationListener;
    private final InterfaceErrorListener interfaceErrorListener;
    private final Timer socksTimer;
    private final V2rayCoreExecutor v2rayCoreExecutor;
    private final StatsHandler statsHandler;
    private final Timer sendTrafficTimer;
    private final Timer apiHeartbeatTimer;
    private SetupListener setupListener;
    private SendTrafficTimeTask sendTrafficTask;
    private APIHeartbeatTask apiHeartbeatTask;
    private SocksHeartbeatTask socksHeartbeatTask;
    private ConnectionState connectionState = ConnectionState.CONNECTING;
    private NetworkState networkState = NetworkState.WORLD_WIDE;
    private AtomicBoolean running = new AtomicBoolean(false);
    private boolean setupInProgress = false;
    private boolean tasksScheduled = false;
    private boolean statsStarted = false;
    private boolean stateChanged = true;

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
        boolean isLoaded = loadV2rayConfig();
        if (!isLoaded) {
            return;
        }
        startV2ray();
        scheduleSocksHeartbeatTask();
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

        boolean restart = v2rayCoreExecutor.getCoreState() == V2rayConstants.CORE_STATES.IDLE ||
                v2rayCoreExecutor.getCoreState() == V2rayConstants.CORE_STATES.RUNNING;
        if (restart) {
            v2rayCoreExecutor.stopCore(false);
        }
        if (!running.get()) {
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
        connectionState = ConnectionState.DISCONNECTED;
        updateConnectionStateUI();
        stopStatsHandler();
        cancelTasks();
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
    }

    private void scheduleSendTrafficTask() {
        sendTrafficTask = new SendTrafficTimeTask(statsHandler, DatabaseHandlerSingleton.getInstance(context), context);
        sendTrafficTimer.schedule(sendTrafficTask, 0, Constants.sendTrafficPeriod);
    }

    private void scheduleSocksHeartbeatTask() {
        socksHeartbeatTask = new SocksHeartbeatTask(running, v2rayCoreExecutor,
                new SocksStateListener() {
                    @Override
                    public void onSocksDown() {
                        updateConnectionStateUI();
                        stopV2ray();
                        loadV2rayConfig();
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
            setupListener = () -> {
                running.set(false);
                stopV2ray();
                socksHeartbeatTask.cancel();
            };
            return;
        }
        running.set(false);
        stopV2ray();
        socksHeartbeatTask.cancel();
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
