package com.securelight.secureshellv.statics;

public class Constants {

    public static final String APP_FILTER_PACKAGES = "appFilterPackages";
    public static final String APP_LANGUAGE_NAME = "appLanguage";
    public static String API_CACHE_PREFERENCES_NAME = "apiCache";
    public static String SERVER_LOCATION_DEFAULT = "ato";
    public static String USER_SETTINGS_PREFERENCES_NAME = "userSettings";
    public static String SERVER_LOCATION_PREFERENCES_NAME = "serverLocation";
    public static String RAPID_SWITCH_PREFERENCES_NAME = "rapidSwitch";
    public static String IRAN_SWITCH_PREFERENCES_NAME = "iranSwitch";
    public static final String APP_FILTER_MODE_NAME = "appFilterMode";

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
}
