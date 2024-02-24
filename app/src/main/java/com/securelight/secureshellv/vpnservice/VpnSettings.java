package com.securelight.secureshellv.vpnservice;

import java.io.Serializable;

public class VpnSettings implements Serializable {
//    public static final String domainName;
    public static final String iFaceAddress;
    public static final String iFaceSubnet;
    public static final int iFacePrefix;
    public static final int socksPort;
    public static final String dnsHost;

    static {
//        domainName = "weary.tech";
        iFaceAddress = "169.254.1.1";
        iFaceSubnet = "255.255.255.0";
        iFacePrefix = 24;
        socksPort = 10808;
        dnsHost = "8.8.8.8";
    }
}