package com.securelight.secureshellv.SSH;

import android.util.Log;

import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.common.session.SessionListenerManager;
import org.apache.sshd.common.util.SshdEventListener;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;

public class DynamicPFEventListener implements PortForwardingEventListener {
    private String TAG = getClass().getName();

    @Override
    public void establishingDynamicTunnel(Session session, SshdSocketAddress local) {
        Log.d(TAG, "establishing dynamic PF: " + session.getRemoteAddress()
                + " on " + local);
    }

    @Override
    public void establishedDynamicTunnel(Session session, SshdSocketAddress local,
                                         SshdSocketAddress boundAddress, Throwable reason) {
        Log.i(TAG, "established dynamic PF: " + session.getRemoteAddress() + " on "
                + boundAddress, reason);

    }

    @Override
    public void tearingDownDynamicTunnel(Session session, SshdSocketAddress address) {
        Log.d(TAG, "tearing down dynamic PF: " + address);
    }

    @Override
    public void tornDownDynamicTunnel(Session session, SshdSocketAddress address, Throwable reason) {
        Log.i(TAG, "Tore down dynamic PF: " + address, reason);
    }

}
