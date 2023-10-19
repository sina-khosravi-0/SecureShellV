package com.securelight.secureshellv.vpnservice.connection;

import com.securelight.secureshellv.statics.Values;

public enum ConnectionState {
    CONNECTED(Values.CONNECTED_STRING),
    CONNECTING(Values.CONNECTING_STRING),
    DISCONNECTED(Values.DISCONNECTED_STRING);

    public final String value;

    ConnectionState(String value) {
        this.value = value;
    }

}
