package com.securelight.secureshellv.backend;

import android.content.Context;

import com.securelight.secureshellv.tun2socks.Tun2SocksJni;

import java.util.TimerTask;

public class SendTrafficTimeTask extends TimerTask {
    private final Context context;

    public SendTrafficTimeTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void run() {
        // TODO: Create method in the handler
        DatabaseHandlerSingleton.getInstance(context).sendTrafficIncrement(calcBytes());
    }

    public long calcBytes() {
        try {
            return Tun2SocksJni.getRxBytes() + Tun2SocksJni.getTxBytes() + Tun2SocksJni.getUDPBytes();
        } finally {
            Tun2SocksJni.resetBytes();
        }
    }
}
