package com.securelight.secureshellv.ssh;

import android.os.Build;
import android.util.Log;

import com.securelight.secureshellv.VpnSettings;
import com.securelight.secureshellv.connection.ConnectionHandler;
import com.securelight.secureshellv.database.ClientData;

import org.apache.sshd.client.ClientBuilder;
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
    private final String TAG = getClass().getSimpleName();
    private final ConnectionHandler connectionHandler;
    private SshClient sshClient;
    private ClientBuilder clientBuilder;
    private ClientSession session;
    private ClientSession sessionIran;
    private final List<PortForwardingTracker> portForwardingTrackers;

    private final VpnSettings vpnSettings;

    public SSHManager(VpnSettings vpnSettings, ConnectionHandler connectionHandler) {
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
        CoreModuleProperties.IDLE_TIMEOUT.set(sshClient, Duration.ofMillis(5000));
    }

    public void setupConnection() {
        boolean successful = false;
        while (!successful && connectionHandler.isServiceActive()) {
            try {
                // wait for InternetAccessHandler to notify on internet access
                // or for stop method to notify.
                try {
                    connectionHandler.getLock().lock();
                    connectionHandler.getInternetAvailableCondition().await();
                    if (!connectionHandler.isServiceActive()) {
                        // return if after being notified, service is off to avoid connection after
                        // ConnectionHandler termination.
                        return;
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Current thread was Interrupted");
                    // return if interrupt was called, while waiting.
                    return;
                } finally {
                    connectionHandler.getLock().unlock();
                }
                // todo: do something about this mess
                // opening session for iran server
                /* for some reason verify method forces timeout after about 12 seconds even
                 * when no timeout is specified */
//                sessionIran = sshClient.connect(ClientData.getUserName(), "one.weary.tech", 22)
//                        .verify(12, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT)
//                        .getClientSession();
//                sessionIran.addSessionListener(getSessionListener());
//                sessionIran.addPortForwardingEventListener(new PFEventListener(connectionHandler));
//                sessionIran.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE
//                        , TimeUnit.SECONDS, 3);
//                sessionIran.addPasswordIdentity(String.valueOf(ClientData.getSshPassword()));
//                sessionIran.auth().verify(3000, CancelOption.CANCEL_ON_TIMEOUT);
//                 open port 2000
//                startIranPortForwarding();

                // note: iran port forwarding forwards local port 20317 to one.weary.tech port 2000
                // to 64.226.64.126 port 22
                session = sshClient.connect(ClientData.getUserName(), getSshAddress(), 22)
                        .verify(12, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT)
                        .getClientSession();
                session.addSessionListener(getSessionListener());
                session.addPortForwardingEventListener(new PFEventListener(connectionHandler));
                session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE,
                        TimeUnit.SECONDS, 3);
                session.addPasswordIdentity(String.valueOf(ClientData.getSshPassword()));
                session.auth().verify(3000, CancelOption.CANCEL_ON_TIMEOUT);

                successful = true;
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
                Log.e(TAG, "connection failed.");
            }
        } // while (!successful && connectionHandler.isRunning())
    }

    public void startPortForwarding() {
        if (session == null || session.isClosed() || !connectionHandler.isServiceActive()) {
            // return if session is null or closed or service is not active
            return;
        }
        try {
//            portForwardingTrackers.add(session.createLocalPortForwardingTracker(
//            new SshdSocketAddress(vpnSettings.getHost(), 10809),
//            new SshdSocketAddress("127.0.0.1", 3129)));
            portForwardingTrackers.add(session.createDynamicPortForwardingTracker(
                    new SshdSocketAddress(vpnSettings.getHost(), 10808)));
        } catch (IOException e) {
            Log.e(TAG, "error during port forwarding");
        }
    }

    public void startIranPortForwarding() throws IOException {
        portForwardingTrackers.add(sessionIran.createLocalPortForwardingTracker(
                new SshdSocketAddress("127.0.0.1", 20317),
                new SshdSocketAddress("127.0.0.1", 2000)));
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

    /**
     * closes clientSession and signals setup connection to wake up.
     * it will keep signaling until connection thread is dead to avoid
     * dead-locking in setup connection.
     */
    public void close() {
        boolean closed = false;
        if (session != null) {
            do {
                try {
                    session.close();
                    closed = true;
                } catch (IOException ignored) {
                    Log.d(TAG, "Session closing error. Retrying...");
                }
            } while (!closed);
        }
        portForwardingTrackers.clear();
    }

    public void closeAndFinalize() {
        close();
        sshClient.stop();

        do {
            connectionHandler.getLock().lock();
            try {
                connectionHandler.getInternetAvailableCondition().signalAll();
            } finally {
                connectionHandler.getLock().unlock();
            }
            // don't stop signalling until we are sure we aren't stuck at:
            // internetAvailableCondition().await()
        } while (connectionHandler.isRunning());
    }

    public void cancelAvailabilityLock() {

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
