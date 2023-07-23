package com.securelight.secureshellv.connection;


import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class BroadCastSampleActivity {
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

    public static boolean isInternetAvailable() {
        try {
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress("google.com", 443);
            socket.connect(socketAddress);
            socket.close();
            return true;
        } catch (IOException e){
             return false;
        }
    }


}