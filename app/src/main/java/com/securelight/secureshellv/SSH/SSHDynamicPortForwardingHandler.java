package com.securelight.secureshellv.SSH;

import android.os.Build;
import android.util.Log;

import com.securelight.secureshellv.database.ClientData;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
import org.apache.sshd.common.future.CancelOption;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SSHDynamicPortForwardingHandler {
    private SshClient sshClient;
    private ClientSession session;
    private PortForwardingTracker portForwardingTracker;
    private final com.securelight.secureshellv.VPNSettings VPNSettings;

    public SSHDynamicPortForwardingHandler(com.securelight.secureshellv.VPNSettings VPNSettings) {
        this.VPNSettings = VPNSettings;
        init();
    }

    private void init() {
        System.setProperty("user.home", Build.MODEL);
        Log.d(getClass().getName(), "Set user.home to '" + Build.MODEL + "'");
        sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        Log.d(getClass().getName(), "SshClient started: " + sshClient.toString());
    }

    public boolean startPortForwarding() {
        Thread thread = new Thread(() -> {
            try {
                session = sshClient.connect(ClientData.getUserName(), getBestServer(), 22)
                        .verify(5, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT).getSession();
                DynamicPFEventListener pfEventListener = new DynamicPFEventListener();
                session.addPortForwardingEventListener(pfEventListener);
                session.addPasswordIdentity(String.valueOf(ClientData.getSshPassword()));
                session.auth().verify(5, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT);
                portForwardingTracker = session.createDynamicPortForwardingTracker(
                        new SshdSocketAddress(VPNSettings.getHost(), VPNSettings.getPort()));

            } catch (IOException e) {
                Log.e(getClass().getName(), "Error during port forwarding", e);
            }
        });
        thread.start();
//        try {
//            thread.join();
//        } catch (InterruptedException e) {
//            Log.e(getClass().getName(), e.getMessage(), e);
//        }
        return true;
    }

    public String getBestServer() {
        //todo: calculate best server (possibly based on selected options)
        List<String> serversAddresses = ClientData.getServerAddresses();
        return "test.weary.tech";
    }

    public SshClient getSshClient() {
        return sshClient;
    }

    public ClientSession getSession() {
        return session;
    }

    public PortForwardingTracker getPortForwardingTracker() {
        return portForwardingTracker;
    }

}
