package com.securelight.secureshellv.vpnservice.listeners;

import java.net.Socket;

public interface SocketProtector {
    void protectSocks(Socket socket);
}
