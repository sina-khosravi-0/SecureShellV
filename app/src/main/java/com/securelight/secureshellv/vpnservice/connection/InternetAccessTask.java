package com.securelight.secureshellv.vpnservice.connection;

import static com.securelight.secureshellv.utility.Utilities.checkAndGetAccessType;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class InternetAccessTask extends TimerTask {
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

    public InternetAccessTask(ReentrantLock lock, Condition internetAvailableCondition) {

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