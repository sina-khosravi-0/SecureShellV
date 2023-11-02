package com.securelight.secureshellv.ui.bottomsheet.account;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;
import com.securelight.secureshellv.MainActivity;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.DataManager;

public class AccountFragment extends Fragment {
    private TextInputLayout username;
    private TextInputLayout endCreditDate;
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
                username.getEditText().setText(String.valueOf(dataManager.getUserName()));

                endCreditDate.getEditText().setText(String.valueOf(dataManager.getJalaliEndCreditDate()));
                remainingTr.getEditText().setText(String.valueOf(dataManager.getRemainingTrafficGB()));
                usedTr.getEditText().setText(String.valueOf(dataManager.getUsedTrafficGB()));
                totalTr.getEditText().setText(String.valueOf(dataManager.getTotalTrafficGB()));
                unlimitedTime.setChecked(dataManager.isUnlimitedCreditTime());
                unlimitedTraffic.setChecked(dataManager.isUnlimitedTraffic());
                connectedIps.getEditText().setText(String.valueOf(dataManager.getConnectedIps()));
                serverMessage.getEditText().setText(String.valueOf(dataManager.getMessage()));
                serverMessage.setHelperText(String.valueOf(dataManager.getMessageDateTimeString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

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

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireActivity());
        lbm.registerReceiver(updateUserDataBroadcastReceiver,
                new IntentFilter(MainActivity.UPDATE_USER_DATA_INTENT));
        username = view.findViewById(R.id.account_username_text_field);
        endCreditDate = view.findViewById(R.id.end_credit_date_text_field);
        remainingTr = view.findViewById(R.id.remaining_tr_text_field);
        usedTr = view.findViewById(R.id.used_tr_text_field);
        totalTr = view.findViewById(R.id.total_tr_text_field);
        unlimitedTime = view.findViewById(R.id.unlimited_time);
        unlimitedTraffic = view.findViewById(R.id.unlimited_traffic);
        connectedIps = view.findViewById(R.id.connected_ips_text_field);
        serverMessage = view.findViewById(R.id.server_message_text_field);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(updateUserDataBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
//        System.out.println("RESUME");
    }

    @Override
    public void onPause() {
        super.onPause();
//        System.out.println("PAUSE");
    }
}