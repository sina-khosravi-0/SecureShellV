package com.securelight.secureshellv.ui.bottomsheet.settings;

import android.content.Intent;
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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.ui.bottomsheet.settings.appfilter.InstalledPackageDialogFragment;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

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
        ImageButton appFilterInfoButton = view.findViewById(R.id.app_filter_info);
        RadioGroup appFilterRadioGroup = view.findViewById(R.id.app_filter_mode_radio_group);
        MaterialButton selectAppsButton = view.findViewById(R.id.select_apps_button);

        RadioGroup appLanguageRadioGroup = view.findViewById(R.id.app_language_radio_group);
        MaterialButton appRestartButton = view.findViewById(R.id.restart_app_button);

        MaterialSwitch persistentSwitch = view.findViewById(R.id.persistent_notification_switch);

        switch (preferences.getAppFilterMode()) {
            case OFF:
                appFilterRadioGroup.check(R.id.off_app_filter_radio);
                break;
            case EXCLUDE:
                appFilterRadioGroup.check(R.id.exclude_app_filter_radio);
                break;
            case INCLUDE:
                appFilterRadioGroup.check(R.id.include_app_filter_radio);
                break;
        }

        switch(preferences.getAppLanguage()) {
            case "en" :
                appLanguageRadioGroup.check(R.id.english_radio);
                break;
            case "fa" :
                appLanguageRadioGroup.check(R.id.persian_radio);
                break;
        }

        appFilterInfoButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(getActivity())
                    .setTitle(R.string.what_is_app_filter).setMessage(R.string.selected_app_alert)
                    .setNeutralButton(R.string.ok, null)
                    .show();
        });
        selectAppsButton.setOnClickListener(v -> {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            InstalledPackageDialogFragment installedPackageDialogue = InstalledPackageDialogFragment.newInstance();
            installedPackageDialogue.show(fm, "fragment_alert");
        });

        appFilterRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
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
        });

        appLanguageRadioGroup.setOnCheckedChangeListener(((group, checkedId) -> {
            RadioButton radioButton = group.findViewById(checkedId);
            switch(group.indexOfChild(radioButton)) {
                case 0:
                    preferences.setAppLanguage("en");
                    break;
                case 1:
                    preferences.setAppLanguage("fa");
                    break;
            }
        }));
        appRestartButton.setOnClickListener(v -> {
            Intent intent = getActivity().getIntent();
            getActivity().finish();
            startActivity(intent);
        });

        persistentSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.notice)
                    .setMessage(R.string.persistent_notification_switch_hint)
                    .setNeutralButton(R.string.ok, null).show();
        }));
    }
}