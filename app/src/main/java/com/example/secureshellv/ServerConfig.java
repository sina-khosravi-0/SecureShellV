package com.example.secureshellv;

import java.io.Serializable;

public class ServerConfig implements Serializable {

    private String host;
    private int port;

    public ServerConfig(){
        host = "10.87.0.37";
        port = 10808;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
}