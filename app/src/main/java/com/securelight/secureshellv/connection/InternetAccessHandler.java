package com.securelight.secureshellv.connection;

import com.securelight.secureshellv.Values;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class InternetAccessHandler extends TimerTask {
    private final String TAG = getClass().getName();
    private AccessChangeListener accessChangeListener;
    private final ConnectionHandler connectionHandler;
    private AccessType accessType;
    private final ReentrantLock lock;
    private final Condition iFaceAvailableCondition;
    private final AtomicBoolean awake = new AtomicBoolean(true);

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
        lock = new ReentrantLock();
        iFaceAvailableCondition = lock.newCondition();
    }

    @Override
    public void run() {
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
    }

    private AccessType checkAndGetAccessType() {
        if (!connectionHandler.isNetworkIFaceAvailable()) {
            return AccessType.VOID;
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("google.com", 443), 2500);
            socket.close();
            return AccessType.FULL_ACCESS;
        } catch (IOException e) {
            try {
                socket.connect(new InetSocketAddress("snapp.ir", 443), 1500);
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
                iFaceAvailableCondition.signal();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                lock.unlock();
            }
        });
        wakeupThread.start();
    }
}