package com.securelight.secureshellv;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.ui.login.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    public static final String EXIT_APP_BR = "com.securelight.secureshellv.EXIT_APP";
    public static final String DO_SIGN_IN_BR = "com.securelight.secureshellv.DO_SIGN_IN";
    private static SSVpnService ssVpnService;

    private static boolean appClosing = false;
    private final MyBroadcastReceiver startBr = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startVpn();
        }
    };

    private final MyBroadcastReceiver exitBr = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("FUCK FROM EXIT APP");
            exitApp();
        }
    };
    private final MyBroadcastReceiver signInBr = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize database handler singleton
        DatabaseHandlerSingleton.getInstance(this);


        Values.CONNECTED_STRING = getString(R.string.connected);
        Values.DISCONNECTED_STRING = getString(R.string.disconnected);
        Values.CONNECTING_STRING = getString(R.string.connecting);
        Values.FULL_INTERNET_ACCESS_STRING = getString(R.string.full_internet_access);
        Values.RESTRICTED_INTERNET_ACCESS_STRING = getString(R.string.restricted_internet_access);
        Values.NO_INTERNET_ACCESS_STRING = getString(R.string.no_internet_access);
        Values.NETWORK_UNAVAILABLE_STRING = getString(R.string.network_unavailable);
        Values.INTERNET_ACCESS_STATE_STRING = getString(R.string.internet_access_state);
        Values.CANNOT_ACCESS_SERVER_STRING = getString(R.string.cannot_access_server);

        checkAndAddPermissions();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(startBr, new IntentFilter(SSVpnService.VPN_SERVICE_START_BR));
        lbm.registerReceiver(exitBr, new IntentFilter(EXIT_APP_BR));
        lbm.registerReceiver(signInBr, new IntentFilter(DO_SIGN_IN_BR));

        // TODO: fuck after database
        ((RadioGroup) findViewById(R.id.protocolGroup)).setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.directSshProtocol) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("direct__"));
            } else if (checkedId == R.id.tLSSshProtocol) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("tls__"));
            } else if (checkedId == R.id.dualSshProtocol) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("dual__"));
            }

        });
    }

    private void checkAndAddPermissions() {
        List<String> requiredPermissions = new ArrayList<>(Arrays.asList(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.VIBRATE));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> missingPermissions = new ArrayList<>();
        requiredPermissions.forEach(perm -> {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        });
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, missingPermissions/*requiredPermissions*/.toArray(new String[0]), 0);
        }
    }

    public void onCheckClicked(View view) {
        SharedPreferences preferences = getSharedPreferences(Constants.API_CACHE_PREFERENCES_NAME, Activity.MODE_PRIVATE);
        String accessToken = preferences.getString("access", "");
        String refreshToken = preferences.getString("refresh", "");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            System.out.println(DatabaseHandlerSingleton.verifyToken(accessToken));
        });
//        System.out.println(DatabaseHandlerSingleton.verifyToken(refreshToken));

//        Toast myToast = Toast.makeText(this, "connected", Toast.LENGTH_SHORT);
//        System.out.println(getPreferences(MODE_PRIVATE).getString("ConnectProtocol", "N/A"));
//        String urlString = "https://checkip.amazonaws.com/";
//        URL url = new URL(urlString);
//        new Thread(() -> {
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
//                myToast.setText(br.readLine());
//                myToast.show();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
    }

    public void onStartClicked(View view) {
        // ssVpnService being null means it's not started since its instance is passed when it's started
        if (ssVpnService == null) {
            startVpn();
        }

    }

    private void startVpn() {
        Intent intent = VpnService.prepare(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    public void onStopClicked(View view) {
        if (ssVpnService != null && ssVpnService.isServiceActive()) {
            try {
                System.out.println("FUCK from activity");
                ssVpnService.stopVpnService();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Intent intent = getServiceIntent();
            startService(intent);
        }
        super.onActivityResult(request, result, data);
    }


    private Intent getServiceIntent() {
        return new Intent(this, SSVpnService.class).setAction(Intent.ACTION_VIEW)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setPackage(getPackageName());
    }

    /**
     * REMOVE
     * only for debugging.
     */
    public void onDestroyClicked(View view) {
        System.out.println("FUCK FROM DESTROY CLICKED");
        exitApp();
    }

    // todo: implement app exit sequence
    private void exitApp() {
        appClosing = true;
        try {
            if (ssVpnService != null) {
                ssVpnService.finalizeAndStop();
            }
        } catch (IOException ignored) {
        }
        finishAndRemoveTask();
        finishActivity(0);
        finish();
        finishAffinity();
        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
        System.exit(0);
    }

    public void onYesClicked(View view) {
//        ssVpnService.yes();
//        SharedPreferences preferences = this.getSharedPreferences(Constants.API_CACHE_PREFERENCES_NAME, Activity.MODE_PRIVATE);
//        String accessToken = preferences.getString("refresh", "N/A");
//        DatabaseHandlerSingleton instance = DatabaseHandlerSingleton.getInstance(this);
//
//        String url = "http://192.168.19.71:8000/api/token/verify/";
//        JSONObject object;
//        object = new JSONObject();
//        try {
//            object.put("token", accessToken);
//        } catch (JSONException ignored) {
//        }
//        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest
//                (Request.Method.POST, url, object, aResponse -> {
//                    System.out.println(aResponse.toString());
//                }, error -> {
//                    System.out.println("error.toString() = " + error.toString());
//                }) {
//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> params = new HashMap<>();
//                params.put("Authorization", "Bearer " + accessToken);
//                return params;
//            }
//        };
//        instance.addToRequestQueue(jsonArrayRequest);
//        startActivity(new Intent(getApplicationContext(), LoginActivity.class));

    }

    public void onNoClicked(View view) {
//        ssVpnService.no();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(DatabaseHandlerSingleton::fetchUserData);
//        executorService.execute(DatabaseHandlerSingleton::doRefreshToken);
//        DatabaseHandlerSingleton.doRefreshToken();

    }

    //todo: implement on low memory
    @Override
    protected void onResume() {
        super.onResume();
    }

    public static void setSsVpnService(SSVpnService ssVpnService) {
        MainActivity.ssVpnService = ssVpnService;
    }

    public static boolean isAppClosing() {
        return appClosing;
    }

}