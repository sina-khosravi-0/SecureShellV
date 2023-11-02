package com.securelight.secureshellv.ssh;

import android.os.Build;
import android.util.Log;

import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.statics.Constants;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
import org.apache.sshd.common.SshException;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SshManager {
    private final String TAG = getClass().getSimpleName();
    private final List<PortForwardingTracker> portForwardingTrackers;
    private final SshConfigs configs;
    private SshClient sshClient;
    private ClientSession session;
    private ClientSession bridgeSession;
    private boolean established = false;
    ReentrantLock lock;
    Condition internetAvailableCondition;


    public SshManager(ReentrantLock lock, Condition internetAvailableCondition, SshConfigs configs) {
        this.lock = lock;
        this.internetAvailableCondition = internetAvailableCondition;
        this.configs = configs;
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

    public void connect(String password) {
        established = false;
        try {
            // wait for InternetAccessHandler or stop() to notify.
            try {
                lock.lock();
                internetAvailableCondition.await();
            } catch (InterruptedException e) {
                Log.d(TAG, "Current thread was Interrupted");
                return;
            } finally {
                lock.unlock();
            }

            if (sshClient.isStarted()){
                session = sshClient.connect(DataManager.getInstance().getUserName(),
                                DataManager.getInstance().getBestServer().getIp(),
                                configs.hostPort)
                        .verify(12, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT).getClientSession();
                session.addSessionListener(getSessionListener());
                session.addPortForwardingEventListener(new PFEventListener());
                session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE,
                        TimeUnit.SECONDS, 3);
                session.addPasswordIdentity(password);
                session.auth().verify(6000, CancelOption.CANCEL_ON_TIMEOUT);
                established = true;
            }
        } catch (SshException ignored) {
        } catch (IOException | IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            Log.e(TAG, "connection failed.", e);
        }
    }

    public void connectWithBridge() {
        established = false;
        try {
            // wait for InternetAccessHandler or stop() to notify.
            try {
                lock.lock();
                internetAvailableCondition.await();
            } catch (InterruptedException e) {
                Log.d(TAG, "Current thread was Interrupted");
                return;
            } finally {
                lock.unlock();
            }

            // opening session for iran server
            bridgeSession = sshClient.connect(DataManager.getInstance().getUserName(), configs.bridgeHostAddress, configs.bridgeHostPort)
                    .verify(12, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT)
                    .getClientSession();
            bridgeSession.addSessionListener(getSessionListener());
            bridgeSession.addPortForwardingEventListener(new PFEventListener());
            bridgeSession.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE
                    , TimeUnit.SECONDS, 3);
            bridgeSession.addPasswordIdentity(DataManager.getInstance().getSshPassword());
            bridgeSession.auth().verify(3000, CancelOption.CANCEL_ON_TIMEOUT);

            // create local port forwarding
            portForwardingTrackers.add(bridgeSession.createLocalPortForwardingTracker(
                    new SshdSocketAddress(configs.hostAddress, configs.hostPort),
                    new SshdSocketAddress(DataManager.getInstance().getBestServer().getLocal_ip(),
                            DataManager.getInstance().getBestServer().getLocal_port())));

            session = sshClient.connect(DataManager.getInstance().getUserName(), configs.hostAddress, configs.hostPort)
                    .verify(12, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT)
                    .getClientSession();
            session.addSessionListener(getSessionListener());
            session.addPortForwardingEventListener(new PFEventListener());
            session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE,
                    TimeUnit.SECONDS, 3);
            session.addPasswordIdentity(DataManager.getInstance().getSshPassword());
            session.auth().verify(3000, CancelOption.CANCEL_ON_TIMEOUT);

            established = true;
        } catch (IOException e) {
            Log.d(TAG, "connection failed.", e);
        }
    }

    public void createPortForwarding() {
        if (session == null || session.isClosed()) {
            // return if session is null or closed
            return;
        }
        try {
//            portForwardingTrackers.add(session.createLocalPortForwardingTracker(
//            new SshdSocketAddress(vpnSettings.getHost(), 10809),
//            new SshdSocketAddress("127.0.0.1", 3129)));
            portForwardingTrackers.add(session.createDynamicPortForwardingTracker(
                    new SshdSocketAddress(configs.socksAddress, configs.socksPort)));
        } catch (IOException e) {
            Log.e(TAG, "error during port forwarding");
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

    /**
     * closes the session (and the bridge session)
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
        if (configs.connectionMethod == Constants.Protocol.DUAL_SSH) {
            closed = false;
            if (bridgeSession != null) {
                do {
                    try {
                        bridgeSession.close();
                        closed = true;
                    } catch (IOException ignored) {
                        Log.d(TAG, "Session closing error. Retrying...");
                    }
                } while (!closed);
            }
        }
        portForwardingTrackers.clear();
    }

    /**
     * signals internet available condition to wake up.
     */
    public void clearLock() {
        lock.lock();
        try {
            internetAvailableCondition.signalAll();
        } finally {
            lock.unlock();
        }
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

    public boolean isEstablished() {
        return established;
    }

    public void setEstablished(boolean established) {
        this.established = established;
    }
}
