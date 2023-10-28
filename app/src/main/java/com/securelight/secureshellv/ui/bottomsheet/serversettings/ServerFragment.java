package com.securelight.secureshellv.ui.bottomsheet.serversettings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ServerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ServerFragment extends Fragment {

    // TODO: Rename and change types and number of parameters
    public static ServerFragment newInstance() {
        return new ServerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_server, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedPreferencesSingleton preferencesSingleton = SharedPreferencesSingleton.getInstance(getActivity());
        TextInputLayout textInputLayout = view.findViewById(R.id.server_location_text_input);
        // TODO: get servers list
        List<String> items =
                Arrays.asList("ato", "us", "ae", "au", "ca", "cn", "de", "fr",
                        "in", "it", "jp", "nl", "se", "sg", "tr", "uk");

        ServerLocationArrayAdapter arrayAdapter = new ServerLocationArrayAdapter(
                requireActivity(), R.layout.dropdown_item, items);
        MaterialAutoCompleteTextView autoComplete = textInputLayout.findViewById(R.id.server_location_auto_complete);
        autoComplete.setAdapter(arrayAdapter);
        autoComplete.setDropDownAnchor(R.id.server_location_text_input);

        arrayAdapter.setAutoCompleteItem
                (autoComplete, preferencesSingleton.getSelectedServerLocation());

        autoComplete.setOnItemClickListener((parent, textView, position, menuItemId) -> {
            ((TextView) autoComplete).setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ((TextView) textView).getCompoundDrawablesRelative()[2],
                    null, null, null);
            preferencesSingleton.setServer(arrayAdapter.getCode(position));
        });
    }
}