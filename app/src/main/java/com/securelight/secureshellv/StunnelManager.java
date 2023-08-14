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

    public void open() {
        
        try {
            stunnel = stunnelBuilder.addService()
                    .client()
                    .acceptLocal(2000)
                    .connect("one.weary.tech", 80)
                    .sslVersion(SSLVersion.TLSv1_3)
                    .apply()
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void close() {
        try {
            stunnel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
