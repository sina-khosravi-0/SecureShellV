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

import com.github.eloyzone.jalalicalendar.DateConverter;
import com.google.android.material.textfield.TextInputLayout;
import com.securelight.secureshellv.MainActivity;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.UserData;

public class AccountFragment extends Fragment {
    TextInputLayout username;
    TextInputLayout endCreditDate;
    TextInputLayout remainingTr;
    TextInputLayout usedTr;
    TextInputLayout totalTr;
    TextInputLayout connectedIps;
    TextInputLayout serverMessage;

    private final BroadcastReceiver updateUserDataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UserData userData = UserData.getInstance();
            try {
                username.getEditText().setText(String.valueOf(userData.getUserName()));
                DateConverter dateConverter = new DateConverter();
                endCreditDate.getEditText().setText(String.valueOf(dateConverter.gregorianToJalali(
                        userData.getEndCreditDate().getYear(),
                        userData.getEndCreditDate().getMonthValue(),
                        userData.getEndCreditDate().getDayOfMonth())));
                remainingTr.getEditText().setText(String.valueOf(userData.getRemainingTrafficGB()));
                usedTr.getEditText().setText(String.valueOf(userData.getUsedTrafficGB()));
                totalTr.getEditText().setText(String.valueOf(userData.getTotalTrafficGB()));
                connectedIps.getEditText().setText(String.valueOf(userData.getConnectedIps()));
                serverMessage.getEditText().setText(String.valueOf(userData.getMessage()));
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