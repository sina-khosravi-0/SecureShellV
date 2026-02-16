package com.securelight.secureshellv.vpnservice;

import com.securelight.secureshellv.vpnservice.v2ray.V2rayCoreManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;

public class StatsHandler {
    private final V2rayCoreManager v2rayCoreManager;
    ScheduledExecutorService scheduler;
    private long bytesDownloaded = 0;
    private long bytesUploaded = 0;
    private boolean running = false;

    public StatsHandler(V2rayCoreManager v2rayCoreExecutor) {
        this.v2rayCoreManager = v2rayCoreExecutor;
    }

    public void start() {
        if (running) {
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(() -> {
                    bytesDownloaded += v2rayCoreManager.getDownloadSpeed();
                    bytesUploaded += v2rayCoreManager.getUploadSpeed();
                },
                0,
                100,
                TimeUnit.MILLISECONDS);
        running = true;
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        scheduler.shutdown();
        resetStats();
    }

    public long getBytesDownloaded() {
        try {
            return bytesDownloaded;
        } finally {
            bytesDownloaded = 0;
        }
    }

    public long getBytesUploaded() {

        try {
            return bytesUploaded;
        } finally {
            bytesUploaded = 0;
        }
    }

    public void resetStats() {
        bytesDownloaded = 0;
        bytesUploaded = 0;
    }
}
