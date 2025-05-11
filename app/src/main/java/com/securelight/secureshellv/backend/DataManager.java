package com.securelight.secureshellv.backend;

import static com.securelight.secureshellv.utility.Utilities.calculatePassword;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import com.github.eloyzone.jalalicalendar.DateConverter;
import com.github.eloyzone.jalalicalendar.JalaliDate;
import com.securelight.secureshellv.utility.NetTools;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

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
import java.util.stream.Collectors;

public class DataManager {
    private static DataManager dataManager;
    private final List<V2rayConfig> v2rayConfigs = new ArrayList<>();
    private String userName;
    private List<TargetServer> targetServers = new ArrayList<>();
    private TargetServer bestServer;
    private double remainingTrafficGB;
    private LocalDateTime endCreditDate;
    private double totalTrafficGB;
    private double usedTrafficGB;
    private boolean unlimitedCreditTime;
    private boolean unlimitedTraffic;
    private boolean renewPending;
    private int connectedIps;
    private int allowedIps;
    private Image paymentReceipt;
    private String message;
    private LocalDateTime messageDate;
    private boolean messagePending;
    private boolean isFetching = false;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private int emptyCount = 0;

    private DataManager() {
    }

    public static synchronized DataManager getInstance() {
        if (dataManager == null) {
            dataManager = new DataManager();
        }
        return dataManager;
    }

    /**
     * Requests password of the best server from the backend
     */
    public String getSshPassword(boolean reset) {
        String result = DatabaseHandlerSingleton.getInstance(null).retrievePassword(bestServer.getId(), reset);
        return calculatePassword(userName, result);
    }

    public void parseData(JSONObject data) throws JSONException {

        if (dataManager == null) {
            return;
        }
        JSONObject userCreditInfo = data.getJSONObject("user_credit_info");
        double remainingGb = userCreditInfo.getDouble("remaining_gb");
        String endCreditDate = userCreditInfo.getString("end_credit_date");
        long totalTrafficB = userCreditInfo.getLong("data_limit_b");
        long usedTrafficB = userCreditInfo.getLong("used_data_b");
        boolean unlimitedCreditTime = userCreditInfo.getBoolean("unlimited_credit_time");
        boolean unlimitedTraffic = userCreditInfo.getBoolean("unlimited_data");
        boolean renewPending = userCreditInfo.getBoolean("renew_pending");
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
        this.renewPending = renewPending;
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

    public void parseTargetServers(JSONArray data) throws JSONException {
        if (data == null) {
            return;
        }
        targetServers.clear();
        for (int i = 0; i < data.length(); i++) {
            TargetServer targetServer = new TargetServer();
            targetServer.parseData(data.getJSONObject(i));
            targetServers.add(targetServer);
        }
    }

    /**
     * Gets servers with regards to the selected location
     */
    public List<TargetServer> getTargetServers() {
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
            JSONArray response = DatabaseHandlerSingleton.getInstance(null)
                    .fetchServerList(SharedPreferencesSingleton.getInstance(null).getSelectedServerLocation());
            if (response.length() == 0) {
                // if nothing was found we fetch servers from all locations
                response = DatabaseHandlerSingleton.getInstance(null).fetchServerList("");
                SharedPreferencesSingleton.getInstance(null).setServerLocation("ato");
            }
            parseTargetServers(response);
        } catch (JSONException e) {
            throw new RuntimeException("error parsing target servers", e);
        } finally {
            isFetching = false;
            lock.lock();
            condition.signalAll();
            lock.unlock();
        }
        return targetServers;
    }

