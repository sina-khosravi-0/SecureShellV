package com.securelight.secureshellv;

import android.content.Context;

import java.io.IOException;

import de.fwinkel.android_stunnel.SSLVersion;
import de.fwinkel.android_stunnel.Stunnel;
import de.fwinkel.android_stunnel.StunnelBuilder;

public class StunnelManager {
    StunnelBuilder stunnelBuilder;
    Stunnel stunnel;

    public StunnelManager(Context context) {
        stunnelBuilder = new StunnelBuilder(context);
    }

    public void open(String hostAddress, int hostPort, int localPort) {
        
        try {
            stunnel = stunnelBuilder.addService()
                    .client()
                    .acceptLocal(localPort)
                    .connect(hostAddress, hostPort)
                    .sslVersion(SSLVersion.TLSv1_3)
                    .apply()
                    .start();
        } catch (IOException e) {
            throw new RuntimeException("couldn't start stunnel");
        }
    }

    public void close() {
        try {
            stunnel.close();
        } catch (IOException e) {
            throw new RuntimeException("couldn't stop stunnel");
        }
    }
}
