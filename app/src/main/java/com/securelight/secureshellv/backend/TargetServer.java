package com.securelight.secureshellv.backend;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TargetServer {
    static class ISPScore {
        String ispName;
        double ispScore;

        public void parseData(JSONObject data) throws JSONException {
            ispName = data.getString("isp_name");
            ispScore = data.getDouble("score");
        }

        @NonNull
        @Override
        public String toString() {
            return "ISPScore{" +
                    "ispName='" + ispName + '\'' +
                    ", ispScore=" + ispScore +
                    '}';
        }
    }

    private int id;
    private final List<ISPScore> ispScores = new ArrayList<>();
    private String ip;
    private String locationCode;
    private String type;
    private int port;
    private int local_ip;
    private int local_port;

    public void parseData(JSONObject data) throws JSONException {
        id = data.getInt("id");
        for (int i = 0; i < data.getJSONArray("isp_scores").length(); i++) {
            ISPScore ispScore = new ISPScore();
            ispScore.parseData(data.getJSONArray("isp_scores").getJSONObject(i));
            ispScores.add(ispScore);
        }
        ip = data.getString("ip");
        locationCode = data.getString("location_code");
        type = data.getString("type");
        port = data.getInt("port");
        if (!type.equals("D")) {
            local_ip = data.getInt("local_ip");
            local_port = data.getInt("local_port");
        }
    }

    public int getId() {
        return id;
    }

    public List<ISPScore> getIspScores() {
        return ispScores;
    }

    public String getIp() {
        return ip;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getType() {
        return type;
    }

    public int getPort() {
        return port;
    }

    public int getLocal_ip() {
        return local_ip;
    }

    public int getLocal_port() {
        return local_port;
    }

    @NonNull
    @Override
    public String toString() {
        return "TargetServer{" +
                "id=" + id +
                ", ispScores=" + ispScores.toString() +
                ", ip='" + ip + '\'' +
                ", locationCode='" + locationCode + '\'' +
                ", type='" + type + '\'' +
                ", port=" + port +
                ", local_ip=" + local_ip +
                ", local_port=" + local_port +
                '}';
    }
}
