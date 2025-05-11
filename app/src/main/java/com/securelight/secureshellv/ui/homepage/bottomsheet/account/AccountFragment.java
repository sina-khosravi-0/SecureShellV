package com.securelight.secureshellv.ui.homepage.bottomsheet.account;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;
import com.securelight.secureshellv.resubscribe.SelectServiceActivity;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.resubscribe.CheckoutActivity;
import com.securelight.secureshellv.backend.DataManager;

import java.util.Objects;

public class AccountFragment extends Fragment {
    private TextInputLayout username;
    private TextInputLayout endCreditDate;
    private LinearLayout dataLimitBlock;
    private TextInputLayout remainingTr;
    private TextInputLayout usedTr;
    private TextInputLayout totalTr;
    private MaterialCheckBox unlimitedTime;
    private MaterialCheckBox unlimitedTraffic;
    private TextInputLayout connectedIps;
    private TextInputLayout serverMessage;
    private final BroadcastReceiver updateUserDataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataManager dataManager = DataManager.getInstance();
            try {
                Objects.requireNonNull(username.getEditText()).setText(String.valueOf(dataManager.getUserName()));
                if (dataManager.isUnlimitedCreditTime()) {
                    endCreditDate.setVisibility(View.GONE);
                } else {
                    Objects.requireNonNull(endCreditDate.getEditText()).setText(String.valueOf(dataManager.getJalaliEndCreditDate()));
                }
                if (dataManager.isUnlimitedTraffic()) {
                    dataLimitBlock.setVisibility(View.GONE);
                } else {
                    dataLimitBlock.setVisibility(View.VISIBLE);
                    Objects.requireNonNull(remainingTr.getEditText()).setText(String.valueOf(dataManager.getRemainingTrafficGB()));
                    Objects.requireNonNull(usedTr.getEditText()).setText(String.valueOf(dataManager.getUsedTrafficGB()));
                    Objects.requireNonNull(totalTr.getEditText()).setText(String.valueOf(dataManager.getTotalTrafficGB()));
                }
                unlimitedTime.setChecked(dataManager.isUnlimitedCreditTime());
                unlimitedTraffic.setChecked(dataManager.isUnlimitedTraffic());
                Objects.requireNonNull(connectedIps.getEditText()).setText(String.valueOf(dataManager.getConnectedIps()));
                Objects.requireNonNull(serverMessage.getEditText()).setText(String.valueOf(dataManager.getMessage()));
                serverMessage.setHelperText(String.valueOf(dataManager.getMessageDateTimeString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    /** @noinspection FieldCanBeLocal*/
    private MaterialButton resubscribeButton;

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
        view.findViewById(R.id.touch_blocker).setOnTouchListener((v, event) -> {
            v.performClick();
            return true;
        });

        LocalBroadcastManager.getInstance(requireActivity())
                .registerReceiver(updateUserDataBroadcastReceiver,
                        new IntentFilter(Intents.UPDATE_USER_DATA_INTENT));

        username = view.findViewById(R.id.account_username_text_field);
        endCreditDate = view.findViewById(R.id.end_credit_date_text_field);
        dataLimitBlock = view.findViewById(R.id.data_limit_block);
        remainingTr = view.findViewById(R.id.remaining_tr_text_field);
        usedTr = view.findViewById(R.id.used_tr_text_field);
        totalTr = view.findViewById(R.id.total_tr_text_field);
        unlimitedTime = view.findViewById(R.id.unlimited_time);
        unlimitedTraffic = view.findViewById(R.id.unlimited_traffic);
        connectedIps = view.findViewById(R.id.connected_ips_text_field);
        serverMessage = view.findViewById(R.id.server_message_text_field);
        resubscribeButton = view.findViewById(R.id.account_fragment_renew_button);

        resubscribeButton.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SelectServiceActivity.class));
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(updateUserDataBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}