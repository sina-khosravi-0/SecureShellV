/*
 * Copyright (C) Psiphon Inc.
 * Released under badvpn licence: https://github.com/ambrop72/badvpn#license
 */

package com.securelight.secureshellv.tun2socks;

import android.util.Log;

import com.securelight.secureshellv.statics.Values;

/**
 * runTun2Socks takes a tun device file descriptor (from Android's VpnService,
 * for example) and plugs it into tun2socks, which routes the tun TCP traffic
 * through the specified SOCKS proxy. UDP traffic is sent to the specified
 * udpgw server.
 * The tun device file descriptor should be set to non-blocking mode.
 * tun2Socks does *not* take ownership of the tun device file descriptor; the
 * caller is responsible for closing it after tun2socks terminates.
 * runTun2Socks blocks until tun2socks is stopped by calling terminateTun2Socks.
 * It's safe to call terminateTun2Socks from a different thread.
 * logTun2Socks is called from tun2socks.c when an event is to be logged.
 */
public class Tun2SocksJni {
    //basically the same as an interface method
    public static native int runTun2Socks(
            int vpnInterfaceFileDescriptor,
            int vpnInterfaceMTU,
            String vpnIpAddress,
            String vpnNetMask,
            String socksServerAddress,
            String udpgwServerAddress,
            int udpgwTransparentDNS,
            int isDebugging);

    public static native int terminateTun2Socks();

    public static native long getRxBytes();

    public static native long getTxBytes();

    public static native void resetBytes();

    public static native int canStart();

    public static native int canStop();

    public static native long getUDPBytes();

    public static void logTun2Socks(String level, String channel, String msg) {
        String logMsg = String.format("%s (%s): %s", level, channel, msg);
        if (Values.DEBUG) Log.v("Tun2Socks", logMsg);
    }

    public static void sendTrafficData(long txBytes, long rxBytes) {
        Log.v("Tun2Socks", "Transmit: " + txBytes + "\n\tReceived: " + rxBytes);
    }

    static {
        System.loadLibrary("tun2socks");
    }
}