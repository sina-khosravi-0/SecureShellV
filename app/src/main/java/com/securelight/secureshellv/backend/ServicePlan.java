package com.securelight.secureshellv.backend;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ServicePlan implements Parcelable {
    private int id;
    private int price;
    private int users;
    private int traffic;
    private int months;
    private boolean gold;

    public ServicePlan(){

    }

    protected ServicePlan(Parcel in){

        id = in.readInt();
        price = in.readInt();
        users = in.readInt();
        traffic = in.readInt();
        months = in.readInt();
        gold = in.readByte() != 0;
    }

    public static final Creator<ServicePlan> CREATOR = new Creator<ServicePlan>() {
        @Override
        public ServicePlan createFromParcel(Parcel in) {
            return new ServicePlan(in);
        }

        @Override
        public ServicePlan[] newArray(int size) {
            return new ServicePlan[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(price);
        dest.writeInt(users);
        dest.writeInt(traffic);
        dest.writeInt(months);
        dest.writeByte((byte) (gold ? 1 : 0));
    }
}
