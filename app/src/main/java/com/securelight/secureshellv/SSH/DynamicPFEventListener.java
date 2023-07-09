package com.securelight.secureshellv.SSH;

import android.util.Log;

import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;

public class DynamicPFEventListener implements PortForwardingEventListener {
    @Override
    public void establishingDynamicTunnel(Session session, SshdSocketAddress local) {
        Log.d(getClass().getName(), "establishing dynamic PF: " + session.getRemoteAddress()
                + "on" + local);
    }

    @Override
    public void establishedDynamicTunnel(Session session, SshdSocketAddress local,
                                         SshdSocketAddress boundAddress, Throwable reason) {
        Log.d(getClass().getName(), "established dynamic PF: " + session.getRemoteAddress() + " on "
                + boundAddress, reason);

    }

    @Override
    public void tearingDownDynamicTunnel(Session session, SshdSocketAddress address) {
        Log.d(getClass().getName(), "tearing down dynamic PF");
    }

    @Override
    public void tornDownDynamicTunnel(Session session, SshdSocketAddress address, Throwable reason) {
        Log.d(getClass().getName(), "tore down dynamic PF: " + address, reason);
    }
}
