package com.securelight.secureshellv.backend;

import com.securelight.secureshellv.vpnservice.VpnSettings;

import java.io.Serializable;
import java.util.ArrayList;

public class V2rayConfigModel implements Serializable {

    public String remark;
    public ArrayList<String> blockedApplications = null;
    public String fullJsonConfig;
    public String currentServerAddress = "";
    public int currentServerPort = 443;
    public int localSocksPort = VpnSettings.socksPort;
    public int localHttpPort = VpnSettings.httpPort;
    public int localDNSPort = VpnSettings.localDnsPort;
    public boolean enableFakeDns = false;

    @Override
    public String toString() {
        return "V2rayConfigModel{" +
                ", remark='" + remark + '\'' +
                ", blockedApplications=" + blockedApplications +
                ", fullJsonConfig='" + fullJsonConfig + '\'' +
                ", currentServerAddress='" + currentServerAddress + '\'' +
                ", currentServerPort=" + currentServerPort +
                ", localSocksPort=" + localSocksPort +
                ", localHttpPort=" + localHttpPort +
                ", localDNSPort=" + localDNSPort +
                '}';
    }
}
