package com.securelight.secureshellv;

import java.io.Serializable;

public class VPNSettings implements Serializable {

    private String host;
    private int port;
    private String dnsHost;

    public VPNSettings(){
        host = "10.87.0.37";
//        host = "127.0.0.1";
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

}