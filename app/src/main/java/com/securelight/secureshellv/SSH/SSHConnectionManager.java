package com.securelight.secureshellv.SSH;

import android.os.Build;
import android.util.Log;

import com.securelight.secureshellv.VPNSettings;
import com.securelight.secureshellv.database.ClientData;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
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
    private SshClient sshClient;
    private ClientSession session;
    private List<PortForwardingTracker> portForwardingTrackers;
    private final com.securelight.secureshellv.VPNSettings VPNSettings;

    public SSHConnectionManager(VPNSettings VPNSettings) {
        this.VPNSettings = VPNSettings;
        portForwardingTrackers = new ArrayList<>();
        init();
    }

    private void init() {
        System.setProperty("user.home", Build.MODEL);
        Log.d(TAG, "Set user.home to '" + Build.MODEL + "'");
        sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        Log.d(TAG, "SshClient started: " + sshClient.toString());
    }




    public void startPortForwarding() {
        Thread thread = new Thread(() -> {
            try {
                session = sshClient.connect(ClientData.getUserName(), getBestServer(), 22)
                        .verify(5, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT).getSession();
                session.addSessionListener(new SessionListener() {
                    @Override
                    public void sessionClosed(Session session) {
                        portForwardingTrackers.forEach(portForwardingTracker -> {
                            try {
                                portForwardingTracker.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Dynamic PF, closing error", e);
                            }
                        });
                    }
                });
                DynamicPFEventListener pfEventListener = new DynamicPFEventListener();
                session.addPortForwardingEventListener(pfEventListener);
                session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, TimeUnit.SECONDS, 2);
                session.addPasswordIdentity(String.valueOf(ClientData.getSshPassword()));
                session.auth().verify(5, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT);
                portForwardingTrackers.add(session.createDynamicPortForwardingTracker(
                        new SshdSocketAddress(VPNSettings.getHost(), VPNSettings.getPort())));
            } catch (IOException e) {
                Log.e(TAG, "Error during port forwarding", e);
            } catch (UnresolvedAddressException unresolvedAddressException) {
                Log.e(TAG, unresolvedAddressException.getMessage());
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public String getBestServer() {
        //todo: calculate best server (possibly based on selected options e.g location, ISP)
        List<String> serversAddresses = ClientData.getServerAddresses();
        return "test.weary.tech";
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
