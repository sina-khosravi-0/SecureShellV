package com.securelight.secureshellv.backend;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TargetServer {
    public enum Type {
        M("MASTER"),
        N("NODE");

        public final String value;

        Type(String value) {
            this.value = value;
        }
    }

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
    private Type type;
    private int pingPort;
    private String local_ip = "";
    private int local_port = 0;

    public void parseData(JSONObject data) throws JSONException {
        id = data.getInt("id");
        for (int i = 0; i < data.getJSONArray("isp_scores").length(); i++) {
            ISPScore ispScore = new ISPScore();
            ispScore.parseData(data.getJSONArray("isp_scores").getJSONObject(i));
            ispScores.add(ispScore);
        }
        ip = data.getString("address");
        locationCode = data.getString("location");
        type = Type.valueOf(data.getString("type"));
        pingPort = data.getInt("ping_port");
    }

    public static TargetServer newEmptyServer(String s) {
        TargetServer server = new TargetServer();
        server.id = -1;
        server.ip = "";
        server.locationCode = "";
        server.type = null;
        server.pingPort = 0;
        server.local_ip = "";
        server.local_port = 0;
        return server;
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

    public Type getType() {
        return type;
    }

    public int getPingPort() {
        return pingPort;
    }

    public String getLocal_ip() {
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
                ", port=" + pingPort +
                ", local_ip=" + local_ip +
                ", local_port=" + local_port +
                '}';
    }
}
