package com.securelight.secureshellv.backend;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ServicePlan {
    private int id;
    private int price;
    private int users;
    private int traffic;
    private boolean unlimitedUsers;

    public void parseData(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getInt("id");
        price = jsonObject.getInt("price");
        users = jsonObject.getInt("users");
        traffic = jsonObject.getInt("traffic");
        unlimitedUsers = jsonObject.getBoolean("unlimited_users");
    }

    public int getId() {
        return id;
    }

    public int getPrice() {
        return price;
    }

    public int getUsers() {
        return users;
    }

    public int getTraffic() {
        return traffic;
    }

    public boolean isUnlimitedUsers() {
        return unlimitedUsers;
    }

    @NonNull
    @Override
    public String toString() {
        return "ServicePlan{" +
                "id=" + id +
                ", price=" + price +
                ", users=" + users +
                ", traffic=" + traffic +
                ", unlimitedUsers=" + unlimitedUsers +
                '}';
    }
}
