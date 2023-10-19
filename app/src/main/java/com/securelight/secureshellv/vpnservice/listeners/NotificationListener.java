package com.securelight.secureshellv.vpnservice.listeners;

import com.securelight.secureshellv.vpnservice.connection.ConnectionState;
import com.securelight.secureshellv.vpnservice.connection.NetworkState;

public interface NotificationListener {
    void updateNotification(NetworkState networkState, ConnectionState connectionState);
}
