package com.securelight.secureshellv.vpnservice.connection;

import com.securelight.secureshellv.statics.Constants;

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
    private NetworkState networkState;
    private final ReentrantLock lock;
    private final Condition internetAvailableCondition;
    private final ReentrantLock thisLock;
    private final Condition iFaceAvailableCondition;
    private final AtomicBoolean networkIFaceAvailable = new AtomicBoolean(true);
    private final AtomicBoolean awake = new AtomicBoolean(true);

    interface AccessChangeListener {
        void onNetworkStateChanged(NetworkState networkState);
    }

    public InternetAccessHandler(ReentrantLock lock, Condition internetAvailableCondition) {
        thisLock = new ReentrantLock();
        iFaceAvailableCondition = thisLock.newCondition();
        this.lock = lock;
        this.internetAvailableCondition = internetAvailableCondition;
    }

    @Override
    public void run() {
        NetworkState tempType = checkAndGetAccessType();
        switch (tempType) {
            case RESTRICTED:
            case WORLD_WIDE:
                lock.lock();
                try {
                    internetAvailableCondition.signalAll();
                } finally {
                    lock.unlock();
                }
                if (networkState != tempType) {
                    networkState = tempType;
                    accessChangeListener.onNetworkStateChanged(networkState);
                }
                break;
            case NO_ACCESS:
            case UNAVAILABLE:
                if (networkState != tempType) {
                    networkState = tempType;
                    accessChangeListener.onNetworkStateChanged(networkState);
                }
                break;
        }
    }

    private NetworkState checkAndGetAccessType() {
        if (!networkIFaceAvailable.get()) {
            return NetworkState.UNAVAILABLE;
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("google.com", 443), 2500);
            socket.close();
            return NetworkState.WORLD_WIDE;
        } catch (IOException e) {
            try {
                socket.connect(new InetSocketAddress("snapp.ir", 443), 1500);
                socket.close();
                return NetworkState.RESTRICTED;
            } catch (IOException ignored) {
                return NetworkState.NO_ACCESS;
            }
        }
    }

    public NetworkState getNetworkState() {
        return networkState;
    }

    public void setAccessChangeListener(AccessChangeListener listener) {
        this.accessChangeListener = listener;
    }

    public void setNetworkIFaceAvailable(boolean available) {
        networkIFaceAvailable.set(available);
    }

    public void wakeup() {
        Thread wakeupThread = new Thread(() -> {
            while (!awake.get()) {
                thisLock.lock();
                iFaceAvailableCondition.signal();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                thisLock.unlock();
            }
        });
        wakeupThread.start();
    }
}