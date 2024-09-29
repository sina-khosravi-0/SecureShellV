package com.securelight.secureshellv.statics

enum class EConfigType(val value: Int, val protocolScheme: String) {
    VMESS(1, "VMESS"),
    CUSTOM(2, "CUSTOM"),
    SHADOWSOCKS(3, "SHADOWSOCKS"),
    SOCKS(4, "SOCKS"),
    VLESS(5, "VLESS"),
    TROJAN(6, "TROJAN"),
    WIREGUARD(7, "WIREGUARD");

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value }
    }
}
