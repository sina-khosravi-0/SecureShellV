package com.securelight.secureshellv.vpnservice.connection;

import android.content.Context;

import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;

import java.util.TimerTask;

public class APIHeartbeatHandler extends TimerTask {
    private final DatabaseHandlerSingleton databaseHandler;

    public APIHeartbeatHandler(Context context) {
        databaseHandler = DatabaseHandlerSingleton.getInstance(context);
    }

    @Override
    public void run() {
        databaseHandler.sendHeartbeat();
    }
}
