package com.securelight.secureshellv.utility;


import android.util.Log;

import com.securelight.secureshellv.backend.TargetServer;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.vpnservice.VpnSettings;
import com.securelight.secureshellv.vpnservice.connection.NetworkState;
import com.securelight.secureshellv.vpnservice.listeners.SocketProtector;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;

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
        int attempts = 5;

        Socket[] sockets = new Socket[attempts];
        for (int i = 0; i < attempts; i++) {
            sockets[i] = new Socket(proxy);
        }

        try {
            for (int i = 0; i < attempts; i++) {
                long prev = System.currentTimeMillis();
                sockets[i].connect(new InetSocketAddress("google.com", 443),
                        3000);
                averageMilli += System.currentTimeMillis() - prev;
            }
            averageMilli /= attempts;
        } catch (IOException ignored) {
        }

        for (int i = 0; i < attempts; i++) {
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
        int attempts = 5;

        Socket[] sockets = new Socket[attempts];
        for (int i = 0; i < attempts; i++) {
            sockets[i] = new Socket();
        }

        try {
            for (int i = 0; i < attempts; i++) {
                long prev = System.currentTimeMillis();
                sockets[i].connect(new InetSocketAddress("google.com", 443),
                        3000);
                averageMilli += System.currentTimeMillis() - prev;
            }
            averageMilli /= attempts;
        } catch (IOException ignored) {
        }

        for (int i = 0; i < attempts; i++) {
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
        int attempts = 5;

        Socket[] sockets = new Socket[attempts];
        for (int i = 0; i < attempts; i++) {
            sockets[i] = new Socket();
        }

        try {
            for (int i = 0; i < attempts; i++) {
                long prev = System.currentTimeMillis();

                try {
                    sockets[i].connect(new InetSocketAddress(server.getIp(), server.getPingPort()),
                            3000);
                } catch (SocketTimeoutException ignored) {
                }

                averageMilli += System.currentTimeMillis() - prev;
                Thread.sleep(300);
            }
            averageMilli /= attempts;
        } catch (IOException | InterruptedException ignored) {
        }

        for (int i = 0; i < attempts; i++) {
            try {
                sockets[i].close();
            } catch (IOException ignored) {
            }
        }
        return (int) averageMilli;
    }

    public static NetworkState checkAndGetAccessType(SocketProtector socketProtector) {
        try (SocketChannel channel = SocketChannel.open()) {
            Socket socket = channel.socket();
            socketProtector.protectSocks(socket);
            InetAddress[] addresses = InetAddress.getAllByName("google.com");
            for (InetAddress address : addresses) {
                if (address instanceof Inet4Address) {
                    socket.connect(new InetSocketAddress(address, 443), 2500);
                    socket.close();
                    return NetworkState.WORLD_WIDE;
                }
            }
        } catch (IOException e) {
            try (SocketChannel channel = SocketChannel.open()) {
                Socket socket = channel.socket();
                socketProtector.protectSocks(socket);
                InetAddress[] addresses = InetAddress.getAllByName("google.com");
                for (InetAddress address : addresses) {
                    if (address instanceof Inet4Address) {
                        socket.connect(new InetSocketAddress("snapp.ir", 443), 1500);
                        socket.close();
                        return NetworkState.RESTRICTED;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return NetworkState.NO_ACCESS;
    }


    /**
     * @throws IOException when failed to find any open ports
     */
    public static int checkOrFindFreePort(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                return serverSocket.getLocalPort();
            }
        }
    }

    public static void checkAndSetEndpointAddress(SocketProtector socketProtector) {
        if (socketProtector == null) {
            socketProtector = socket -> {};
        }
//        try ()
    }
}