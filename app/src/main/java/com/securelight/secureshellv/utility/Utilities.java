package com.securelight.secureshellv.utility;


import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.util.Log;

import com.securelight.secureshellv.backend.V2rayConfigModel;
import com.securelight.secureshellv.statics.V2rayConstants;
import com.securelight.secureshellv.vpnservice.VpnSettings;
import com.securelight.secureshellv.vpnservice.connection.NetworkState;
import com.securelight.secureshellv.vpnservice.listeners.SocketProtector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class Utilities {

    public static V2rayConstants.CONNECTION_STATES connectionState = V2rayConstants.CONNECTION_STATES.DISCONNECTED;
    public static V2rayConstants.SERVICE_MODES serviceMode = V2rayConstants.SERVICE_MODES.VPN_MODE;
    public static V2rayConfigModel currentConfig = new V2rayConfigModel();


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

    public static int convertDPtoPX(Resources resources, int dp) {
        final float scale = resources.getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static boolean refillV2rayConfig(String remark,
                                            String config,
                                            final ArrayList<String> blockedApplications,
                                            boolean trafficStatsEnabled) {
        currentConfig.remark = remark;
        currentConfig.blockedApplications = blockedApplications;
        try {
            JSONObject config_json =
                    new JSONObject(/*todo if need uri config*//*normalizeV2rayFullConfig*/(config));
            try {
                JSONArray inbounds = config_json.getJSONArray("inbounds");
                for (int i = 0; i < inbounds.length(); i++) {
                    try {
                        if (inbounds.getJSONObject(i).getString("protocol").equals("socks")) {
                            currentConfig.localSocksPort = inbounds.getJSONObject(i).getInt("port");
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                    try {
                        if (inbounds.getJSONObject(i).getString("protocol").equals("http")) {
                            currentConfig.localHttpPort = inbounds.getJSONObject(i).getInt("port");
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            } catch (Exception e) {
                Log.w("RefillCurrentConfig", "startCore warn => can`t find inbound port of socks5 or http.");
                return false;
            }
            try {
                currentConfig.currentServerAddress = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getString("address");
                currentConfig.currentServerPort = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getInt("port");
            } catch (Exception e) {
                currentConfig.currentServerAddress = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getString("address");
                currentConfig.currentServerPort = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getInt("port");
            }
            try {
                if (config_json.has("policy")) {
                    config_json.remove("policy");
                }
                if (config_json.has("stats")) {
                    config_json.remove("stats");
                }
            } catch (Exception ignore_error) {
                //ignore
            }
            if (trafficStatsEnabled) {
                try {
                    JSONObject policy = new JSONObject();
                    JSONObject levels = new JSONObject();
                    levels.put("8", new JSONObject().put("connIdle", 300).put("downlinkOnly", 1).put("handshake", 4).put("uplinkOnly", 1));
                    JSONObject system = new JSONObject().put("statsOutboundUplink", true).put("statsOutboundDownlink", true);
                    policy.put("levels", levels);
                    policy.put("system", system);
                    config_json.put("policy", policy);
                    config_json.put("stats", new JSONObject());
                } catch (Exception e) {
                    Log.e("refillV2rayConfig", e.toString());
                    //ignore
                }
            }

            try {
                config_json.getJSONArray("inbounds").put(new JSONObject("{\"listen\": \"127.0.0.1\",\"port\": 10853,\"protocol\": \"dokodemo-door\",\"settings\": {\"address\": \"1.1.1.1\",\"network\": \"tcp,udp\",\"port\": 53},\"tag\": \"dns-in\"}"));
                config_json.getJSONArray("outbounds").put(new JSONObject("{\"protocol\": \"dns\", \"tag\": \"dns-out\"}"));
                config_json.put("dns", new JSONObject("{\n" +
                        "    \"hosts\": {\n" +
                        "      \"domain:googleapis.cn\": \"googleapis.com\",\n" +
                        "      \"dns.pub\": [\n" +
                        "        \"1.12.12.12\",\n" +
                        "        \"120.53.53.53\"\n" +
                        "      ],\n" +
                        "      \"dns.alidns.com\": [\n" +
                        "        \"223.5.5.5\",\n" +
                        "        \"223.6.6.6\",\n" +
                        "        \"2400:3200::1\",\n" +
                        "        \"2400:3200:baba::1\"\n" +
                        "      ],\n" +
                        "      \"one.one.one.one\": [\n" +
                        "        \"1.1.1.1\",\n" +
                        "        \"1.0.0.1\",\n" +
                        "        \"2606:4700:4700::1111\",\n" +
                        "        \"2606:4700:4700::1001\"\n" +
                        "      ],\n" +
                        "      \"dns.google\": [\n" +
                        "        \"8.8.8.8\",\n" +
                        "        \"8.8.4.4\",\n" +
                        "        \"2001:4860:4860::8888\",\n" +
                        "        \"2001:4860:4860::8844\"\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    \"servers\": [\n" +
                        "      {\n" +
                        "        \"address\": \"fakedns\",\n" +
                        "        \"domains\": [\n" +
                        "          \"geosite:cn\"\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      \"1.1.1.1\",\n" +
                        "      {\n" +
                        "        \"address\": \"223.5.5.5\",\n" +
                        "        \"domains\": [\n" +
                        "          \"geosite:cn\"\n" +
                        "        ],\n" +
                        "        \"expectIPs\": [\n" +
                        "          \"geoip:cn\"\n" +
                        "        ],\n" +
                        "        \"port\": 53\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }"));
                if (VpnSettings.fakeDnsEnabled) {
                    config_json.put("fakedns", new JSONArray("[\n" +
                            "    {\n" +
                            "      \"ipPool\": \"198.18.0.0/15\",\n" +
                            "      \"poolSize\": 10000\n" +
                            "    }\n" +
                            "  ]"));
                }
            } catch (JSONException ignored) {
            }
            currentConfig.fullJsonConfig = config_json.toString();
            return true;
        } catch (Exception e) {
            Log.e("RefillV2rayConfig", "parseV2rayJsonFile failed => ", e);
            return false;
        }
    }

    public static int[] parseIranianDate(String date) {
        if (date == null || !date.matches("\\d+-\\d+-\\d+")) {
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD");
        }

        String[] parts = date.split("-");

        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);

        return new int[]{year, month, day};
    }
//    todo: do this if need uri
//    public static String normalizeV2rayFullConfig(String config) {
//        if (Libv2ray.isXrayURI(config)) {
//            return V2rayConstants.DEFAULT_FULL_JSON_CONFIG.replace(DEFAULT_OUT_BOUND_PLACE_IN_FULL_JSON_CONFIG, Libv2ray.getXrayOutboundFromURI(config));
//        }
//        return config;
//    }

}
