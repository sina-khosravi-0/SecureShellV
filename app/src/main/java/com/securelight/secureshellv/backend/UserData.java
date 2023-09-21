package com.securelight.secureshellv.backend;

import android.icu.util.Calendar;
import android.media.Image;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserData {
    private static UserData userData;
    private String userName;
    private final List<String> serverAddresses = new ArrayList<>();
    private double remainingGb;
    private Date endCreditDate;
    private long totalTrafficB;
    private long usedTrafficB;
    private boolean unlimitedCreditTime;
    private boolean unlimitedTraffic;
    private boolean hasPaid;
    private int connectedIps;
    private int allowedIps;
    private Image paymentReceipt;
    private String message;
    private boolean messagePending;
    private int userId;


    private UserData() {
        userName = "sina";
        serverAddresses.add("64.226.64.126");
    }

    public static char[] getSshPassword() {
        //todo: fetch password from database
        char[] sshPassword = {'S', 'i', 'n', 'a', '@', '1', '3', '1', '1', '2', '0', '4', '0'};
        return sshPassword;
    }

    public static synchronized UserData getInstance() {
        if (userData == null) {
            userData = new UserData();
        }
        return userData;
    }

    public void parseData(double remainingGb,
                          String endCreditDate,
                          long totalTrafficB,
                          long usedTrafficB,
                          boolean unlimitedCreditTime,
                          boolean unlimitedTraffic,
                          boolean hasPaid,
                          int connectedIps,
                          int allowedIps,
                          String paymentReceipt,
                          String message,
                          boolean messagePending,
                          int userId) {

        if (userData == null) {
            return;
        }
        System.out.println("hello");
        this.remainingGb = remainingGb;

        Calendar calendar = Calendar.getInstance();
        try {
            // yy-mm-ddThh:mm:ss -> date=yy,mm,dd time=hh,mm,ss
            String[] dateTime = endCreditDate.split("T");
            String[] date = dateTime[0].split("-");
            String[] time = dateTime[1].split(":");

            calendar.set(Integer.parseInt(date[0]),
                    Integer.parseInt(date[1]),
                    Integer.parseInt(date[2]),
                    Integer.parseInt(time[0]),
                    Integer.parseInt(time[1]),
                    Integer.parseInt(time[2]));
        } catch (NumberFormatException e) {
            calendar.set(0, 0, 0, 0, 0, 0);
        }
        this.endCreditDate = calendar.getTime();

        this.totalTrafficB = totalTrafficB;
        this.usedTrafficB = usedTrafficB;
        this.unlimitedCreditTime = unlimitedCreditTime;
        this.unlimitedTraffic = unlimitedTraffic;
        this.hasPaid = hasPaid;
        this.connectedIps = connectedIps;
        this.allowedIps = allowedIps;
//        this.paymentReceipt = paymentReceipt;
        this.message = message;
        this.messagePending = messagePending;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public List<String> getServerAddresses() {
        return serverAddresses;
    }

    public int getAllowedIps() {
        return allowedIps;
    }

    public double getRemainingGb() {
        return remainingGb;
    }

    public Date getEndCreditDate() {
        return endCreditDate;
    }

    public long getTotalTrafficB() {
        return totalTrafficB;
    }

    public long getUsedTrafficB() {
        return usedTrafficB;
    }

    public boolean isUnlimitedCreditTime() {
        return unlimitedCreditTime;
    }

    public boolean isUnlimitedTraffic() {
        return unlimitedTraffic;
    }

    public boolean isHasPaid() {
        return hasPaid;
    }

    public int getConnectedIps() {
        return connectedIps;
    }

    public Image getPaymentReceipt() {
        return paymentReceipt;
    }

    public String getMessage() {
        return message;
    }

    public boolean isMessagePending() {
        return messagePending;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "userName='" + userName + '\'' +
                ", serverAddresses=" + serverAddresses +
                ", remainingGb=" + remainingGb +
                ", endCreditDate=" + endCreditDate +
                ", totalTrafficB=" + totalTrafficB +
                ", usedTrafficB=" + usedTrafficB +
                ", unlimitedCreditTime=" + unlimitedCreditTime +
                ", unlimitedTraffic=" + unlimitedTraffic +
                ", hasPaid=" + hasPaid +
                ", connectedIps=" + connectedIps +
                ", allowedIps=" + allowedIps +
                ", paymentReceipt=" + paymentReceipt +
                ", message='" + message + '\'' +
                ", messagePending=" + messagePending +
                ", userId=" + userId +
                '}';
    }
}
