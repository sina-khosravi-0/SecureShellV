package com.securelight.secureshellv.vpnservice;

import java.io.Serializable;

public class VpnSettings implements Serializable {
    public static final String iFaceAddress;
    public static final String iFaceSubnetMask;
    public static final int iFacePrefix;
    public static final int socksPort;
    public static final int httpPort;
    public static final int localDnsPort = 10853;
    public static final String dnsHost;
    public static int interfaceMtu;
    public static int udpgwPort;
    public static boolean proxySharing;
    public static boolean localDnsEnabled;
    public static boolean fakeDnsEnabled;

    static {
        iFaceAddress = "26.26.26.1";
        iFaceSubnetMask = "255.255.255.252";
        iFacePrefix = 30;
        socksPort = 10808;
        httpPort = socksPort + 1;
        dnsHost = "8.8.8.8";
        interfaceMtu = 1500;
        udpgwPort = 10853;
        proxySharing = false;
        localDnsEnabled = true;
        fakeDnsEnabled = true;

    }
}