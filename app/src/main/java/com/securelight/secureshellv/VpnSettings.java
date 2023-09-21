package com.securelight.secureshellv;

import java.io.Serializable;

public class VpnSettings implements Serializable {

    private String iFaceAddress;
    private int iFacePrefix;
    private int socksPort;
    private String iFaceSubnet;
    private String dnsHost;

    public VpnSettings() {
        iFaceAddress = "169.254.1.0";
        iFacePrefix = 24;
        iFaceSubnet = "255.255.255.0";
        socksPort = 10808;
        dnsHost = "8.8.8.8";
    }

    public int getSocksPort() {
        return socksPort;
    }

    public String getIFaceAddress() {
        return iFaceAddress;
    }

    public String getDnsHost() {
        return dnsHost;
    }

    public void setIFaceAddress(String iFaceAddress) {
        this.iFaceAddress = iFaceAddress;
    }

    public void setSocksPort(int iFacePort) {
        this.socksPort = iFacePort;
    }

    public void setDnsHost(String dnsHost) {
        this.dnsHost = dnsHost;
    }

    public String getIFaceSubnet() {
        return iFaceSubnet;
    }

    public int getIFacePrefix() {
        return iFacePrefix;
    }
}