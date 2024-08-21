package com.securelight.secureshellv.vpnservice;

import java.io.Serializable;

public class VpnSettings implements Serializable {
    public static final String iFaceAddress;
    public static final String iFaceSubnetMask;
    public static final int iFacePrefix;
    public static final int socksPort;
    public static final String dnsHost;
    public static int interfaceMtu;

    static {
        iFaceAddress = "26.26.26.1";
        iFaceSubnetMask = "255.255.255.252";
        iFacePrefix = 30;
        socksPort = 10808;
        dnsHost = "8.8.8.8";
        interfaceMtu = 1500;
    }
}