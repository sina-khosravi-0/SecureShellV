package com.securelight.secureshellv;

import android.content.pm.PackageManager;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.PortForwardingTracker;

import java.io.IOException;
import java.util.Set;


public class SockConnection implements Runnable {

    public final String TAG = this.getClass().getName();

    private final SSVpnService mService;
    private final VPNSettings mConfig;
    private final Set<String> mPackages;


//    todo:private static final int MAX_PACKET_SIZE = Short.MAX_VALUE;
//    private static final long IDLE_INTERVAL_MS = TimeUnit.MILLISECONDS.toMillis(100);


    @Override
    public void run() {
        try {
            connect();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
    SshClient sshClient;

    ClientSession session;

    PortForwardingTracker portForwardingTracker;
    private void connect() throws PackageManager.NameNotFoundException {


//        SSHDynamicPortForwardingHandler sshDPFHandler = new SSHDynamicPortForwardingHandler(mConfig);
//        if (sshDPFHandler.startPortForwarding()) {
//            sshClient = sshDPFHandler.getSshClient();
//            session = sshDPFHandler.getSession();
//            portForwardingTracker = sshDPFHandler.getPortForwardingTracker();
//        }
/*      this is todo:engine.Key key = new engine.Key();
        key.setMark(0);
        key.setMTU(0);
        key.setDevice("fd://" + iFace.getFd());
        key.setInterface("");
        //[debug|info|warning|error|silent]
        key.setLogLevel("error");
        key.setProxy("socks5://" + mConfig.getHost() + ":" + mConfig.getPort());
        key.setRestAPI("");
        key.setTCPSendBufferSize("");
        key.setTCPReceiveBufferSize("");
        key.setTCPModerateReceiveBuffer(false);
        engine.Engine.insert(key);
        executors.submit(Engine::start);*/
    }





    public void disconnect() throws IOException {
        iFace.close();
    }

    public SockConnection(final SSVpnService service,
                          final VPNSettings config,
                          final Set<String> packages) {
        mService = service;
        mConfig = config;
        mPackages = packages;
    }
}