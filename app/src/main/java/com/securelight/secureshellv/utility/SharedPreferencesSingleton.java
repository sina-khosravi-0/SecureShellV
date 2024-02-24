package com.securelight.secureshellv.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.ArraySet;

import com.securelight.secureshellv.statics.Constants;

import java.util.HashSet;
import java.util.Set;

public class SharedPreferencesSingleton {

    private static SharedPreferencesSingleton instance;
    private final SharedPreferences userSettingsPreferences;
    private final SharedPreferences apiCachePreferences;

    private SharedPreferencesSingleton(Context context) {
        userSettingsPreferences = context.getSharedPreferences(Constants.USER_SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE);
        apiCachePreferences = context.getSharedPreferences(Constants.API_CACHE_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferencesSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesSingleton(context);
        }
        return instance;
    }

    public void setServerLocation(String code) {
        userSettingsPreferences.edit().putString(Constants.SERVER_LOCATION_PREFERENCES_NAME, code).apply();
    }

    public String getSelectedServerLocationForDropDown() {
        return userSettingsPreferences.getString(Constants.SERVER_LOCATION_PREFERENCES_NAME, Constants.SERVER_LOCATION_DEFAULT);
    }

    public String getSelectedServerLocation() {
        String selected = getSelectedServerLocationForDropDown();
        return selected.equals("ato") ? "" : selected;
    }

    public void clearFilteredPackages() {
        userSettingsPreferences.edit().remove(Constants.APP_FILTER_PACKAGES).apply();
    }

    public void addToPackageFilter(String packageName) {
        Set<String> packages = userSettingsPreferences.getStringSet(Constants.APP_FILTER_PACKAGES,
                new HashSet<>());
        Set<String> packagesCopy = new ArraySet<>();
        packagesCopy.addAll(packages);
        packagesCopy.add(packageName);
        userSettingsPreferences.edit().putStringSet(Constants.APP_FILTER_PACKAGES, packagesCopy).apply();
    }

    public Set<String> getFilteredPackages() {
        return userSettingsPreferences.getStringSet(Constants.APP_FILTER_PACKAGES,
                new ArraySet<>());
    }

    public boolean isPackageFiltered(String packageName) {
        HashSet<String> packages = (HashSet<String>) userSettingsPreferences.getStringSet(Constants.APP_FILTER_PACKAGES,
                new HashSet<>());
        return packages.stream().anyMatch(s -> s.equals(packageName));
    }

    public Constants.AppFilterMode getAppFilterMode() {
        switch (userSettingsPreferences.getInt(Constants.APP_FILTER_MODE_NAME, Constants.AppFilterMode.OFF.value)) {
            default: // off
                return Constants.AppFilterMode.OFF;
            case 1: // exclude
                return Constants.AppFilterMode.EXCLUDE;
            case 2: // include
                return Constants.AppFilterMode.INCLUDE;
        }
    }

    public void setAppFilterMode(Constants.AppFilterMode mode) {
        userSettingsPreferences.edit().putInt(Constants.APP_FILTER_MODE_NAME, mode.value).apply();
    }

    public String getAppLanguage() {
        return userSettingsPreferences.getString(Constants.APP_LANGUAGE_NAME,
                Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage());
    }

    public void setAppLanguage(String language) {
        userSettingsPreferences.edit().putString(Constants.APP_LANGUAGE_NAME, language).apply();
    }

    public void saveAccessToken(String access) {
        apiCachePreferences.edit().putString(Constants.ACCESS_TOKEN_PREF_NAME, access).apply();
    }

    public void saveRefreshToken(String refresh) {
        apiCachePreferences.edit().putString(Constants.REFRESH_TOKEN_PREF_NAME, refresh).apply();
    }

    public String getAccessToken() {

        return apiCachePreferences.getString(Constants.ACCESS_TOKEN_PREF_NAME, "289734582");
    }

    public String getRefreshToken() {
        return apiCachePreferences.getString(Constants.REFRESH_TOKEN_PREF_NAME, "27831891");
    }

    public void clearTokens() {
        apiCachePreferences.edit().remove(Constants.ACCESS_TOKEN_PREF_NAME).apply();
        apiCachePreferences.edit().remove(Constants.REFRESH_TOKEN_PREF_NAME).apply();
    }
}
