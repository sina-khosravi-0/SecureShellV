package com.securelight.secureshellv.connection;

import com.securelight.secureshellv.Values;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class InternetAccessHandler extends Thread {
    private final String TAG = InternetAccessHandler.class.getSimpleName();
    private AccessChangeListener accessChangeListener;
    private final ConnectionHandler connectionHandler;
    private AccessType accessType;
    private final ReentrantLock lock;
    private final Condition networkAvailableCondition;
    private final AtomicBoolean awake = new AtomicBoolean(true);
    private int interval;

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
        setName("InternetAccessHandler");
        this.connectionHandler = connectionHandler;
        lock = new ReentrantLock();
        networkAvailableCondition = lock.newCondition();
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
                            connectionHandler.getInternetAvailableCondition().signalAll();
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
//                if (Instant.now().getEpochSecond() - connectionHandler.getSshManager()
//                        .getSession().getIdleTimeoutStart().getEpochSecond() >= 6) {
//
//                    System.out.println("MOTHER");
//
//                }
            }

            // todo: can possibly avoid busy wait?
            try {
                sleep(interval);
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
            socket.connect(new InetSocketAddress("google.com", 443), 4000);
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

    public void wakeup() {
        Thread wakeupThread = new Thread(() -> {
            while (!awake.get()) {
                lock.lock();
                networkAvailableCondition.signal();
                try {
                    sleep(500);
                } catch (InterruptedException ignored) {
                }
                lock.unlock();
            }
        });
        wakeupThread.start();
    }
}