package com.securelight.secureshellv.tun2socks;


import com.securelight.secureshellv.statics.V2rayConstants;

public interface Tun2SocksListener {
    void OnTun2SocksHasMassage(V2rayConstants.CORE_STATES tun2SocksState, String newMessage);
}
