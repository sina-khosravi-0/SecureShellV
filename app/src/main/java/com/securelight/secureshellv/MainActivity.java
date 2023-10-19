package com.securelight.secureshellv;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.statics.Values;
import com.securelight.secureshellv.ui.login.LoginActivity;
import com.securelight.secureshellv.utility.CustomExceptionHandler;
import com.securelight.secureshellv.vpnservice.SSVpnService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    public static final String EXIT_APP_ACTION = "com.securelight.secureshellv.EXIT_APP";
    public static final String SIGN_IN_ACTION = "com.securelight.secureshellv.DO_SIGN_IN";
    Intent vpnServiceIntent;
    private ParcelFileDescriptor vpnInterface;
    static final String VPN_SERVICE_ACTION = "android.net.VpnService";
    private SSVpnService.VpnServiceBinder vpnServiceBinder;
    private final ServiceConnection vpnServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            vpnServiceBinder = (SSVpnService.VpnServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            vpnServiceBinder = null;
        }
    };

    private static boolean appClosing = false;
    private final BroadcastReceiver startBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onStartClicked(null);
        }
    };

    private final BroadcastReceiver exitBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            exitApp();
        }
    };
    private final BroadcastReceiver signInBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sets the default uncaught exception handler. This handler is invoked
        // in case any Thread dies due to an unhandled exception.
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler("/storage/",
                this));

        System.out.println((String) null);

        setContentView(R.layout.activity_main);
        // initialize database handler singleton
        DatabaseHandlerSingleton.getInstance(this);

        // set vpn intent
        vpnServiceIntent = new Intent(this, SSVpnService.class).setAction(VPN_SERVICE_ACTION)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setPackage(getPackageName());

        fetchStringValues();

        checkAndAddPermissions();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(startBr, new IntentFilter(SSVpnService.START_VPN_ACTION));
        lbm.registerReceiver(exitBr, new IntentFilter(EXIT_APP_ACTION));
        lbm.registerReceiver(signInBr, new IntentFilter(SIGN_IN_ACTION));

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

    private void fetchStringValues() {
        Values.CONNECTED_STRING = getString(R.string.connected);
        Values.DISCONNECTED_STRING = getString(R.string.disconnected);
        Values.CONNECTING_STRING = getString(R.string.connecting);
        Values.FULL_INTERNET_ACCESS_STRING = getString(R.string.full_internet_access);
        Values.RESTRICTED_INTERNET_ACCESS_STRING = getString(R.string.restricted_internet_access);
        Values.NO_INTERNET_ACCESS_STRING = getString(R.string.no_internet_access);
        Values.NETWORK_UNAVAILABLE_STRING = getString(R.string.network_unavailable);
        Values.INTERNET_ACCESS_STATE_STRING = getString(R.string.internet_access_state);
        Values.CANNOT_ACCESS_SERVER_STRING = getString(R.string.cannot_access_server);
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
        Intent intent = SSVpnService.prepare(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            startService(vpnServiceIntent);
        }
        super.onActivityResult(request, result, data);
    }

    public void onStopClicked(View view) {
        if (vpnServiceBinder != null) {
            vpnServiceBinder.getService().stopVpnService();
            stopService(vpnServiceIntent);
        }
    }


    /**
     * REMOVE
     * only for debugging.
     */
    public void onDestroyClicked(View view) {
        exitApp();
    }

    // TODO: implement app exit sequence
    private void exitApp() {
        appClosing = true;
        vpnServiceBinder.stopService();
        finishAndRemoveTask();
//        finishActivity(0);
//        finish();
//        finishAffinity();
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
        vpnServiceBinder.getService().no();
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        executorService.execute(DatabaseHandlerSingleton::fetchUserData);
//        executorService.execute(DatabaseHandlerSingleton::doRefreshToken);
//        DatabaseHandlerSingleton.doRefreshToken();
    }

    //todo: implement on low memory
    @Override
    protected void onResume() {
        super.onResume();
        bindService(vpnServiceIntent, vpnServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vpnServiceBinder != null) {
            unbindService(vpnServiceConnection);
        }
    }

    public static boolean isAppClosing() {
        return appClosing;
    }

}