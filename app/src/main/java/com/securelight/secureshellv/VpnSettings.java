package com.securelight.secureshellv;

import java.io.Serializable;

public class VpnSettings implements Serializable {

    private String host;
    private int port;
    private String dnsHost;
    private String subnet;
    int prefix;

    public VpnSettings(){
        host = "10.12.10.13";
        prefix = 24;
        subnet = "10.0.0.0";

        port = 10808;
        dnsHost = "8.8.8.8";
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getDnsHost() {
        return dnsHost;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDnsHost(String dnsHost) {
        this.dnsHost = dnsHost;
    }

    public String getSubnet() {
        return subnet;
    }

    public int getPrefix() {
        return prefix;
    }
}