package com.securelight.secureshellv.vpnservice.connection;

import android.content.Context;

import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.statics.Constants;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class APIHeartbeatTask extends TimerTask {
    private final DatabaseHandlerSingleton databaseHandler;
    private final AtomicInteger nFailed = new AtomicInteger(0);

    public APIHeartbeatTask(Context context) {
        databaseHandler = DatabaseHandlerSingleton.getInstance(context);
    }

    @Override
    public void run() {
        Constants.ApiHeartbeatResult result = databaseHandler.sendHeartbeat();
        if (result == Constants.ApiHeartbeatResult.TIMEOUT) {
            if (nFailed.incrementAndGet() >= 3) {

            }
        }
    }
}
