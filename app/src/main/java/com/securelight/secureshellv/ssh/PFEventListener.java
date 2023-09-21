package com.securelight.secureshellv.ssh;

import android.util.Log;

import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;

/**
 * Currently only for debugging
 * */
public class PFEventListener implements PortForwardingEventListener {
    private final String TAG = getClass().getSimpleName();

    @Override
    public void establishedDynamicTunnel(Session session, SshdSocketAddress local,
                                         SshdSocketAddress boundAddress, Throwable reason) {
        Log.d(TAG, "Established dynamic PF: " + boundAddress, reason);
    }

    @Override
    public void tornDownDynamicTunnel(Session session, SshdSocketAddress address, Throwable reason) {
        Log.d(TAG, "Tore down dynamic PF: " + address, reason);
    }

    @Override
    public void establishedExplicitTunnel(Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding, SshdSocketAddress boundAddress, Throwable reason) {
        Log.d(TAG, "Established explicit PF: " + local + " <- "
                + remote, reason);
    }
}
