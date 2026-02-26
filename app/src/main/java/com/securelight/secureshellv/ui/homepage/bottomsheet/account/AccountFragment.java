package com.securelight.secureshellv.ui.homepage.bottomsheet.account;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.securelight.secureshellv.backend.ServicePlan;
import com.securelight.secureshellv.resubscribe.CheckoutActivity;
import com.securelight.secureshellv.resubscribe.SelectServiceActivity;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.utility.Utilities;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AccountFragment extends Fragment {
    private final String TAG = AccountFragment.class.getSimpleName();
    private TextView username;
    private TextView currentServiceTime;
    private TextView currentServiceGig;
    private TextView currentServicePrice;
    private TextView endCreditDate;
    private TextView totalTraffic;
    private final BroadcastReceiver updateUserDataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataManager dataManager = DataManager.getInstance();
            List<ServicePlan> servicePlans = dataManager.getServicePlans();
            try {
                username.setText(String.valueOf(dataManager.getUserName()));
                ServicePlan servicePlan = null;
                try {
                    servicePlan = servicePlans.stream().filter(plan -> plan.getId() == dataManager.getCurrentPlanId()).collect(Collectors.toList()).get(0);
                } catch (NullPointerException ignored) {
                }
                if (servicePlan != null) {
                    currentServiceTime.setText(context.getResources().getQuantityString(
                            R.plurals.months, servicePlan.getMonths(), servicePlan.getMonths()));
                    currentServicePrice.setText(String.format(Locale.getDefault(), "%,d%s", servicePlan.getPrice(), getString(R.string.toman)));
                    if (dataManager.isUnlimitedTraffic()) {
                        currentServiceGig.setText(context.getResources().getQuantityString(R.plurals.users,
                                servicePlan.getUsers(), servicePlan.getUsers()));
                    } else {
                        currentServiceGig.setText(String.format(Locale.getDefault(), "%d %s", servicePlan.getTraffic(), context.getString(R.string.gigs)));
                    }
                }
                if (dataManager.isUnlimitedCreditTime()) {
                    endCreditDate.setVisibility(View.GONE);
                } else {
                    int[] parts = Utilities.parseIranianDate(dataManager.getJalaliEndCreditDate());
                    endCreditDate.setText(String.format(Locale.getDefault(), "%d / %d / %d", parts[0], parts[1], parts[2]));
                }
//                unlimitedTime.setChecked(dataManager.isUnlimitedCreditTime());
//                unlimitedTraffic.setChecked(dataManager.isUnlimitedTraffic());
                if (dataManager.isUnlimitedTraffic()) {
                    totalTraffic.setText("∞");
                } else {
                    DecimalFormat df = new DecimalFormat("#.##");
                    df.setMinimumFractionDigits(0);
                    df.setMaximumFractionDigits(2);
                    totalTraffic.setText(String.format(Locale.getDefault(), "%s %s",
                            df.format(dataManager.getTotalTrafficGB()),
                            getString(R.string.gigs)));
                }
//                Objects.requireNonNull(connectedIps.getEditText()).setText(String.valueOf(dataManager.getConnectedIps()));
//                Objects.requireNonNull(serverMessage.getEditText()).setText(String.valueOf(dataManager.getMessage()));
//                serverMessage.setHelperText(String.valueOf(dataManager.getMessageDateTimeString()));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    };
    private MaterialButton resubscribeCurrentButton;
    private LinearProgressIndicator trafficProgressIndicator;
    private TextView usedTraffic;

    /**
     * @noinspection FieldCanBeLocal
     */

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LocalBroadcastManager.getInstance(requireActivity())
                .registerReceiver(updateUserDataBroadcastReceiver,
                        new IntentFilter(Intents.UPDATE_USER_DATA_INTENT));

        username = view.findViewById(R.id.account_username_text_field);
        currentServiceTime = view.findViewById(R.id.current_service_time);
        currentServiceGig = view.findViewById(R.id.current_service_gig);
        currentServicePrice = view.findViewById(R.id.current_service_price);
        endCreditDate = view.findViewById(R.id.end_credit_date_text_field);
        trafficProgressIndicator = view.findViewById(R.id.traffic_progress_indicator);
        totalTraffic = view.findViewById(R.id.total_tr_text);
        usedTraffic = view.findViewById(R.id.used_tr_text);
        MaterialButton resubscribeButton = view.findViewById(R.id.account_fragment_renew_button);
        MaterialButton resubscribeCurrentButton = view.findViewById(R.id.account_fragment_renew_current_button);
        resubscribeButton.setOnClickListener(v -> startActivity(new Intent(getContext(), SelectServiceActivity.class)));
        resubscribeCurrentButton.setOnClickListener(v -> {
            DataManager dataManager = DataManager.getInstance();
            Intent checkoutIntent = new Intent(requireActivity(), CheckoutActivity.class);
            try {
//                leave me alone bro
                checkoutIntent.putExtra("service_plan", dataManager.getServicePlans().stream().filter(servicePlan -> servicePlan.getId() == dataManager.getCurrentPlanId()).collect(Collectors.toList()).get(0));
            } catch (NullPointerException ignored) {
                return;
            }
            startActivity(checkoutIntent);
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(updateUserDataBroadcastReceiver);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        Animation animation = new Animation() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                DataManager dataManager = DataManager.getInstance();
                double value;
                if (dataManager.isUnlimitedTraffic()) {
                    value = 100;
                } else {
                    value = dataManager.getUsedTrafficGB() / dataManager.getTotalTrafficGB() *
                            interpolatedTime * 100;
                }
                double traffic = 0 + dataManager.getUsedTrafficGB() * interpolatedTime;
                usedTraffic.setText(String.format("%.2f %s", traffic, getString(R.string.gigs)));
                trafficProgressIndicator.setProgress((int) value);
            }
        };
        animation.setDuration(1000);
        animation.setInterpolator((Interpolator) AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
        trafficProgressIndicator.startAnimation(animation);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}