package com.securelight.secureshellv.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InternetAccessHandler extends Thread {
    public final String TAG = getClass().getName();
    private AccessChangeListener accessChangeListener;
    private AccessType accessType;
    private int interval;
    private final ConnectionHandler connectionHandler;

    public enum AccessType {
        FULL_ACCESS,
        IRAN_ACCESS,
        NONE,
        VOID
    }

    interface AccessChangeListener {
        void onAccessTypeChanged(AccessType accessType);
    }

    public InternetAccessHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
        accessType = AccessType.VOID;
        interval = 1000;
    }

    @Override
    public void run() {
        int counterInitialValue = 4;
        int counter = counterInitialValue;

        accessType = checkAndGetAccessType();

        while (connectionHandler.isServiceActive()) {
            if (connectionHandler.getSshManager() != null) {
                AccessType tempType = checkAndGetAccessType();
                switch (tempType) {
                    case FULL_ACCESS:
                        connectionHandler.getLock().lock();
                        try {
                            connectionHandler.getInternetAccessCondition().signalAll();
                        } finally {
                            connectionHandler.getLock().unlock();
                        }
                        if (accessType != tempType) {
                            accessType = tempType;
                            accessChangeListener.onAccessTypeChanged(accessType);
                        }
                        break;
                    case IRAN_ACCESS:
                        if (accessType != tempType) {
                            accessType = tempType;
                            accessChangeListener.onAccessTypeChanged(accessType);
                        }
                        break;
                    case NONE:
                    case VOID:
                        if (counter == 0) {
                            if (accessType != tempType) {
                                accessType = tempType;
                                accessChangeListener.onAccessTypeChanged(accessType);
                            }
                            counter = counterInitialValue;
                        }
                        break;
                }
                counter--;
            }

            // todo: can possibly avoid busy wait?
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private AccessType checkAndGetAccessType() {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("google.com", 443), 2000);
            socket.close();
            return AccessType.FULL_ACCESS;
        } catch (IOException e) {
            if (e.getMessage().contains("ECONNREFUSED")) {
                return AccessType.VOID;
            }
            try {
                socket.connect(new InetSocketAddress("snapp.ir", 443), 2000);
                socket.close();
                return AccessType.IRAN_ACCESS;
            } catch (IOException ignored) {
                return AccessType.NONE;
            }
        }
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessChangeListener(AccessChangeListener accessChangeListener) {
        this.accessChangeListener = accessChangeListener;
    }
}