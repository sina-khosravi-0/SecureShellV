package com.securelight.secureshellv;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.backend.UserData;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.statics.Values;
import com.securelight.secureshellv.ui.login.LoginActivity;
import com.securelight.secureshellv.utility.CustomExceptionHandler;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;
import com.securelight.secureshellv.vpnservice.SSVpnService;
import com.securelight.secureshellv.vpnservice.connection.ConnectionState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    public static final String EXIT_APP_ACTION = "com.securelight.secureshellv.EXIT_APP";
    public static final String SIGN_IN_ACTION = "com.securelight.secureshellv.DO_SIGN_IN";
    public static final String VPN_SERVICE_ACTION = "android.net.VpnService";
    public static final String CONNECTION_INFO_PREF = "CONNECTION_INFO";
    public static final String UPDATE_USER_DATA_INTENT = "UPDATE_USER_DATA";
    public static boolean started = false;
    private Intent vpnServiceIntent;
    private LinearLayout bottomSheetLayout;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private FrameLayout buttonFrame;
    private ImageView buttonImage;
    private TextView buttonText;
    private TextView mainConnectText;
    private TextView daysLeftText;
    private CircularProgressIndicator trafficProgressIndicator;

    public static int colorPrimary;
    public static int colorOnPrimary;
    public static int colorSecondary;
    public static int colorOnSecondary;
    public static int colorSecondaryContainer;
    public static int colorOnSecondaryContainer;
    public static int colorTertiary;
    public static int colorOnTertiary;
    public static int colorTertiaryContainer;
    public static int colorOnTertiaryContainer;
    public static int colorOk;
    public static int colorWarning;
    public static int colorAlert;
    private SSVpnService.VpnServiceBinder vpnServiceBinder;
    private final ServiceConnection vpnServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            vpnServiceBinder = (SSVpnService.VpnServiceBinder) binder;
            switch (vpnServiceBinder.getConnectionState()) {
                case CONNECTED:
                    performConnectedAction();
                    break;
                case CONNECTING:
                    performConnectingAction();
                    break;
                case DISCONNECTED:
                    performDisconnectedAction();
                    break;
            }
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
            startVpnService();
        }
    };

    private final BroadcastReceiver exitBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            exitApp();
        }
    };
    private final BroadcastReceiver connectedBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            performConnectedAction();
        }
    };

    private final BroadcastReceiver connectingBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            performConnectingAction();
        }
    };

    private final BroadcastReceiver disconnectedBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            started = true;
            performDisconnectedAction();
        }
    };
    private final BroadcastReceiver signInBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
    };

    private final BroadcastReceiver updateUserDataUIBr = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            UserData userData = UserData.getInstance();
            buttonText.setText(userData.getRemainingTrafficGB() + "\nGB");
            if (vpnServiceBinder != null && vpnServiceBinder.getService().isServiceActive()) {
                trafficProgressIndicator.setProgress(userData.getRemainingPercent(), true);
            }

            if (userData.getDaysLeft() <= 3 && userData.getDaysLeft() > 1) {
                daysLeftText.setTextColor(colorWarning);
            } else if (userData.getDaysLeft() <= 1) {
                daysLeftText.setTextColor(colorAlert);
            } else {
                daysLeftText.setTextColor(colorOk);
            }
            daysLeftText.setText(getResources().getQuantityString(R.plurals.days_left,
                    (int) userData.getDaysLeft(), (int) userData.getDaysLeft()));
        }
    };

    private final BroadcastReceiver insufficientTrafficBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle(R.string.insufficient_traffic)
                    .setMessage(R.string.you_traffic_has_run_out)
                    .setNeutralButton(R.string.ok, null).show();
        }
    };
    private final BroadcastReceiver creditExpiredBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle(R.string.credit_expired)
                    .setMessage(R.string.credit_time_has_run_out)
                    .setNeutralButton(R.string.ok, null).show();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndAddPermissions();
        fetchStringValues();

        // Sets the default uncaught exception handler. This handler is invoked
        // in case any Thread dies due to an unhandled exception.
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler("/storage/",
                this));

        Locale locale = new Locale(SharedPreferencesSingleton
                .getInstance(this)
                .getAppLanguage());

        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale));

        setContentView(R.layout.activity_main);
        setColors();
        // initialize database handler singleton
        DatabaseHandlerSingleton.getInstance(this);

        // set vpn intent
        vpnServiceIntent = new Intent(this, SSVpnService.class).setAction(VPN_SERVICE_ACTION)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setPackage(getPackageName());

        updateUserData();
        initUIComponents();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(startBr, new IntentFilter(SSVpnService.START_VPN_ACTION));
        lbm.registerReceiver(exitBr, new IntentFilter(EXIT_APP_ACTION));
        lbm.registerReceiver(connectedBr, new IntentFilter(SSVpnService.CONNECTED_ACTION));
        lbm.registerReceiver(connectingBr, new IntentFilter(SSVpnService.CONNECTING_ACTION));
        lbm.registerReceiver(disconnectedBr, new IntentFilter(SSVpnService.DISCONNECTED_ACTION));
        lbm.registerReceiver(signInBr, new IntentFilter(SIGN_IN_ACTION));
        lbm.registerReceiver(updateUserDataUIBr, new IntentFilter(UPDATE_USER_DATA_INTENT));
        lbm.registerReceiver(insufficientTrafficBr, new IntentFilter(Intents.INSUFFICIENT_TRAFFIC_INTENT));
        lbm.registerReceiver(creditExpiredBr, new IntentFilter(Intents.CREDIT_EXPIRED_INTENT));

    }

    private void initUIComponents() {
        buttonFrame = findViewById(R.id.vpn_toggle_frame);
        buttonImage = buttonFrame.findViewById(R.id.vpn_toggle_img);
        buttonText = buttonFrame.findViewById(R.id.vpn_toggle_txt);
        trafficProgressIndicator = buttonFrame.findViewById(R.id.traffic_progress);
        mainConnectText = findViewById(R.id.main_connect_status);
        daysLeftText = findViewById(R.id.days_left);
        buttonFrame.setOnClickListener(v -> {
            updateUserData();
            if (vpnServiceBinder.getConnectionState().equals(ConnectionState.DISCONNECTED)) {
                startVpnService();
            } else {
                stopVpnService();
            }
        });

        bottomSheetLayout = findViewById(R.id.standard_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        CoordinatorLayout rootLayout = (CoordinatorLayout) bottomSheetLayout.getParent();

        int NUMBER_OF_TABS = 5;
        TabLayout tabLayout = findViewById(R.id.bottom_sheet_menu_tab_layout);
        ViewPager2 viewPager = findViewById(R.id.bottom_sheet_view_pager);
        BottomSheetTabAdapter tabAdapter = new BottomSheetTabAdapter(
                getSupportFragmentManager(), getLifecycle(), NUMBER_OF_TABS);
        viewPager.setAdapter(tabAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheetBehavior.setDraggable(false);
                } else {
                    if (tab.getPosition() == 0) {
                        updateUserData();
                    }
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    bottomSheetBehavior.setDraggable(true);
                }

                View view = tab.getCustomView();
                TextView textView = view.findViewById(R.id.text_view);
                ImageView imageView = view.findViewById(R.id.image_view);
                ViewPropertyAnimator textAnimator = textView.animate();
                float offset = (textView.getHeight() == 0 ? 20 : textView.getHeight());
                textAnimator.translationY(-offset / 3f).setDuration(300);

                ViewPropertyAnimator imageAnimator = imageView.animate();
                imageAnimator.translationY(-offset / 2f).setDuration(300);
                imageAnimator.scaleX(1.5f).setDuration(300);
                imageAnimator.scaleY(1.5f).setDuration(300);

                Integer colorFrom = colorOnSecondaryContainer;
                Integer colorTo = colorPrimary;

                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                colorAnimation.addUpdateListener(animator -> {
                    imageView.setColorFilter((Integer) animator.getAnimatedValue());
                });
                colorAnimation.start();

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View view = tab.getCustomView();
                TextView textView = view.findViewById(R.id.text_view);
                ImageView imageView = view.findViewById(R.id.image_view);

                ViewPropertyAnimator textAnimator = textView.animate();
                textAnimator.translationY(0);
                textAnimator.scaleX(1).setDuration(300);
                textAnimator.scaleY(1).setDuration(300);

                ViewPropertyAnimator imageAnimator = imageView.animate();
                imageAnimator.translationY(0).setDuration(300);
                imageAnimator.scaleX(1).setDuration(300);
                imageAnimator.scaleY(1).setDuration(300);

                Integer colorFrom = colorPrimary;
                Integer colorTo = colorOnSecondaryContainer;
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                colorAnimation.addUpdateListener(animator ->
                        imageView.setColorFilter((Integer) animator.getAnimatedValue()));
                colorAnimation.start();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            ConstraintLayout view = (ConstraintLayout) getLayoutInflater().inflate(R.layout.bottom_sheet_tab, null);
            ImageView imageView = view.findViewById(R.id.image_view);
            TextView textView = view.findViewById(R.id.text_view);
            textView.setTextColor(colorOnSecondaryContainer);
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(colorOnSecondaryContainer));

            switch (position) {
                case 0:
                    imageView.setImageResource(R.drawable.person_icon);
                    textView.setText(R.string.bottom_sheet_account);
                    tab.setCustomView(view);
                    break;
                case 1:
                    imageView.setImageResource(R.drawable.server_icon);
                    textView.setText(R.string.bottom_sheet_server);
                    tab.setCustomView(view);
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.ic_home_black_24dp);
                    textView.setText(R.string.bottom_sheet_home);
                    tab.setCustomView(view);

                    break;
                case 3:
                    imageView.setImageResource(R.drawable.settings_icon);
                    textView.setText(R.string.bottom_sheet_settings);
                    tab.setCustomView(view);
                    break;
                case 4:
                    imageView.setImageResource(R.drawable.more_icon);
                    textView.setText(R.string.bottom_sheet_more);
                    tab.setCustomView(view);
                    break;
                default:
                    break;
            }
        }).attach();
        tabLayout.getTabAt(2).select();

        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(NUMBER_OF_TABS);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    tabLayout.getTabAt(2).select();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

