package com.securelight.secureshellv.backend;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ServicePlan {
    private int id;
    private int price;
    private int users;
    private int traffic;
    private int months;
    private boolean gold;

    public void parseData(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getInt("id");
        price = jsonObject.getInt("price");
        users = jsonObject.getInt("users");
        gold = jsonObject.getBoolean("gold");
        if (gold){
            traffic = jsonObject.getInt("traffic");
        } else {
            traffic = 0;
        }
        months = jsonObject.getInt("months");
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

    public int getMonths() {
        return months;
    }

    @NonNull
    @Override
    public String toString() {
        return "ServicePlan{" +
                "id=" + id +
                ", price=" + price +
                ", users=" + users +
                ", traffic=" + traffic +
                ", months=" + months +
                '}';
    }

    public boolean isGold() {
        return gold;
    }
}
