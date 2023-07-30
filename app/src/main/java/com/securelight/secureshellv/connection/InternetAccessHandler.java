package com.securelight.secureshellv.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InternetAccessHandler extends Thread {

    public enum AccessType {
        FULL_ACCESS,
        IRAN_ACCESS,
        NONE,
        VOID
    }

    private static AccessType accessType;
    private int interval = 1000;
    private final ConnectionHandler connectionHandler;

    public InternetAccessHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void run() {
        // todo: can possibly avoid busy wait?
        while (connectionHandler.isServiceOn()) {
            if (connectionHandler.getSshManager() != null & isInternetAvailable() == AccessType.FULL_ACCESS) {
                synchronized (connectionHandler.getSshManager()) {
                    connectionHandler.getSshManager().notify();
                }
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private AccessType isInternetAvailable() {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("google.com", 443), 2000);
            socket.close();
            accessType = AccessType.FULL_ACCESS;
            return accessType;
        } catch (IOException e) {
            if (e.getMessage().contains("ECONNREFUSED")) {
                accessType = AccessType.VOID;
                return accessType;
            }
            try {
                socket.connect(new InetSocketAddress("snapp.ir", 443), 2000);
                socket.close();
                accessType = AccessType.IRAN_ACCESS;
            } catch (IOException ex) {
                accessType = AccessType.NONE;
            }
        }
        return accessType;
    }

    public AccessType getInternetAccess() {
        return accessType;
    }
}
