package com.securelight.secureshellv;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.os.LocaleListCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivityTest extends AppCompatActivity {
    LinearLayout bottomSheetLayout;
    BottomSheetBehavior<View> bottomSheetBehavior;
    CoordinatorLayout rootLayout;

    public static int colorPrimary;
    public static int colorOnPrimary;
    public static int colorSecondary;
    public static int colorOnSecondary;
    public static int colorSecondaryContainer;
    public static int colorOnSecondaryContainer;
    public static int colorTertiary;
    public static int colorOnTertiary;
    public static int colorTertiaryContainer;
    public static int colorOnTertiaryContainer;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        Locale locale = new Locale("fa");
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale));

        setContentView(R.layout.activity_main);
        setColors();

        ImageButton vpnButton = findViewById(R.id.vpn_toggle_img);
        AtomicBoolean off = new AtomicBoolean(true);

        vpnButton.setOnClickListener(v -> {
            if (off.get()) {
                vpnButton.setImageResource(R.drawable.vpn_loading_animated);
                off.set(false);
                AnimatedVectorDrawable vectorDrawable = (AnimatedVectorDrawable) vpnButton.getDrawable();
                vectorDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        vectorDrawable.start();
                    }
                });
                vectorDrawable.start();
            } else {
                vpnButton.setImageResource(R.drawable.vpn_toggle_vector);
                off.set(true);
            }
        });

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

                ViewPropertyAnimator imageAnimator = imageView.animate();
                imageAnimator.translationY(-offset / 2f).setDuration(300);
                imageAnimator.scaleX(1.5f).setDuration(300);
                imageAnimator.scaleY(1.5f).setDuration(300);

                Integer colorFrom = colorOnSecondaryContainer;
                Integer colorTo = colorPrimary;

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

                Integer colorFrom = colorPrimary;
                Integer colorTo = colorOnSecondaryContainer;
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
            ConstraintLayout view = (ConstraintLayout) getLayoutInflater().inflate(R.layout.bottom_sheet_tab, null);
            ImageView imageView = view.findViewById(R.id.image_view);
            TextView textView = view.findViewById(R.id.text_view);
            textView.setTextColor(colorOnSecondaryContainer);
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(colorOnSecondaryContainer));

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
        viewPager.setOffscreenPageLimit(NUMBER_OF_TABS);
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
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private void setColors() {
        TypedValue typedValue = new TypedValue();

        this.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;
        this.getTheme().resolveAttribute(R.attr.colorOnPrimary, typedValue, true);
        colorOnPrimary = typedValue.data;
        this.getTheme().resolveAttribute(R.attr.colorSecondaryContainer, typedValue, true);
        colorSecondaryContainer = typedValue.data;
        this.getTheme().resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true);
        colorOnSecondaryContainer = typedValue.data;
        this.getTheme().resolveAttribute(R.attr.colorTertiary, typedValue, true);
        colorTertiary = typedValue.data;
        this.getTheme().resolveAttribute(R.attr.colorOnTertiary, typedValue, true);
        colorOnTertiary = typedValue.data;
        this.getTheme().resolveAttribute(R.attr.colorTertiaryContainer, typedValue, true);
        colorTertiaryContainer = typedValue.data;
        this.getTheme().resolveAttribute(R.attr.colorOnTertiaryContainer, typedValue, true);
        colorOnTertiaryContainer = typedValue.data;
    }
}