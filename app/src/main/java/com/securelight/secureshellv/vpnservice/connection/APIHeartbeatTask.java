package com.securelight.secureshellv.vpnservice.connection;

import android.content.Context;

import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;

import java.util.TimerTask;

public class APIHeartbeatTask extends TimerTask {
    private final DatabaseHandlerSingleton databaseHandler;

    public APIHeartbeatTask(Context context) {
        databaseHandler = DatabaseHandlerSingleton.getInstance(context);
    }

    @Override
    public void run() {
        databaseHandler.sendHeartbeat();
    }
}
