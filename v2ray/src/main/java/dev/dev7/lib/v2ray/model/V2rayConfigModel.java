package dev.dev7.lib.v2ray.model;

import java.io.Serializable;
import java.util.ArrayList;

public class V2rayConfigModel implements Serializable {

    public String applicationName;
    public int applicationIcon;
    public String remark;
    public ArrayList<String> blockedApplications = null;
    public String fullJsonConfig;
    public String currentServerAddress = "";
    public int currentServerPort = 443;
    public int localSocksPort = 10808;
    public int localHttpPort = 10809;
    public int localDNSPort = 10853;
    public boolean enableTrafficStatics = false;
    public boolean enableTrafficStaticsOnNotification = false;
    public boolean enableLocalTunneledDNS = true;

    @Override
    public String toString() {
        return "V2rayConfigModel{" +
                "applicationName='" + applicationName + '\'' +
                ", applicationIcon=" + applicationIcon +
                ", remark='" + remark + '\'' +
                ", blockedApplications=" + blockedApplications +
                ", fullJsonConfig='" + fullJsonConfig + '\'' +
                ", currentServerAddress='" + currentServerAddress + '\'' +
                ", currentServerPort=" + currentServerPort +
                ", localSocksPort=" + localSocksPort +
                ", localHttpPort=" + localHttpPort +
                ", localDNSPort=" + localDNSPort +
                ", enableTrafficStatics=" + enableTrafficStatics +
                ", enableTrafficStaticsOnNotification=" + enableTrafficStaticsOnNotification +
                ", enableLocalTunneledDNS=" + enableLocalTunneledDNS +
                '}';
    }
}
