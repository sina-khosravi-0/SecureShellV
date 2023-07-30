package com.securelight.secureshellv.ssh;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import com.securelight.secureshellv.connection.ConnectionHandler;
import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.database.ClientData;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
import org.apache.sshd.common.future.CancelOption;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.core.CoreModuleProperties;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SSHManager {
    private final String TAG = getClass().getName();
    private final ConnectionHandler connectionHandler;
    private SshClient sshClient;
    private ClientSession session;
    private final List<PortForwardingTracker> portForwardingTrackers;
    private final VpnSettings vpnSettings;

    public SSHManager(VpnSettings vpnSettings, ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
        this.vpnSettings = vpnSettings;
        portForwardingTrackers = new ArrayList<>();
        init();
    }

    @SuppressLint("NewApi")
    private void init() {
        System.setProperty("user.home", Build.MODEL);
        Log.d(TAG, "Set user.home to '" + Build.MODEL + "'");
        sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        CoreModuleProperties.IDLE_TIMEOUT.set(sshClient, Duration.ofSeconds(2));
    }

    public void setupConnection() {
        try {
            // wait for InternetAccessHandler to notify
            synchronized (this) {
                wait();
            }
            /* for some reason verify method forces timeout after about 12 seconds even
            * when no timeout is specified */
            session = sshClient.connect(ClientData.getUserName(), getSshAddress(), 22)
                    .verify(12, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT).getClientSession();
            session.addSessionListener(getSessionListener());
            session.addPortForwardingEventListener(new PFEventListener(connectionHandler));
//            session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, TimeUnit.MILLISECONDS, 500);
            session.addPasswordIdentity(String.valueOf(ClientData.getSshPassword()));
            session.auth().verify(3000, CancelOption.CANCEL_ON_TIMEOUT);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            Log.e(TAG, "Connection failed.");
            // don't let method return before successfully connecting
            setupConnection();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected Error occurred.");
        }
    }

    private SessionListener getSessionListener() {
        return new SessionListener() {
            @Override
            public void sessionClosed(Session session) {
                Log.d(TAG, "Session closed: " + session);
                getPortForwardingTrackers().forEach(portForwardingTracker -> {
                    Log.v(TAG, portForwardingTracker + " closing.");
                    try {
                        portForwardingTracker.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                portForwardingTrackers.clear();
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
        session.close();
//        session = null;
        portForwardingTrackers.clear();
    }

    public String getSshAddress() {
        //todo: calculate best server (possibly based on selected options e.g location, ISP)
        List<String> serverAddresses = ClientData.getServerAddresses();
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
