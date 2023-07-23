package com.securelight.secureshellv.ssh;

import android.os.Build;
import android.util.Log;

import com.securelight.secureshellv.ConnectionHandler;
import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.database.ClientData;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.future.CancelOption;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SSHConnectionManager {
    private final String TAG = getClass().getName();
    private final ConnectionHandler connectionHandler;
    private SshClient sshClient;
    private ClientSession session;
    private List<PortForwardingTracker> portForwardingTrackers;
    private final VpnSettings vpnSettings;

    public SSHConnectionManager(VpnSettings vpnSettings, ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
        this.vpnSettings = vpnSettings;
        portForwardingTrackers = new ArrayList<>();
        init();
    }

    private void init() {
        System.setProperty("user.home", Build.MODEL);
        Log.d(TAG, "Set user.home to '" + Build.MODEL + "'");
        sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
    }

    public boolean setupConnection() {
        try {
            session = sshClient.connect(ClientData.getUserName(), getSshAddress(), 22)
                    .verify().getSession();
            session.addSessionListener(getSessionListener());
            session.addPortForwardingEventListener(new PFEventListener(connectionHandler));
            session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, TimeUnit.MILLISECONDS, 500);
            session.addPasswordIdentity(String.valueOf(ClientData.getSshPassword()));
            session.auth().verify(1500, CancelOption.CANCEL_ON_TIMEOUT);
            return true;
        } catch (SshException e) {
            Log.d(TAG, e.getMessage());
            Log.e(TAG, "Connection failed.");
            setupConnection();
        } catch (IOException e) {
            Log.d(TAG, "Connection setup error", e);
        }
        return false;
    }

    private SessionListener getSessionListener() {
        return new SessionListener() {
            @Override
            public void sessionClosed(Session session) {
                getPortForwardingTrackers().forEach(portForwardingTracker -> {
                    try {
                        portForwardingTracker.close();
                        Log.v(TAG, portForwardingTracker + " closed.");
                    } catch (IOException e) {
                        Log.e(TAG, "PF closing error");
                    }
                });
            }

            @Override
            public void sessionEvent(Session session, Event event) {
                if (event == Event.Authenticated) {
                    Log.d(TAG, "Session authed: " + session.getRemoteAddress());
                }
            }
        };
    }

    public void startPortForwarding() {
        try {
            portForwardingTrackers.add(session.createLocalPortForwardingTracker(
                    new SshdSocketAddress(vpnSettings.getHost(), 10809),
                    new SshdSocketAddress("127.0.0.1", 3129)));
            portForwardingTrackers.add(session.createDynamicPortForwardingTracker(
                    new SshdSocketAddress(vpnSettings.getHost(), vpnSettings.getPort())));
        } catch (IOException e) {
            Log.e(TAG, "Error during port forwarding");
        }
    }

    public void close() throws IOException {
        if (session.isOpen()) {
            session.close();
        }
//        session = null;
        portForwardingTrackers.clear();
    }

    public String getSshAddress() {
        //todo: calculate best server (possibly based on selected options e.g location, ISP)
        List<String> serversAddresses = ClientData.getServerAddresses();
        return "64.226.64.126";
    }

    public SshClient getSshClient() {
        return sshClient;
    }

    public ClientSession getSession() {
        return session;
    }

    public List<PortForwardingTracker> getPortForwardingTrackers() {
        return portForwardingTrackers;
    }
}
