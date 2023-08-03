package com.securelight.secureshellv.ssh;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.securelight.secureshellv.connection.ConnectionHandler;

import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;

public class PFEventListener implements PortForwardingEventListener {
    private String TAG = getClass().getName();
    private ConnectionHandler connectionHandler;

    Handler handler;

    public PFEventListener(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
        handler = new Handler(connectionHandler.getApplication().getMainLooper());
    }

    @Override
    public void establishedDynamicTunnel(Session session, SshdSocketAddress local,
                                         SshdSocketAddress boundAddress, Throwable reason) {
        Log.d(TAG, "Established dynamic PF: " + boundAddress, reason);
        handler.post(() -> Toast.makeText(connectionHandler.getApplication(), "Connected", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void tornDownDynamicTunnel(Session session, SshdSocketAddress address, Throwable reason) {
        Log.d(TAG, "Tore down dynamic PF: " + address, reason);
        handler.post(() -> Toast.makeText(connectionHandler.getApplication(), "Disconnected", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void establishedExplicitTunnel(Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding, SshdSocketAddress boundAddress, Throwable reason) {
        Log.d(TAG, "Established explicit PF: " + local + " <- "
                + remote, reason);
    }
}
