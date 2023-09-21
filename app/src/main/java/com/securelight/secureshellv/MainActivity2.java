package com.securelight.secureshellv;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MainActivity2 extends AppCompatActivity {
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        ConstraintLayout constraintLayout = findViewById(R.id.standard_bottom_sheet);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(constraintLayout);
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) constraintLayout.getParent();

        bottomSheetBehavior.setMaxHeight((int) (coordinatorLayout.getHeight() * 0.75));
//        bottomSheetBehavior.setPeekHeight((int) (coordinatorLayout.getHeight() * 0.10));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        ConstraintLayout constraintLayout = findViewById(R.id.standard_bottom_sheet);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(constraintLayout);
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) constraintLayout.getParent();

//        bottomSheetBehavior.setPeekHeight(200);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
//                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
//                    // Animate transition from rounded to flat edges
//                    ViewPropertyAnimator viewPropertyAnimator = frameLayout.animate();
//                    viewPropertyAnimator.translationX(20);
////                    ObjectAnimator animatorSet = (ObjectAnimator) AnimatorInflater.loadAnimator(
////                            getApplicationContext(), R.animator.rounded_to_flat);
////                    animatorSet.setTarget(frameLayout);
////                    animatorSet.start();
////                    frameLayout.setBackgroundResource(R.drawable.flat);
//                } else {
//                    // Animate transition from flat to rounded edges
//                    ViewPropertyAnimator viewPropertyAnimator = frameLayout.animate();
//                    viewPropertyAnimator.translationX(-20);
////                    ObjectAnimator animatorSet = (ObjectAnimator) AnimatorInflater.loadAnimator(
////                            getApplicationContext(), R.animator.flat_to_rounded);
////                    animatorSet.setTarget(frameLayout);
////                    animatorSet.start();
////                    frameLayout.setBackgroundResource(R.drawable.rounded);
//                }
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
}