//                float progress = Math.abs(slideOffset); // Get the absolute slide offset value
//
//                // Calculate the interpolated progress for the animation
//                float interpolatedProgress = Math.min(0.5f, progress);
//                System.out.println(slideOffset);
//                // Apply the interpolated progress to the views
//                constraintLayout.setScaleX(interpolatedProgress + 1);
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
//        executorService.execute(() -> {
//            System.out.println(DatabaseHandlerSingleton.getInstance(this).
//                    verifyToken(accessToken));
//        });
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

    public void startVpnService() {
        Intent intent = SSVpnService.prepare(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    static long bytes = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileRxBytes();

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            startService(vpnServiceIntent);
        }
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                runOnUiThread(() -> {
//                    buttonText.setText(String.format("%.3f\n%.3f", getBytes() / (1000000000d),
//                            (TrafficStats.getMobileRxBytes() + TrafficStats.getMobileRxBytes() - bytes) / 1000000000d));
//                });
//                System.out.println(getBytes());
//            }
//        }).start();
        super.onActivityResult(request, result, data);
    }

    public void stopVpnService() {
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
        updateUserData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vpnServiceBinder != null) {
            unbindService(vpnServiceConnection);
        }
    }


    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private void performConnectedAction() {
        buttonImage.setVisibility(View.GONE);
        buttonText.setVisibility(View.VISIBLE);
        mainConnectText.setText(R.string.connected);
        trafficProgressIndicator.setIndeterminate(false);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                UserData userData = UserData.getInstance();
                double value = userData.getRemainingTrafficGB() / userData.getTotalTrafficGB() *
                        interpolatedTime * 100;
                double traffic = 0 + userData.getRemainingTrafficGB() * interpolatedTime;
                buttonText.setText(String.format("%.2f", traffic) + "\nGB");
                trafficProgressIndicator.setProgress((int) value);
            }

        };
        animation.setDuration(1000);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());

        trafficProgressIndicator.startAnimation(animation);
    }

    private void performConnectingAction() {
        buttonImage.setImageResource(R.drawable.vpn_loading_animated);
        buttonImage.setVisibility(View.VISIBLE);
        buttonText.setVisibility(View.GONE);
        mainConnectText.setText(R.string.connecting);
        trafficProgressIndicator.setIndeterminate(true);

        AnimatedVectorDrawable vectorDrawable = (AnimatedVectorDrawable) buttonImage.getDrawable();
        vectorDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                vectorDrawable.start();
            }
        });
        vectorDrawable.start();
    }


    private void performDisconnectedAction() {
        buttonImage.setImageResource(R.drawable.vpn_toggle_vector);
        buttonImage.setVisibility(View.VISIBLE);
        buttonText.setVisibility(View.GONE);
        mainConnectText.setText(R.string.disconnected);
        trafficProgressIndicator.setIndeterminate(false);

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                UserData userData = UserData.getInstance();
                double value = (userData.getRemainingTrafficGB() / userData.getTotalTrafficGB() -
                        userData.getRemainingTrafficGB() / userData.getTotalTrafficGB() *
                                interpolatedTime) * 100;

                double traffic = userData.getRemainingTrafficGB() -
                        userData.getRemainingTrafficGB() * interpolatedTime;
                buttonText.setText(String.format("%.2f", traffic) + "\nGB");
                trafficProgressIndicator.setProgress((int) value);

            }

        };
        animation.setDuration(1000);
        animation.setInterpolator(new DecelerateInterpolator());
        if (started) {
            trafficProgressIndicator.startAnimation(animation);
        }
    }

    private void updateUserData() {
        new Thread(() -> {
            DatabaseHandlerSingleton.getInstance(this).fetchUserData();
        }).start();

    }

    private void setColors() {
        TypedValue typedValue = new TypedValue();

        getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorOnPrimary, typedValue, true);
        colorOnPrimary = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorSecondaryContainer, typedValue, true);
        colorSecondaryContainer = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true);
        colorOnSecondaryContainer = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorTertiary, typedValue, true);
        colorTertiary = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorOnTertiary, typedValue, true);
        colorOnTertiary = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorTertiaryContainer, typedValue, true);
        colorTertiaryContainer = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorOnTertiaryContainer, typedValue, true);
        colorOnTertiaryContainer = typedValue.data;
        getTheme().resolveAttribute(R.attr.ok, typedValue, true);
        colorOk = typedValue.data;
        getTheme().resolveAttribute(R.attr.warning, typedValue, true);
        colorWarning = typedValue.data;
        getTheme().resolveAttribute(R.attr.alert, typedValue, true);
        colorAlert = typedValue.data;
    }

    public static boolean isAppClosing() {
        return appClosing;
    }

}