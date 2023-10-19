package com.securelight.secureshellv.vpnservice.connection;


import android.util.Log;

import java.io.IOException;

public class Tools {
    public static boolean checkInternetAccess() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process mIpAdderProcess = runtime.exec("/system/bin/ping -c 1 google.com");
            int mExitValue = mIpAdderProcess.waitFor();

            return mExitValue == 0;

        } catch (InterruptedException | IOException e) {
            Log.d("Error", "Ping", e);
        }
        return false;
    }
}