package com.securelight.secureshellv.ui.homepage.bottomsheet.serversettings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ServerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ServerFragment extends Fragment {

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedPreferencesSingleton preferencesSingleton = SharedPreferencesSingleton.getInstance(getActivity());
        TextInputLayout textInputLayout = view.findViewById(R.id.server_location_text_input);
        List<String> items = new ArrayList<>();
        items.add("ato");
        items.addAll(DataManager.getInstance().getAvailableServerLocations());

        ServerLocationArrayAdapter arrayAdapter = new ServerLocationArrayAdapter(
                requireActivity(), R.layout.dropdown_item, items);
        MaterialAutoCompleteTextView autoComplete = textInputLayout.findViewById(R.id.server_location_auto_complete);
        autoComplete.setAdapter(arrayAdapter);

        new Thread(() -> {
            DataManager.getInstance().fetchServerSelection();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    arrayAdapter.setAutoCompleteItem(autoComplete, preferencesSingleton.getSelectedServerLocationForDropDown());
                });
            }
        }).start();

        autoComplete.setOnItemClickListener((parent, textView, position, menuItemId) -> {
            autoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ((TextView) textView).getCompoundDrawablesRelative()[2],
                    null, null, null);
            preferencesSingleton.setServerLocation(arrayAdapter.getCode(position));
        });
    }

    @Override
    public void onResume() {
        new Thread(() -> DataManager.getInstance().fetchServerSelection()).start();
        super.onResume();
    }
}