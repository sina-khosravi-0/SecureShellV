package com.securelight.secureshellv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(getActivity());
        ImageButton infoButton = view.findViewById(R.id.app_filter_info);
        RadioGroup radioGroup = view.findViewById(R.id.app_filter_mode_radio_group);
        switch (preferences.getAppFilterMode()) {
            case OFF:
                radioGroup.check(R.id.off_app_filter_radio);
                break;
            case EXCLUDE:
                radioGroup.check(R.id.exclude_app_filter_radio);
                break;
            case INCLUDE:
                radioGroup.check(R.id.include_app_filter_radio);
                break;
        }

        infoButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(getActivity())
                    .setTitle(R.string.what_is_app_filter).setMessage(R.string.selected_app_alert)
                    .setNeutralButton(R.string.ok, null)
                    .show();
        });
        MaterialButton selectAppsButton = view.findViewById(R.id.select_apps_button);
        selectAppsButton.setOnClickListener(v -> {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            InstalledPackageFragment installedPackageDialogue = InstalledPackageFragment.newInstance();
            installedPackageDialogue.show(fm, "fragment_alert");
        });

        radioGroup.setOnCheckedChangeListener(((group, checkedId) -> {
            RadioButton radioButton = group.findViewById(checkedId);
            switch (group.indexOfChild(radioButton)) {
                case 0: // off
                    preferences.setAppFilterMode(Constants.AppFilterMode.OFF);
                    break;
                case 1: // exclude
                    preferences.setAppFilterMode(Constants.AppFilterMode.EXCLUDE);
                    break;
                case 2: // include
                    preferences.setAppFilterMode(Constants.AppFilterMode.INCLUDE);
            }
        }));

    }
}