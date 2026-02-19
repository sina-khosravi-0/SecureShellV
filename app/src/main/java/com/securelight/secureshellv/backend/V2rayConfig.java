package com.securelight.secureshellv.backend;


import static com.securelight.secureshellv.vpnservice.v2ray.V2rayCoreManager.getConfigDelay;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.securelight.secureshellv.vpnservice.listeners.SocketProtector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class V2rayConfig {
    private final List<Thread> threads = new ArrayList<>();
    private String[] configs;
    private String json;
    private JSONArray ips;
    private boolean[] reachabilityArray;
    private int bestIpIndex = -1;

    public void parseData(JSONObject jsonObject) throws JSONException {
        json = jsonObject.getString("json");
        ips = new JSONArray(jsonObject.getJSONObject("server").getJSONArray("ips").toString());
        configs = new String[ips.length()];
        mapIpToConfig();
    }

    public double calculateBestPing() {
        if (configs == null) {
            return -1;
        }
        threads.clear();
        List<Long> pings = new ArrayList<>(Collections.nCopies(configs.length, 0L));
        for (int i = 0; i < configs.length; i++) {
            if (!reachabilityArray[i]) {
                continue;
            }
            int finalI = i;
            Thread thread = new Thread(() -> {
                pings.set(finalI, getConfigDelay(configs[finalI]));
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                return -1;
            }
        }
        long bestPing = Long.MAX_VALUE;
        for (int i = 0; i < configs.length; i++) {
            if (pings.get(i) > 0 && pings.get(i) < bestPing) {
                bestPing = pings.get(i);
                this.bestIpIndex = i;
                json = configs[i];
            }
        }

        return bestPing;
    }

    public String getConfig() throws Exception {
        if (bestIpIndex == -1) {
            throw new Exception("Empty Config");
        }
        return json;
    }

    private void mapIpToConfig() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);
        JsonNode config;
        try {
            config = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            Log.e("V2rayConfig.parseData", e.getMessage(), e);
            return;
        }

        for (int i = 0; i < ips.length(); i++) {
            String ip;
            int port;
            try {
                ip = ips.getJSONObject(i).getString("ip");
                port = ips.getJSONObject(i).getInt("port");
            } catch (JSONException e) {
                continue;
            }
            try {
                ObjectNode vnext = (ObjectNode) config.path("outbounds").get(0).get("settings").get("vnext").get(0);
                vnext.put("address", ip);
                vnext.put("port", port);
            } catch (Exception e) {
                ObjectNode settings = (ObjectNode) config.path("outbounds").get(0).get("settings");
                settings.put("address", ip);
                settings.put("port", port);
            }
            try {
                configs[i] = mapper.writeValueAsString(config);
            } catch (JsonProcessingException e) {
                Log.e("V2rayConfig.parseData", e.getMessage(), e);
            }
        }
    }
    public void checkConfigReachability(SocketProtector socketProtector) {
        reachabilityArray = new boolean[configs.length];
        for (int i = 0; i < ips.length(); i++) {
            String address;
            int port;
            try {
                address = ips.getJSONObject(i).getString("ip");
                 port = ips.getJSONObject(i).getInt("port");
            } catch (JSONException e) {
                continue;
            }

            try (SocketChannel channel = SocketChannel.open()) {
                Socket socket = channel.socket();
                socketProtector.protectSocks(socket);
                InetAddress[] addresses = InetAddress.getAllByName(address);
                for (InetAddress ip : addresses) {
                    if (ip instanceof Inet4Address) {
                        socket.connect(new InetSocketAddress(ip, port), 2500);
                        socket.close();
                        reachabilityArray[i] = true;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


//{
//        "dns":{"queryStrategy": "UseIP", "servers":[{"address": "8.8.8.8", "skipFallback": False}],"tag": "dns_out"},
//        "inbounds": [{"port": 10808, "protocol": "mixed", "settings": {"auth": "noauth", "udp": True, "userLevel": 8}, "sniffing": {"destOverride": ["http", "tls", "quic", "fakedns"], "enabled": True}, "tag": "mixed"}, {"port": 10809, "protocol": "http", "settings": {"userLevel": 8}, "tag": "http"}],
//        "log": {"loglevel": "warning"},
//        "outbounds":[
//                {
//                "protocol":"vless",
//                "tag":"proxy",
//                "streamSettings":{"network":"tcp","security":"none","tcpSettings":{"header":{"type":"none"}}},
//                "settings":{"address":"164.90.165.75","encryption":"none","id":"5990c5461e01dac71018909463c29425","port":80}},
//                {"protocol":"freedom","settings":{"domainStrategy":"AsIs","noises":[],"redirect":""},"tag":"direct"},
//                {"protocol":"blackhole","settings":{"response":{"type":"http"}},"tag":"block"}
//                ],
//        "policy": {"levels": {"8": {"connIdle": 300, "downlinkOnly": 1, "handshake": 4, "uplinkOnly": 1}}, "system": {"statsOutboundDownlink": True, "statsOutboundUplink": True}},
//        "remarks": "sina-85dip-1D⏳",
//        "routing": {"domainStrategy": "AsIs", "rules": [{"network": "tcp,udp", "outboundTag": "proxy", "type": "field"}]},
//        "stats": {}}