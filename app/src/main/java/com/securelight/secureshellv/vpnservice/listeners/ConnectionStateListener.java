package com.securelight.secureshellv.vpnservice.listeners;

import com.securelight.secureshellv.vpnservice.connection.ConnectionState;

public interface ConnectionStateListener {
    void onConnectionStateListener(ConnectionState connectionState);
}
