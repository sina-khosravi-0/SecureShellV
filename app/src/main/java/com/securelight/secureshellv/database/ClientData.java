package com.securelight.secureshellv.database;

import java.util.ArrayList;
import java.util.List;

public class ClientData {
    private static String userName;
    private static final List<String> serverAddresses = new ArrayList<>();
    private static int numOfAllowedConnections;
    private static int price;
    private static int daysLeft;

    static {
        userName = "sina";
        serverAddresses.add("64.226.64.126");
    }

    public static char[] getSshPassword() {
        //todo: fetch password from database
        char[] sshPassword = {'S', 'i', 'n', 'a', '@', '1', '3', '1', '1', '2', '0', '4', '0'};
        return sshPassword;
    }

    public static String getUserName() {
        return userName;
    }

    public static List<String> getServerAddresses() {
        return serverAddresses;
    }

    public static int getNumOfAllowedConnections() {
        return numOfAllowedConnections;
    }

    public static int getPrice() {
        return price;
    }

    public static int getDaysLeft() {
        return daysLeft;
    }
}
