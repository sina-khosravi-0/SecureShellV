package com.securelight.secureshellv.vpnservice.connection;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.BackendHandlerSingleton;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.statics.Intents;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class APIHeartbeatTask extends TimerTask {
    private final Context context;
    private final BackendHandlerSingleton databaseHandler;
    private final AtomicInteger nFailed = new AtomicInteger(0);

    public APIHeartbeatTask(Context context) {
        this.context = context;
        databaseHandler = BackendHandlerSingleton.getInstance(context);
    }

    @Override
    public void run() {
        Constants.ApiHeartbeatResult result = databaseHandler.sendHeartbeat();
        if (result == Constants.ApiHeartbeatResult.TIMEOUT || result == Constants.ApiHeartbeatResult.ERROR) {
            if (nFailed.incrementAndGet() >= 3) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        new Intent(Intents.START_SERVICE_FAILED_ACTION)
                                .putExtra("message", context.getString(R.string.fail_to_connect_to_server)));
            }
        } else if (result == Constants.ApiHeartbeatResult.SHOULD_RESTART) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                    new Intent(Intents.RESTART_V2RAY_SERVICE_ACTION)
                            .putExtra("message", context.getString(R.string.restarting_connection)));
        }
    }
}
