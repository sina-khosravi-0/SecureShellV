package com.securelight.secureshellv.backend;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.vpnservice.StatsHandler;

import java.util.TimerTask;

public class SendTrafficTimeTask extends TimerTask {
    private final StatsHandler statsHandler;
    private final BackendHandlerSingleton backendHandlerSingleton;
    private final Context context;
    private int counter = 0;

    public SendTrafficTimeTask(StatsHandler statsHandler,
                               BackendHandlerSingleton backendHandlerSingleton,
                               Context context) {
        this.statsHandler = statsHandler;
        this.backendHandlerSingleton = backendHandlerSingleton;
        this.context = context.getApplicationContext();
    }

    @Override
    public void run() {
        sendIncrement();
    }

    public void sendIncrement() {
        boolean successful = backendHandlerSingleton.sendTrafficIncrement(calcBytes());
        if (!successful) {
            counter ++;
            if (counter >= 3) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.SEND_STATS_FAIL_INTENT));
            } else {
                counter = 0;
            }
        }
    }

    private long calcBytes() {
        long bytes = statsHandler.getBytesDownloaded() + statsHandler.getBytesUploaded();
        System.out.println("bytes = " + bytes);
        return bytes;
    }
}
