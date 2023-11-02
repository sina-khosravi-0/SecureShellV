package com.securelight.secureshellv.backend;

import static com.securelight.secureshellv.utility.Utilities.calculatePassword;

import android.annotation.SuppressLint;
import android.media.Image;

import com.github.eloyzone.jalalicalendar.DateConverter;
import com.github.eloyzone.jalalicalendar.JalaliDate;
import com.securelight.secureshellv.utility.NetTools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DataManager {
    private static DataManager dataManager;
    private String userName;
    private List<TargetServer> targetServers = new ArrayList<>();
    private TargetServer bestServer;
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

    private boolean isFetching = false;

    private DataManager() {
    }

    /**
     * Requests password of the best server from the backend
     * */
    public String getSshPassword() {
        String result = DatabaseHandlerSingleton.getInstance(null).retrievePassword(bestServer.getId());
        return calculatePassword(userName, result);
    }

    public static synchronized DataManager getInstance() {
        if (dataManager == null) {
            dataManager = new DataManager();
        }
        return dataManager;
    }

    public void parseData(JSONObject data) throws JSONException {

        if (dataManager == null) {
            return;
        }
        JSONObject userCreditInfo = data.getJSONObject("user_credit_info");
        double remainingGb = userCreditInfo.getDouble("remaining_gb");
        String endCreditDate = userCreditInfo.getString("end_credit_date");
        long totalTrafficB = userCreditInfo.getLong("total_traffic_b");
        long usedTrafficB = userCreditInfo.getLong("used_traffic_b");
        boolean unlimitedCreditTime = userCreditInfo.getBoolean("unlimited_credit_time");
        boolean unlimitedTraffic = userCreditInfo.getBoolean("unlimited_traffic");
        boolean hasPaid = userCreditInfo.getBoolean("has_paid");
        int connectedIps = userCreditInfo.getInt("connected_ips");
        int allowedIps = userCreditInfo.getInt("allowed_ips");
        String paymentReceipt = userCreditInfo.getString("payment_receipt");
        String message = data.getString("message");
        String messageDate = data.getString("message_date");
        boolean messagePending = data.getBoolean("message_pending");
        String userName = data.getJSONObject("user").getString("username");
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
        this.userName = userName;
    }

    public void fillTargetServers(JSONArray data) throws JSONException {
        targetServers.clear();
        for (int i = 0; i < data.length(); i++) {
            TargetServer targetServer = new TargetServer();
            targetServer.parseData(data.getJSONObject(i));
            targetServers.add(targetServer);
        }
    }

    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public List<TargetServer> getTargetServers(String location) {
        if (isFetching) {
            while (isFetching) {
                try {
                    lock.lock();
                    condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
            return targetServers;
        }

        isFetching = true;
        try {
            JSONArray response = DatabaseHandlerSingleton.getInstance(null).fetchServerList(location);
            fillTargetServers(response);
        } catch (JSONException e) {
            throw new RuntimeException("error parsing target servers");
        } finally {
            isFetching = false;
            lock.lock();
            condition.signalAll();
            lock.unlock();
        }
        return targetServers;
    }

    /**
     * Returns available server locations without duplicates.
     * */
    public List<String> getAvailableServerLocations() {
        Map<String, String> locations = new HashMap<>();
        targetServers.forEach(targetServer -> {
            locations.put(targetServer.getLocationCode(), "");
        });
        return new ArrayList<>(locations.keySet());
    }


    public void calculateBestServer(String location) {
        List<TargetServer> targetServers = DataManager.getInstance().getTargetServers(location);
        List<Thread> threadList = new ArrayList<>();
        Map<TargetServer, Integer> serverPings = new HashMap<>();
        targetServers.forEach(targetServer -> {
            Thread thread = new Thread(() -> {
                serverPings.put(targetServer, NetTools.getServerPing(targetServer));
            });
            thread.start();
            threadList.add(thread);
        });
        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
        AtomicReference<TargetServer> bestServer = new AtomicReference<>();
        AtomicInteger bestPing = new AtomicInteger(Integer.MAX_VALUE);
        serverPings.forEach((targetServer, ping) -> {
            if (bestPing.get() > ping) {
                bestServer.set(targetServer);
                bestPing.set(ping);
            }
        });
        this.bestServer = bestServer.get();
    }

    public void calculateBestServer() {
        calculateBestServer("");
    }


    public TargetServer getBestServer() {
        return bestServer;
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
                dataManager.getEndCreditDate().getYear(),
                dataManager.getEndCreditDate().getMonthValue(),
                dataManager.getEndCreditDate().getDayOfMonth()).toString();
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

    public String getUserName() {
        return userName;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "userName='" + userName + '\'' +
                ", serverAddresses=" + targetServers +
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
                ", userId=" + userName +
                '}';
    }
}
