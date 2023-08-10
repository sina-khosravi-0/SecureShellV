package com.securelight.secureshellv.connection;

import com.securelight.secureshellv.Values;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InternetAccessHandler extends Thread {
    private final String TAG = InternetAccessHandler.class.getSimpleName();
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
        interval = 1000;
    }

    @Override
    public void run() {
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
                            connectionHandler.networkStateString = Values.FULL_INTERNET_ACCESS_STRING;
                            accessChangeListener.onAccessTypeChanged(accessType);
                        }
                        break;
                    case IRAN_ACCESS:
                        if (accessType != tempType) {
                            accessType = tempType;
                            connectionHandler.networkStateString = Values.RESTRICTED_INTERNET_ACCESS_STRING;
                            accessChangeListener.onAccessTypeChanged(accessType);
                        }
                        break;
                    case NONE:
                        if (accessType != tempType) {
                            accessType = tempType;
                            connectionHandler.networkStateString = Values.NO_INTERNET_ACCESS_STRING;
                            accessChangeListener.onAccessTypeChanged(accessType);
                        }
                        break;
                    case VOID:
                        if (accessType != tempType) {
                            accessType = tempType;
                            connectionHandler.networkStateString = Values.NETWORK_UNAVAILABLE_STRING;
                            accessChangeListener.onAccessTypeChanged(accessType);
                        }
                        break;
                }
            }

            // todo: can possibly avoid busy wait?
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private AccessType checkAndGetAccessType() {
        if (!connectionHandler.isNetworkIFaceAvailable()) {
            return AccessType.VOID;
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("google.com", 443), 6000);
            socket.close();
            return AccessType.FULL_ACCESS;
        } catch (IOException e) {
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