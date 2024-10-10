package com.securelight.secureshellv.ui.homepage;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
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
import android.widget.Toast;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.securelight.secureshellv.BottomSheetTabAdapter;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.statics.Values;
import com.securelight.secureshellv.ui.login.LoginActivity;
import com.securelight.secureshellv.utility.CustomExceptionHandler;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;
import com.securelight.secureshellv.vpnservice.SSVpnService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import dev.dev7.lib.v2ray.V2rayController;

public class HomepageActivity extends AppCompatActivity {
    public static boolean isTrafficProgressBarAnimated = false;
    public static int colorPrimary;
    public static int colorOnPrimary;
    /**
     * @noinspection unused
     */
    public static int colorSecondary;
    /**
     * @noinspection unused
     */
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
    private static boolean appClosing = false;
    private final BroadcastReceiver signInBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
    };
    private final BroadcastReceiver insufficientTrafficBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new MaterialAlertDialogBuilder(HomepageActivity.this)
                    .setTitle(R.string.insufficient_data)
                    .setMessage(R.string.data_limit_reached)
                    .setNeutralButton(R.string.ok, null).show();
        }
    };
    private final BroadcastReceiver creditExpiredBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new MaterialAlertDialogBuilder(HomepageActivity.this)
                    .setTitle(R.string.credit_expired)
                    .setMessage(R.string.credit_time_has_run_out)
                    .setNeutralButton(R.string.ok, null).show();
        }
    };
    private final BroadcastReceiver killActivityBr = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    private final BroadcastReceiver sendStatsFailBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new MaterialAlertDialogBuilder(HomepageActivity.this)
                    .setTitle(R.string.fail_to_connect_to_server)
                    .setMessage(R.string.couldn_t_send_stats_to_server)
                    .setNeutralButton(R.string.ok, null).show();
            stopVpnService();
        }
    };
    private Intent vpnServiceIntent;
    private final BroadcastReceiver startBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startVpnService();
        }
    };
    private CoordinatorLayout rootLayout;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ImageView buttonImage;
    private TextView buttonText;
    private TextView mainConnectText;
    private TextView remainingTimeText;
    private MaterialButton resubscribeButton;
    private CircularProgressIndicator trafficProgressIndicator;
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
            isTrafficProgressBarAnimated = true;
            performDisconnectedAction();
        }
    };
    private SSVpnService.VpnServiceBinder vpnServiceBinder;
    private final BroadcastReceiver startServiceFailedBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopVpnService();
            performDisconnectedAction();
            Toast.makeText(getApplicationContext(), "failed to connect to server", Toast.LENGTH_SHORT).show();
        }
    };
    private final BroadcastReceiver stopBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            vpnServiceBinder.getService().stopVpnService();
            performDisconnectedAction();
            if (intent.getBooleanExtra(Constants.OUT_OF_TRAFFIC_CODE_STRING, false)) {
                new MaterialAlertDialogBuilder(HomepageActivity.this)
                        .setTitle(R.string.insufficient_data)
                        .setMessage(R.string.data_limit_reached)
                        .setNeutralButton(R.string.ok, null).show();
            }
            if (intent.getBooleanExtra(Constants.CREDIT_EXPIRED_CODE_STRING, false)) {
                new MaterialAlertDialogBuilder(HomepageActivity.this)
                        .setTitle(R.string.credit_expired)
                        .setMessage(R.string.credit_time_has_run_out)
                        .setNeutralButton(R.string.ok, null).show();
            }
            Log.i("MainActivity", "VPN service stopped");
        }
    };
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
    private final BroadcastReceiver exitBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            exitApp();
        }
    };
    private final BroadcastReceiver updateUserDataUIBr = new BroadcastReceiver() {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void onReceive(Context context, Intent intent) {
            DataManager dataManager = DataManager.getInstance();
            buttonText.setText(String.format("%.2f\nGB", dataManager.getRemainingTrafficGB()));
            if (vpnServiceBinder != null && vpnServiceBinder.getService().isServiceActive()) {
                trafficProgressIndicator.setProgress(dataManager.getRemainingPercent(), true);
            }
            if (dataManager.isUnlimitedCreditTime()) {
                remainingTimeText.setTextColor(colorOk);
                remainingTimeText.setText(R.string.unlimited_time);
            } else {
                long remainingDays = dataManager.getRemainingDays();
                if (remainingDays <= 3 && remainingDays > 1) {
                    remainingTimeText.setTextColor(colorWarning);
                } else if (remainingDays <= 1) {
                    remainingTimeText.setTextColor(colorAlert);
                    resubscribeButton.setVisibility(View.VISIBLE);
                } else {
                    remainingTimeText.setTextColor(colorOk);
                    resubscribeButton.setVisibility(View.GONE);
                }
                if (remainingDays == 0) {
                    long[] timeLeft = dataManager.getRemainingTime();
                    remainingTimeText.setText(getResources().getQuantityString(R.plurals.hours,
                            (int) timeLeft[0], (int) timeLeft[0]) + " " + getResources().getQuantityString(R.plurals.minutes,
                            (int) timeLeft[1], (int) timeLeft[1]) + " " + getResources().getString(R.string.left));
                } else {
                    remainingTimeText.setText(getResources().getQuantityString(R.plurals.days_left,
                            (int) remainingDays, (int) remainingDays));
                }
            }
        }
    };

    public static boolean isAppClosing() {
        return appClosing;
    }

    /**
     * @noinspection CommentedOutCode
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        V2rayController.init(this, R.drawable.ic_launcher_foreground, "V2ray Android");
//        SpeedTestSocket speedTestSocket = new SpeedTestSocket();
//
//// add a listener to wait for speedtest completion and progress
//        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
//
//            @Override
//            public void onCompletion(SpeedTestReport report) {
//                // called when download/upload is complete
//                System.out.println("[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
//                System.out.println("[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
//            }
//
//            @Override
//            public void onError(SpeedTestError speedTestError, String errorMessage) {
//                // called when a download/upload error occur
//                System.out.println(speedTestError);
//                System.out.println(errorMessage);
//            }
//
//            @Override
//            public void onProgress(float percent, SpeedTestReport report) {
//                // called to notify download/upload progress
//                System.out.println("[PROGRESS] progress : " + percent + "%");
//                System.out.println("[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                System.out.println("[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
//            }
//        });
//
//        new Thread(() -> {
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException ignored) {
//            }
//            speedTestSocket.startDownload("http://speedtest.etisalat.af:8080/speedtest/random2000x2000.JPG", 500);
//        }).start();
//

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

        initColors();
        // initialize database handler singleton
        DatabaseHandlerSingleton.getInstance(this);
        SharedPreferencesSingleton.getInstance(this);
        // set vpn intent
        vpnServiceIntent = new Intent(this, SSVpnService.class).setAction(Intents.VPN_SERVICE_ACTION)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setPackage(getPackageName());

        updateUserData();
        initUIComponents();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(startBr, new IntentFilter(Intents.START_VPN_SERVICE_ACTION));
        lbm.registerReceiver(stopBr, new IntentFilter(Intents.STOP_VPN_SERVICE_ACTION));
        lbm.registerReceiver(startServiceFailedBr, new IntentFilter(Intents.START_SERVICE_FAILED_ACTION));
        lbm.registerReceiver(exitBr, new IntentFilter(Intents.EXIT_APP_ACTION));
        lbm.registerReceiver(connectedBr, new IntentFilter(Intents.CONNECTED_ACTION));
        lbm.registerReceiver(connectingBr, new IntentFilter(Intents.CONNECTING_ACTION));
        lbm.registerReceiver(disconnectedBr, new IntentFilter(Intents.DISCONNECTED_ACTION));
        lbm.registerReceiver(signInBr, new IntentFilter(Intents.SIGN_IN_ACTION));
        lbm.registerReceiver(updateUserDataUIBr, new IntentFilter(Intents.UPDATE_USER_DATA_INTENT));
        lbm.registerReceiver(insufficientTrafficBr, new IntentFilter(Intents.INSUFFICIENT_TRAFFIC_INTENT));
        lbm.registerReceiver(creditExpiredBr, new IntentFilter(Intents.CREDIT_EXPIRED_INTENT));
        lbm.registerReceiver(killActivityBr, new IntentFilter(Intents.KILL_HOMEPAGE_ACTIVITY_INTENT));
        lbm.registerReceiver(sendStatsFailBr, new IntentFilter(Intents.SEND_STATS_FAIL_INTENT));
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    lbm.unregisterReceiver(startBr);
                    lbm.unregisterReceiver(stopBr);
                    lbm.unregisterReceiver(startServiceFailedBr);
                    lbm.unregisterReceiver(exitBr);
                    lbm.unregisterReceiver(connectedBr);
                    lbm.unregisterReceiver(connectingBr);
                    lbm.unregisterReceiver(disconnectedBr);
                    lbm.unregisterReceiver(signInBr);
                    lbm.unregisterReceiver(updateUserDataUIBr);
                    lbm.unregisterReceiver(insufficientTrafficBr);
                    lbm.unregisterReceiver(creditExpiredBr);
                    lbm.unregisterReceiver(killActivityBr);
                    lbm.unregisterReceiver(sendStatsFailBr);
                }));
    }

    private void initUIComponents() {
        rootLayout = findViewById(R.id.root);
        FrameLayout startButtonFrame = findViewById(R.id.vpn_toggle_frame);
        buttonImage = startButtonFrame.findViewById(R.id.vpn_toggle_img);
        buttonText = startButtonFrame.findViewById(R.id.vpn_toggle_txt);
        trafficProgressIndicator = startButtonFrame.findViewById(R.id.traffic_progress);
        mainConnectText = findViewById(R.id.main_connect_status);
        remainingTimeText = findViewById(R.id.remaining_time);
        resubscribeButton = findViewById(R.id.main_screen_renew_button);

        startButtonFrame.setOnClickListener(v -> {
            updateUserData();
            if (!vpnServiceBinder.getService().isServiceActive()) {
                startVpnService();
            } else {
                stopVpnService();
            }
        });

        resubscribeButton.setOnClickListener(v -> {
//            startActivity(new Intent(getApplicationContext(), ResubscribeServiceActivity.class));
        });

        LinearLayout bottomSheetLayout = findViewById(R.id.standard_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

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
                assert view != null;
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
                assert view != null;
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
        Objects.requireNonNull(tabLayout.getTabAt(2)).select();

        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(NUMBER_OF_TABS);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    Objects.requireNonNull(tabLayout.getTabAt(2)).select();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

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

    /**
     * @noinspection deprecation
     */
    public void startVpnService() {
        Intent intent = VpnService.prepare(HomepageActivity.this);
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

    public void stopVpnService() {
        if (vpnServiceBinder != null) {
            vpnServiceBinder.getService().stopVpnService();
            stopService(vpnServiceIntent);
        }
    }

    // TODO: implement app exit sequence
    private void exitApp() {
        appClosing = true;
        vpnServiceBinder.stopService();
        finishAndRemoveTask();
        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
        System.exit(0);
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
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                DataManager dataManager = DataManager.getInstance();
                double value = dataManager.getRemainingTrafficGB() / dataManager.getTotalTrafficGB() *
                        interpolatedTime * 100;
                double traffic = 0 + dataManager.getRemainingTrafficGB() * interpolatedTime;
                buttonText.setText(String.format("%.2f", traffic) + "\nGB");
                trafficProgressIndicator.setProgress((int) value);
            }

        };
        animation.setDuration(1000);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());

        trafficProgressIndicator.startAnimation(animation);
        isTrafficProgressBarAnimated = true;
    }

    private void performConnectingAction() {
        buttonImage.setImageResource(R.drawable.vpn_loading_animated);
        buttonImage.setVisibility(View.VISIBLE);
        buttonText.setVisibility(View.GONE);
        mainConnectText.setText(R.string.connecting);
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            runOnUiThread(() -> {
                AnimatedVectorDrawable vectorDrawable = (AnimatedVectorDrawable) buttonImage.getDrawable();
                vectorDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        vectorDrawable.start();
                    }
                });
                vectorDrawable.start();
            });
        }).start();
        trafficProgressIndicator.setIndeterminate(true);
        isTrafficProgressBarAnimated = true;
    }

    private void performDisconnectedAction() {
        buttonImage.setImageResource(R.drawable.vpn_toggle_vector);
        buttonImage.setVisibility(View.VISIBLE);
        buttonText.setVisibility(View.GONE);
        mainConnectText.setText(R.string.disconnected);
        trafficProgressIndicator.setIndeterminate(false);

        Animation animation = new Animation() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                DataManager dataManager = DataManager.getInstance();
                double value = (dataManager.getRemainingTrafficGB() / dataManager.getTotalTrafficGB() -
                        dataManager.getRemainingTrafficGB() / dataManager.getTotalTrafficGB() *
                                interpolatedTime) * 100;

                double traffic = dataManager.getRemainingTrafficGB() -
                        dataManager.getRemainingTrafficGB() * interpolatedTime;
                buttonText.setText(String.format("%.2f", traffic) + "\nGB");
                trafficProgressIndicator.setProgress((int) value);
            }
        };
        animation.setDuration(1000);
        animation.setInterpolator(new DecelerateInterpolator());
        if (isTrafficProgressBarAnimated) {
            trafficProgressIndicator.startAnimation(animation);
        }
    }

    private void updateUserData() {

        new Thread(() -> {
            DatabaseHandlerSingleton.getInstance(this).fetchUserData();
        }).start();

    }

    private void initColors() {
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
}
