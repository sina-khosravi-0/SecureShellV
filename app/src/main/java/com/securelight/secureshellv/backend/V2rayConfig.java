package com.securelight.secureshellv.backend;

import static dev.dev7.lib.v2ray.utils.V2rayConfigs.currentConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dev.dev7.lib.v2ray.V2rayController;

public class V2rayConfig {
    JSONObject json;
    JSONArray ips;
    int bestIpIndex = -1;

    public void parseData(JSONObject jsonObject) throws JSONException {
        json = new JSONObject(jsonObject.getString("json"));
        ips = new JSONArray(jsonObject.getJSONObject("server").getJSONArray("ips").toString());
    }

    public double calculateBestPing() throws JSONException {
        JSONObject alteredJson = new JSONObject(json.toString());
        double bestPing = Double.MAX_VALUE;
        for (int i = 0; i < ips.length(); i++) {
            String ip = ips.getJSONObject(i).getString("ip");
            alteredJson.getJSONArray("outbounds")
                    .getJSONObject(0)
                    .getJSONObject("settings")
                    .getJSONArray("vnext")
                    .getJSONObject(0)
                    .put("address", ip);
            double ping = V2rayController.getV2rayServerDelay(alteredJson.toString());
            if (ping < bestPing) {
                bestPing = ping;
                this.bestIpIndex = i;
                json = alteredJson;
            }
        }
        return bestPing;
    }

    public String getJson() {
        return json.toString();
    }
}
