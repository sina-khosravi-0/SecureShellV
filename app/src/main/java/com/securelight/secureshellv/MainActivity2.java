package com.securelight.secureshellv;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity2 extends AppCompatActivity {
    ConstraintLayout bottomSheetLayout;
    BottomSheetBehavior<View> bottomSheetBehavior;
    CoordinatorLayout rootLayout;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
//        ConstraintLayout constraintLayout = findViewById(R.id.standard_bottom_sheet);
//
//        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(constraintLayout);
//        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) constraintLayout.getParent();
//
//        bottomSheetBehavior.setMaxHeight((int) (coordinatorLayout.getHeight() * 0.75));
////        bottomSheetBehavior.setPeekHeight((int) (coordinatorLayout.getHeight() * 0.10));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main2);
        bottomSheetLayout = findViewById(R.id.standard_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        rootLayout = (CoordinatorLayout) bottomSheetLayout.getParent();

        int NUMBER_OF_TABS = 5;
        TabLayout tabLayout = findViewById(R.id.bottom_sheet_menu_tab_layout);
        ViewPager2 viewPager = findViewById(R.id.bottom_sheet_view_pager);
        BottomSheetTabAdapter tabAdapter = new BottomSheetTabAdapter(
                getSupportFragmentManager(), getLifecycle(), NUMBER_OF_TABS);
        viewPager.setAdapter(tabAdapter);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheetBehavior.setDraggable(false);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                    bottomSheetBehavior.setDraggable(true);
                }
                View view = tab.getCustomView();
                TextView textView = view.findViewById(R.id.text_view);
                ImageView imageView = view.findViewById(R.id.image_view);
                ViewPropertyAnimator textAnimator = textView.animate();
                float offset = (textView.getHeight() == 0 ? 20 : textView.getHeight());
                textAnimator.translationY(-offset / 3f).setDuration(300);
                textAnimator.scaleX(1.2f).setDuration(300);
                textAnimator.scaleY(1.2f).setDuration(300);

                ViewPropertyAnimator imageAnimator = imageView.animate();
                imageAnimator.translationY(-offset / 2f).setDuration(300);
                imageAnimator.scaleX(1.5f).setDuration(300);
                imageAnimator.scaleY(1.5f).setDuration(300);

                Integer colorFrom = getResources().getColor(R.color.white);
                Integer colorTo = getResources().getColor(R.color.black);
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                colorAnimation.addUpdateListener(animator ->
                {
//                            textView.setTextColor((Integer) animator.getAnimatedValue());
                    imageView.setColorFilter((Integer) animator.getAnimatedValue());

                });
                colorAnimation.start();

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View view = tab.getCustomView();
                TextView textView = view.findViewById(R.id.text_view);
                ImageView imageView = view.findViewById(R.id.image_view);

                ViewPropertyAnimator textAnimator = textView.animate();
                textAnimator.translationY(0);
                textAnimator.scaleX(1).setDuration(300);
                textAnimator.scaleY(1).setDuration(300);

                ViewPropertyAnimator imageAnimator = imageView.animate();
                imageAnimator.translationY(0).setDuration(300);
                imageAnimator.scaleX(1).setDuration(300);
                imageAnimator.scaleY(1).setDuration(300);

                Integer colorFrom = getResources().getColor(R.color.black);
                Integer colorTo = getResources().getColor(R.color.white);
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                colorAnimation.addUpdateListener(animator ->
                        imageView.setColorFilter((Integer) animator.getAnimatedValue()));
                colorAnimation.start();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });


        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View view = getLayoutInflater().inflate(R.layout.bottom_sheet_tab, null);
            ImageView imageView = view.findViewById(R.id.image_view);
            TextView textView = view.findViewById(R.id.text_view);
            switch (position) {
                case 0:
                    imageView.setImageResource(R.drawable.person_icon);
                    textView.setText(R.string.bottom_sheet_account);
                    tab.setCustomView(view);
                    break;
                case 1:
                    imageView.setImageResource(R.drawable.server_icon);
                    textView.setText(R.string.bottom_sheet_server);
                    tab.setCustomView(view);
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.ic_home_black_24dp);
                    textView.setText(R.string.bottom_sheet_home);
                    tab.setCustomView(view);
                    tab.select();
                    break;
                case 3:
                    imageView.setImageResource(R.drawable.settings_icon);
                    textView.setText(R.string.bottom_sheet_settings);
                    tab.setCustomView(view);
                    break;
                case 4:
                    imageView.setImageResource(R.drawable.more_icon);
                    textView.setText(R.string.bottom_sheet_more);
                    tab.setCustomView(view);
                    break;
                default:
                    break;
            }
        }).attach();
        tabLayout.getTabAt(2).select();
        viewPager.setUserInputEnabled(false);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    tabLayout.getTabAt(2).select();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                float progress = Math.abs(slideOffset); // Get the absolute slide offset value
//
//                // Calculate the interpolated progress for the animation
//                float interpolatedProgress = Math.min(0.5f, progress);
//                System.out.println(slideOffset);
//                // Apply the interpolated progress to the views
//                constraintLayout.setScaleX(interpolatedProgress + 1);
            }
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }
}