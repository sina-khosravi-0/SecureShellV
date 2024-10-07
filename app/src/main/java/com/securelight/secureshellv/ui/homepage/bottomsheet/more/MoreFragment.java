package com.securelight.secureshellv.ui.homepage.bottomsheet.more;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.ui.homepage.HomepageActivity;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MoreFragment extends Fragment {

    public MoreFragment() {
    }

    public static MoreFragment newInstance() {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(getActivity());
        MaterialButton logoutButton = view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(getActivity())
                    .setTitle(R.string.confirmation).setMessage(R.string.are_you_sure_about_logging_out)
                    .setPositiveButton(R.string.yes, ((dialog, which) -> {
                        preferences.clearCredentials();
                        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
                        localBroadcastManager.sendBroadcast(
                                new Intent(Intents.SIGN_IN_ACTION).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        localBroadcastManager.sendBroadcast(new Intent(Intents.KILL_HOMEPAGE_ACTIVITY));
                    }))
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }
}