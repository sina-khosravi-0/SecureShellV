package com.securelight.secureshellv.vpnservice.connection;

import com.securelight.secureshellv.statics.Values;

public enum NetworkState {
    WORLD_WIDE(Values.FULL_INTERNET_ACCESS_STRING),
    RESTRICTED(Values.RESTRICTED_INTERNET_ACCESS_STRING),
    NO_ACCESS(Values.NO_INTERNET_ACCESS_STRING),
    UNAVAILABLE(Values.NETWORK_UNAVAILABLE_STRING);

    public final String value;

    NetworkState(String value) {
        this.value = value;
    }
}
