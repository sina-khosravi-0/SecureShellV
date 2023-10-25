package com.securelight.secureshellv.backend;

import android.annotation.SuppressLint;
import android.media.Image;

import com.github.eloyzone.jalalicalendar.DateConverter;
import com.github.eloyzone.jalalicalendar.JalaliDate;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    private static UserData userData;
    private String userName;
    private final List<String> serverAddresses = new ArrayList<>();
    private double remainingTrafficGB;
    private LocalDateTime endCreditDate;
    private double totalTrafficGB;
    private double usedTrafficGB;
    private boolean unlimitedCreditTime;
    private boolean unlimitedTraffic;
    private boolean hasPaid;
    private int connectedIps;
    private int allowedIps;
    private Image paymentReceipt;
    private String message;
    private LocalDateTime messageDate;
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
                          String messageDate,
                          boolean messagePending,
                          int userId) {

        if (userData == null) {
            return;
        }
        this.remainingTrafficGB = Math.round(remainingGb * 100) / 100.;

        try {
            this.endCreditDate = LocalDateTime.parse(endCreditDate);
        } catch (DateTimeParseException e) {
            this.endCreditDate = LocalDateTime.parse("0001-01-01T00:00:00");
        }
        this.totalTrafficGB = Math.round((totalTrafficB / 1000000000.) * 100) / 100.;
        this.usedTrafficGB = Math.round((usedTrafficB / 1000000000.) * 100) / 100.;
        this.unlimitedCreditTime = unlimitedCreditTime;
        this.unlimitedTraffic = unlimitedTraffic;
        this.hasPaid = hasPaid;
        this.connectedIps = connectedIps;
        this.allowedIps = allowedIps;
//        this.paymentReceipt = paymentReceipt;
        this.message = message;
        try {
            this.messageDate = LocalDateTime.parse(messageDate);
        } catch (DateTimeParseException e) {
            this.messageDate = LocalDateTime.parse("0001-01-01T00:00:00");
        }
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

    public double getRemainingTrafficGB() {
        return remainingTrafficGB;
    }

    public int getRemainingPercent() {
        return (int) ((remainingTrafficGB / totalTrafficGB) * 100);
    }

    public LocalDateTime getEndCreditDate() {
        return endCreditDate;
    }

    public String getJalaliEndCreditDate() {
        DateConverter dateConverter = new DateConverter();
        return dateConverter.gregorianToJalali(
                userData.getEndCreditDate().getYear(),
                userData.getEndCreditDate().getMonthValue(),
                userData.getEndCreditDate().getDayOfMonth()).toString();
    }

    public long getDaysLeft() {
        return LocalDateTime.now().until(endCreditDate, ChronoUnit.DAYS);
    }

    public double getTotalTrafficGB() {
        return totalTrafficGB;
    }

    public double getUsedTrafficGB() {
        return usedTrafficGB;
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

    public LocalDateTime getMessageDate() {
        return messageDate;
    }

    @SuppressLint("DefaultLocale")
    public String getMessageDateTimeString() {
        DateConverter dateConverter = new DateConverter();
        JalaliDate date = dateConverter.gregorianToJalali(
                messageDate.getYear(), messageDate.getMonth(), messageDate.getDayOfMonth());

        return String.format("%d/%d/%d %d:%d", date.getYear(), date.getMonthPersian().getValue(), date.getDay(),
                messageDate.getHour(), messageDate.getHour());
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
                ", remainingGb=" + remainingTrafficGB +
                ", endCreditDate=" + endCreditDate +
                ", totalTrafficB=" + totalTrafficGB +
                ", usedTrafficB=" + usedTrafficGB +
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
