package com.securelight.secureshellv.ssh;

import com.securelight.secureshellv.statics.Constants;

public class SshConfigs {
    public String hostAddress;
    public int hostPort;
    public String bridgeHostAddress;
    public int bridgeHostPort;
    public String socksAddress;
    public int socksPort;
    public Constants.Protocol connectionMethod;

    public SshConfigs(String hostAddress,
                      int hostPort,
                      String socksAddress,
                      int socksPort,
                      Constants.Protocol connectionMethod) {
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.socksAddress = socksAddress;
        this.socksPort = socksPort;
        this.connectionMethod = connectionMethod;
    }

    public SshConfigs(String hostAddress,
                      int hostPort,
                      String socksAddress,
                      int socksPort,
                      String bridgeHostAddress,
                      int bridgeHostPort,
                      Constants.Protocol connectionMethod) {
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.socksAddress = socksAddress;
        this.socksPort = socksPort;
        this.bridgeHostAddress = bridgeHostAddress;
        this.bridgeHostPort = bridgeHostPort;
        this.connectionMethod = connectionMethod;
    }
}
