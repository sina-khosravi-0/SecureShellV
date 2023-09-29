package com.securelight.secureshellv;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesSingleton {

    private static SharedPreferencesSingleton instance;
    private final Context context;
    private SharedPreferences userSettingsPreferences;

    private SharedPreferencesSingleton(Context context) {
        this.context = context.getApplicationContext();
        userSettingsPreferences = context.getSharedPreferences(Constants.USER_SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferencesSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesSingleton(context);
        }
        return instance;
    }

    public String getSelectedServerLocation() {
        return userSettingsPreferences.getString(Constants.SERVER_LOCATION_PREFERENCES_NAME, Constants.SERVER_LOCATION_DEFAULT);
    }

    public void setServer(String code) {
        userSettingsPreferences.edit().putString(Constants.SERVER_LOCATION_PREFERENCES_NAME, code).apply();
    }

    public boolean isRapidSwitched() {
        return userSettingsPreferences.getBoolean(Constants.RAPID_SWITCH_PREFERENCES_NAME, false);
    }

    public void setRapidSwitched(boolean value) {
        userSettingsPreferences.edit().putBoolean(Constants.RAPID_SWITCH_PREFERENCES_NAME, value).apply();
    }

    public boolean isIranSwitched() {
        return userSettingsPreferences.getBoolean(Constants.IRAN_SWITCH_PREFERENCES_NAME, false);
    }
    public void setIranSwitched(boolean value) {
        userSettingsPreferences.edit().putBoolean(Constants.IRAN_SWITCH_PREFERENCES_NAME, value).apply();
    }
}
