package com.securelight.secureshellv;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.securelight.secureshellv.placeholder.AppInfoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A fragment representing a list of Items.
 */
public class InstalledPackageFragment extends DialogFragment {
    private final String packageRegex = "applicationInfo.flags|com.google.android.googlequicksearchbox|" +
            "com.android.chrome|com.google.android.apps.docs|com.google.ar.lens|" +
            "com.google.android.apps.maps|com.google.android.apps.tachyon|" +
            "com.google.android.apps.meetings|com.google.android.apps.subscriptions.red|" +
            "com.google.android.apps.photos|com.google.android.play.games|" +
            "com.google.android.youtube|com.google.android.apps.youtube.music|" +
            "com.google.android.youtube.tvmusic";
    List<ApplicationInfo> appList;
    View dialogView;
    List<AppInfoItem> appInfoList;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public InstalledPackageFragment() {
    }

    public static InstalledPackageFragment newInstance() {
        return new InstalledPackageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
        dialogView = onCreateView(getLayoutInflater(), null, savedInstanceState);
        builder.setView(dialogView);

        return builder.create();
    }

    @Nullable
    @Override
    public View getView() {
        return dialogView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View linearLayout = inflater.inflate(R.layout.fragment_item_list, container, false);
        RecyclerView recyclerView = linearLayout.findViewById(R.id.package_recycler_view);

        // Set the adapter
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            appInfoList = new ArrayList<>();
            recyclerView.setAdapter(new PackageReViewAdapter(appInfoList));
        }
        return linearLayout;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.package_recycler_view);
        ImageButton selectAll = dialogView.findViewById(R.id.select_all_packages);
        ImageButton deselectAll = dialogView.findViewById(R.id.deselect_all_packages);
        MaterialButton applyPackages = dialogView.findViewById(R.id.apply_package_list);
        MaterialButton cancelPackages = dialogView.findViewById(R.id.cancel_package_list);

        PackageReViewAdapter adapter = (PackageReViewAdapter) recyclerView.getAdapter();
        assert adapter != null;

        PackageManager packageManager = getContext().getApplicationContext().getPackageManager();
        appList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA |
                PackageManager.GET_SHARED_LIBRARY_FILES);
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(getContext());

        new Thread(() -> { // adding items thread
            while (recyclerView.isComputingLayout()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            appList.forEach(applicationInfo -> {
                if (((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        applicationInfo.packageName.matches(packageRegex)) &&
                        !applicationInfo.packageName.equals(getContext().getApplicationContext().getPackageName())) {
                    String appName;
                    try {
                        appName = Objects.requireNonNull(packageManager.getApplicationLabel(applicationInfo)).toString();
                    } catch (NullPointerException e) {
                        appName = applicationInfo.packageName;
                    }
                    Drawable appIcon = null;
                    try {
                        appIcon = Objects.requireNonNull(packageManager.getApplicationIcon(applicationInfo));
                    } catch (NullPointerException ignored) {
                    }
                    AppInfoItem appinfoItem = new AppInfoItem(
                            preferences.isPackageFiltered(applicationInfo.packageName),
                            applicationInfo.packageName,
                            appName,
                            appIcon);

                    adapter.getAppInfoList().add(appinfoItem);
                }
                adapter.getAppInfoList().sort((first, second) -> {
                    if (!first.isChecked() && second.isChecked()) {
                        return 1;
                    }
                    if (first.isChecked() && !second.isChecked()) {
                        return -1;
                    }
                    return 0;
                });
                getActivity().runOnUiThread(() -> {
                    recyclerView.swapAdapter(adapter, false);
                });
            });
        }).start(); // adding items thread

        selectAll.setOnClickListener(v -> {
            new Thread(() -> { // select thread
                adapter.getAppInfoList().forEach(item -> {
                    item.setChecked(true);
                });
                getActivity().runOnUiThread(adapter::notifyDataSetChanged);
            }).start(); // select thread
        });

        deselectAll.setOnClickListener(v -> {
            new Thread(() -> { // deselect thread
                adapter.getAppInfoList().forEach(item -> {
                    item.setChecked(false);
                });
                getActivity().runOnUiThread(adapter::notifyDataSetChanged);
            }).start();// deselect thread
        });
        applyPackages.setOnClickListener(v -> {
            preferences.clearFilteredPackages();
            adapter.getAppInfoList().forEach(appInfoItem -> {
                if (appInfoItem.isChecked()) {
                    preferences.addToPackageFilter(appInfoItem.getPackageName());
                }
            });
            this.dismiss();
        });

        cancelPackages.setOnClickListener(v -> dismiss());
    }
}