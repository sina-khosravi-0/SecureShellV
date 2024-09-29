package com.securelight.secureshellv.utility;

import android.content.res.Resources;

import com.securelight.secureshellv.backend.V2rayConfig;

import org.json.JSONException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Utilities {
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }


    public static byte[] getSHA(String input) throws NoSuchAlgorithmException {
        // Static getInstance method is called with hashing SHA
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // digest() method called
        // to calculate message digest of an input
        // and return array of byte
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String toHexString(byte[] hash) {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 64) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    public static String calculatePassword(String username, String word) {
        try {
            return toHexString(getSHA(username + " 8 " + word + " fuckyou"));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static int convertDPtoPX(Resources resources, int dp){
        final float scale = resources.getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static V2rayConfig getBestV2rayConfig(List<V2rayConfig> configs) {
        double[] pings = new double[configs.size()];
        int bestConfigIndex = 0;
        double bestPing = Double.MAX_VALUE;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            int index  = i;
            V2rayConfig config = configs.get(i);
            Thread thread = new Thread(() -> {
                try {
                    pings[index] = config.calculateBestPing();
                } catch (JSONException ignore) {
                }
            });
            threads.add(thread);
            thread.start();
        }

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        });

        for (int i = 0; i < pings.length; i++) {
            if (bestPing > pings[i]) {
                bestConfigIndex = i;
            }
        }
        return configs.get(bestConfigIndex);
    }
}
