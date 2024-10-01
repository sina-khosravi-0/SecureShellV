package com.securelight.secureshellv.vpnservice;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.dev7.lib.v2ray.core.V2rayCoreExecutor;

public class StatsHandler {
    private final V2rayCoreExecutor v2rayCoreExecutor;
    ScheduledExecutorService scheduler;
    private long bytesDownloaded = 0;
    private long bytesUploaded = 0;
    private boolean running = false;

    public StatsHandler(V2rayCoreExecutor v2rayCoreExecutor) {
        this.v2rayCoreExecutor = v2rayCoreExecutor;
    }

    public void start() {
        if (running) {
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(() -> {
                    bytesDownloaded += v2rayCoreExecutor.getDownloadSpeed();
                    bytesUploaded += v2rayCoreExecutor.getUploadSpeed();
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
