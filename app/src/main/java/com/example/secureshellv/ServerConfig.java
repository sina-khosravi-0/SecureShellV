package com.example.secureshellv;

import java.io.Serializable;

public class ServerConfig implements Serializable {

    private String host;
    private int port;

    public ServerConfig(){
        host = "127.0.0.1";
        port = 2000;
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