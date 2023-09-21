package com.securelight.secureshellv;

public class Constants {
    public enum Protocol {
        DIRECT_SSH("direct"), TLS_SSH("tls"), DUAL_SSH("dual-hop");

        public final String value;

        Protocol(String value) {
            this.value = value;
        }
    }
    public static String API_CACHE_PREFERENCES_NAME = "apiCache";

}
