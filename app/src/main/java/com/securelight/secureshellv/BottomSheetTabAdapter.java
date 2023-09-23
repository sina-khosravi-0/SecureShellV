package com.securelight.secureshellv;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class BottomSheetTabAdapter extends FragmentStateAdapter {
    private int numberOfTabs;
    public BottomSheetTabAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle,
                                 int numberOfTabs) {
        super(fragmentManager, lifecycle);
        this.numberOfTabs = numberOfTabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new AccountFragment();
                break;
            default:
                fragment = new Fragment();
        }
        return fragment;
    }



    @Override
    public int getItemCount() {
        return numberOfTabs;
    }
}