    /**
     * Gets all servers regardless of location
     */
    public List<TargetServer> fetchServerSelection() {
        List<TargetServer> targetServers = new ArrayList<>();
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

            JSONArray response = DatabaseHandlerSingleton.getInstance(null).fetchServerList("");
            parseTargetServers(response);
        } catch (JSONException e) {
            Log.d("DataManager", e.getMessage(), e);
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
     */
    public List<String> getAvailableServerLocations() {
        Map<String, String> locations = new HashMap<>();
        targetServers.forEach(targetServer -> {
            locations.put(targetServer.getLocationCode().toLowerCase(), "");
        });
        return new ArrayList<>(locations.keySet());
    }

    /**
     * Calculate best server and put it in this.bestServer.
     *
     * @return true if best server was found; false otherwise
     */
    public boolean calculateBestServer() {
        List<TargetServer> servers = DataManager.getInstance().getTargetServers();
        while (servers.isEmpty()) {
            emptyCount++;
            servers = DataManager.getInstance().getTargetServers();
            if (emptyCount == 3) {
                return false;
            }
        }

        List<TargetServer> targetServers = servers.stream().filter(server -> server.getType() == TargetServer.Type.M)
                .collect(Collectors.toList());
        List<TargetServer> hopServers = servers.stream().filter(server ->
                        server.getType() == TargetServer.Type.N)
                .collect(Collectors.toList());

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
            if (bestPing.get() > ping && ping != 0) {
                bestServer.set(targetServer);
                bestPing.set(ping);
            }
        });
        // if direct servers were unreachable
        if (bestServer.get() == null) {
            bestServer.set(calculateBestHopServer(hopServers, threadList, serverPings));
        }
        if (bestServer.get() == null) {
            return false;
        }
        this.bestServer = bestServer.get();
        return true;
    }

    private TargetServer calculateBestHopServer(List<TargetServer> hopServers, List<Thread> threadList, Map<TargetServer, Integer> serverPings) {
        threadList.clear();
        serverPings.clear();

        hopServers.forEach(targetServer -> {
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
        AtomicReference<TargetServer> bestHopServer = new AtomicReference<>();
        AtomicInteger bestHopPing = new AtomicInteger(Integer.MAX_VALUE);
        serverPings.forEach((targetServer, ping) -> {
            if (bestHopPing.get() > ping && ping != 0) {
                bestHopServer.set(targetServer);
                bestHopPing.set(ping);
            }
        });
        return bestHopServer.get();
    }

    public List<ServicePlan> getServicePlans() {
        JSONArray response = DatabaseHandlerSingleton.getInstance(null).fetchServicePlans();
        List<ServicePlan> servicePlans = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            ServicePlan servicePlan = new ServicePlan();
            try {
                servicePlan.parseData(response.getJSONObject(i));
            } catch (JSONException ignored) {
            }
            servicePlans.add(servicePlan);
        }
        return servicePlans;
    }

    public List<V2rayConfig> updateV2rayConfigs(String location) throws JSONException {
        getTargetServers();
        JSONArray jsonArray = DatabaseHandlerSingleton.getInstance(null).fetchConfigs(location);
        for (int i = 0; i < jsonArray.length(); i++) {
            V2rayConfig v2rayConfig = new V2rayConfig();
            v2rayConfig.parseData(jsonArray.getJSONObject(i));
            v2rayConfigs.add(v2rayConfig);
        }
        return v2rayConfigs;
    }

    public List<V2rayConfig> getV2rayConfigs() {
        return v2rayConfigs;
    }

    public TargetServer fetchBestServer() {
        calculateBestServer();
        return bestServer;
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

    public long getRemainingDays() {
        return LocalDateTime.now().until(endCreditDate, ChronoUnit.DAYS);
    }

    public long[] getRemainingTime() {
        long[] time = new long[2];
        time[0] = LocalDateTime.now().until(endCreditDate, ChronoUnit.HOURS);
        time[1] = LocalDateTime.now().until(endCreditDate, ChronoUnit.MINUTES) - (time[0] * 60);
        return time;
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

    public boolean isRenewPending() {
        return renewPending;
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

    public void setMessageSeen() {
        messagePending = false;
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
                ", hasPaid=" + renewPending +
                ", connectedIps=" + connectedIps +
                ", allowedIps=" + allowedIps +
                ", paymentReceipt=" + paymentReceipt +
                ", message='" + message + '\'' +
                ", messagePending=" + messagePending +
                ", userId=" + userName +
                '}';
    }
}
