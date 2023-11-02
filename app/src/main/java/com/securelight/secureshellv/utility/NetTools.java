package com.securelight.secureshellv.utility;


import android.util.Log;

import com.securelight.secureshellv.backend.TargetServer;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.vpnservice.VpnSettings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

public class NetTools {
    public static boolean checkInternetAccess() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process mIpAdderProcess = runtime.exec("/system/bin/ping -c 5 google.com");
            int mExitValue = mIpAdderProcess.waitFor();

            return mExitValue == 0;

        } catch (InterruptedException | IOException e) {
            Log.d("Error", "Ping", e);
        }
        return false;
    }

    public static Constants.InternetQuality getVPNConnectionScore() {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                new InetSocketAddress(VpnSettings.iFaceAddress, VpnSettings.socksPort));

        long averageMilli = 0;
        int iterations = 5;

        Socket[] sockets = new Socket[iterations];
        for (int i = 0; i < iterations; i++) {
            sockets[i] = new Socket(proxy);
        }

        try {
            for (int i = 0; i < iterations; i++) {
                long prev = System.currentTimeMillis();
                sockets[i].connect(new InetSocketAddress("google.com", 443),
                        3000);
                averageMilli += System.currentTimeMillis() - prev;
            }
            averageMilli /= iterations;
        } catch (IOException ignored) {
        }

        for (int i = 0; i < iterations; i++) {
            try {
                sockets[i].close();
            } catch (IOException ignored) {
            }
        }

        if (averageMilli < 200) {
            return Constants.InternetQuality.EXCELLENT;
        }
        if (averageMilli < 500) {
            return Constants.InternetQuality.GOOD;
        }
        if (averageMilli < 1000) {
            return Constants.InternetQuality.MEDIUM;
        }
        if (averageMilli < 2000) {
            return Constants.InternetQuality.BAD;
        }
        return Constants.InternetQuality.HORRIBLE;
    }

    public static Constants.InternetQuality getInternetConnectionScore() {
        long averageMilli = 0;
        int iterations = 5;

        Socket[] sockets = new Socket[iterations];
        for (int i = 0; i < iterations; i++) {
            sockets[i] = new Socket();
        }

        try {
            for (int i = 0; i < iterations; i++) {
                long prev = System.currentTimeMillis();
                sockets[i].connect(new InetSocketAddress("google.com", 443),
                        3000);
                averageMilli += System.currentTimeMillis() - prev;
            }
            averageMilli /= iterations;
        } catch (IOException ignored) {
        }

        for (int i = 0; i < iterations; i++) {
            try {
                sockets[i].close();
            } catch (IOException ignored) {
            }
        }

        if (averageMilli < 120) {
            return Constants.InternetQuality.EXCELLENT;
        }
        if (averageMilli < 300) {
            return Constants.InternetQuality.GOOD;
        }
        if (averageMilli < 500) {
            return Constants.InternetQuality.MEDIUM;
        }
        if (averageMilli < 1000) {
            return Constants.InternetQuality.BAD;
        }
        return Constants.InternetQuality.HORRIBLE;
    }

    public static int getServerPing(TargetServer server) {
        long averageMilli = 0;
        int iterations = 5;

        Socket[] sockets = new Socket[iterations];
        for (int i = 0; i < iterations; i++) {
            sockets[i] = new Socket();
        }

        try {
            for (int i = 0; i < iterations; i++) {
                long prev = System.currentTimeMillis();
                sockets[i].connect(new InetSocketAddress(server.getIp(), server.getPort()),
                        3000);
                averageMilli += System.currentTimeMillis() - prev;
                Thread.sleep(300);
            }
            averageMilli /= iterations;
        } catch (IOException | InterruptedException ignored) {
        }

        for (int i = 0; i < iterations; i++) {
            try {
                sockets[i].close();
            } catch (IOException ignored) {
            }
        }
        return (int) averageMilli;
    }
}