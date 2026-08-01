package com.securelight.secureshellv.statics;

public class Constants {
    //    public static final String apiAddress = "https://api.masterelite.shop/";
    public static final String fallbackEndpoint = "https://backup.tellmewhy.ir/";
    public static final String fallbackEndpointHttp = "http://backup.tellmewhy.ir/";
    public static final String endpoint = "https://10.200.177.90:8000/";
    public static final String endpointHttp = "http://10.200.177.90:8000/";
    public static final String endpointAddress = "http://10.200.177.90:8000/";
    public static final String APP_FILTER_PACKAGES = "appFilterPackages";
    public static final String APP_LANGUAGE_NAME = "appLanguage";
    public static final String LOGGED_IN_PREF_NAME = "loginState";
    public static final String ACCESS_TOKEN_PREF_NAME = "verify";
    public static final String REFRESH_TOKEN_PREF_NAME = "refresh";
    public static final String APP_FILTER_MODE_PREF_NAME = "appFilterMode";
    public static final String SERVER_LOCATION_PREF_NAME = "serverLocation";
    public static final String PERSISTENT_NOTIFICATION_PREF_NAME = "persistentNotification";
    public static final String TOKEN_INVALID_CODE_STRING = "token_not_valid";
    public static final String CREDIT_EXPIRED_CODE_STRING = "credit_expired";
    public static final String OUT_OF_TRAFFIC_CODE_STRING = "data_limit_reached";
    public static final String SERVER_LOCATION_DEFAULT = "ato";
    public static final String USER_SETTINGS_PREFERENCE_GROUP = "userSettings";
    public static final String API_CACHE_PREFERENCE_GROUP = "apiCache";

    public enum Protocol {
        DIRECT_SSH("direct"), TLS_SSH("tls"), DUAL_SSH("dual-hop");

        public final String value;

        Protocol(String value) {
            this.value = value;
        }
    }

    public enum AppFilterMode {
        OFF(0),
        EXCLUDE(1),
        INCLUDE(2);
        public final int value;

        AppFilterMode(int value) {
            this.value = value;
        }
    }

    public enum InternetQuality {
        EXCELLENT,
        GOOD,
        MEDIUM,
        BAD,
        HORRIBLE
    }

    public enum ApiHeartbeatResult {
        SUCCESS,
        ERROR,
        TIMEOUT,
        SHOULD_RESTART
    }

    public static final int SEND_TRAFFIC_PERIOD = 10000;
    public static final int API_HEARTBEAT_PERIOD = 8000;
    public static final int SOCKS_HEARTBEAT_PERIOD = 5000;
    public static final int INTERNET_ACCESS_PERIOD = 3000;
}
