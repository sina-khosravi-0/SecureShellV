package com.securelight.secureshellv.backend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.tun2socks.Tun2SocksJni;
import com.securelight.secureshellv.vpnservice.StatsHandler;

import java.util.TimerTask;

public class SendTrafficTimeTask extends TimerTask {
    private final StatsHandler statsHandler;
    private final DatabaseHandlerSingleton databaseHandlerSingleton;
    private final Context context;
    private int counter = 0;

    public SendTrafficTimeTask(StatsHandler statsHandler,
                               DatabaseHandlerSingleton databaseHandlerSingleton,
                               Context context) {
        this.statsHandler = statsHandler;
        this.databaseHandlerSingleton = databaseHandlerSingleton;
        this.context = context.getApplicationContext();
    }

    @Override
    public void run() {
        sendIncrement();
    }

    public void sendIncrement() {
        boolean successful = databaseHandlerSingleton.sendTrafficIncrement(calcBytes());
        if (!successful) {
            counter ++;
            if (counter >= 3) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.SEND_STATS_FAIL_INTENT));
            }
        }
    }

    private long calcBytes() {
        long bytes = statsHandler.getBytesDownloaded() + statsHandler.getBytesUploaded();
        System.out.println(bytes);
        return bytes;
    }
}
