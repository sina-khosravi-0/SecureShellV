package com.securelight.secureshellv.vpnservice;

import android.util.Log;

import com.securelight.secureshellv.utility.NetTools;
import com.securelight.secureshellv.utility.Utilities;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;

public class VpnSettings implements Serializable {
    public static String iFaceAddress;
    public static String iFaceSubnetMask;
    public static final int iFacePrefix;
    public static final String dnsHost;
    public static int socksPort;
    public static int httpPort;
    public static int localDnsPort = 10853;
    public static int interfaceMtu;
    public static int udpgwPort;
    public static boolean proxySharing;
    public static boolean localDnsEnabled;
    public static boolean fakeDnsEnabled;
    public static boolean trafficStatsEnabled;

    static {
        iFaceAddress = "26.26.26.1";
        iFaceSubnetMask = "255.255.255.252";
        iFacePrefix = 30;
        try {
            socksPort = NetTools.checkOrFindFreePort(10808);
            httpPort = NetTools.checkOrFindFreePort(10809);
            localDnsPort = NetTools.checkOrFindFreePort(10853);
        } catch (IOException e) {
            Log.e("VpnSettings", "Failed to find any open ports!");
            throw new RuntimeException(e);
        }
        dnsHost = "8.8.8.8";
        interfaceMtu = 1500;
        udpgwPort = 10853;
        proxySharing = false;
        localDnsEnabled = true;
        fakeDnsEnabled = false;
        trafficStatsEnabled = true;
    }
}