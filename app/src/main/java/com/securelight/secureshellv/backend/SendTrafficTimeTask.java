package com.securelight.secureshellv.backend;

import android.content.Context;

import com.securelight.secureshellv.tun2socks.Tun2SocksJni;
import com.securelight.secureshellv.vpnservice.StatsHandler;

import java.util.TimerTask;

public class SendTrafficTimeTask extends TimerTask {
    private final StatsHandler statsHandler;
    DatabaseHandlerSingleton databaseHandlerSingleton;

    public SendTrafficTimeTask(StatsHandler statsHandler,
                               DatabaseHandlerSingleton databaseHandlerSingleton) {
        this.statsHandler = statsHandler;
        this.databaseHandlerSingleton = databaseHandlerSingleton;
    }

    @Override
    public void run() {
        sendIncrement();
    }

    public void sendIncrement() {
        databaseHandlerSingleton.sendTrafficIncrement(calcBytes());
    }

    private long calcBytes() {
        return statsHandler.getBytesDownloaded() + statsHandler.getBytesUploaded();
    }
}
