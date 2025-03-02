package com.example.myapp.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapp.R;
import com.google.android.material.button.MaterialButton;
import com.airbnb.lottie.LottieAnimationView;


public class OnboardActivity extends AppCompatActivity {

    private ImageView appLogo;
    private CardView textContainer;
    private MaterialButton btnLogin;
    private MaterialButton btnRegister;
    private LottieAnimationView loadingAnimation;
    private View buttonsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboard);

        initializeViews();
        setupWindowInsets();
        startAnimations();
        setupClickListeners();
    }

    private void initializeViews() {
        appLogo = findViewById(R.id.appLogo);
        textContainer = findViewById(R.id.textContainer);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        buttonsContainer = findViewById(R.id.buttonsContainer);

        // Set initial states for animations
        appLogo.setScaleX(0f);
        appLogo.setScaleY(0f);
        textContainer.setAlpha(0f);
        textContainer.setTranslationY(100f);
        buttonsContainer.setAlpha(0f);
        buttonsContainer.setTranslationY(100f);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void startAnimations() {
        // Logo animation
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
                ObjectAnimator.ofFloat(appLogo, "scaleX", 0f, 1f),
                ObjectAnimator.ofFloat(appLogo, "scaleY", 0f, 1f)
        );
        logoAnim.setInterpolator(new OvershootInterpolator());
        logoAnim.setDuration(1000);

        // Text container animation
        AnimatorSet textAnim = new AnimatorSet();
        textAnim.playTogether(
                ObjectAnimator.ofFloat(textContainer, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(textContainer, "translationY", 100f, 0f)
        );
        textAnim.setStartDelay(500);
        textAnim.setDuration(800);

        // Buttons container animation
        AnimatorSet buttonsAnim = new AnimatorSet();
        buttonsAnim.playTogether(
                ObjectAnimator.ofFloat(buttonsContainer, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(buttonsContainer, "translationY", 100f, 0f)
        );
        buttonsAnim.setStartDelay(800);
        buttonsAnim.setDuration(800);

        // Play all animations
        AnimatorSet allAnimations = new AnimatorSet();
        allAnimations.playTogether(logoAnim, textAnim, buttonsAnim);
        allAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        allAnimations.start();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLoginClick());
        btnRegister.setOnClickListener(v -> handleRegisterClick());
    }

    private void handleLoginClick() {
        // Show loading animation
        buttonsContainer.setVisibility(View.INVISIBLE);
        loadingAnimation.setVisibility(View.VISIBLE);
        loadingAnimation.playAnimation();

        // Simulate a delay for loading effect
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(OnboardActivity.this, LoginActivity.class);
            startActivity(intent);
            // Hide loading animation
            loadingAnimation.cancelAnimation();
            loadingAnimation.setVisibility(View.GONE);
            buttonsContainer.setVisibility(View.VISIBLE);
        }, 1500);
    }

    private void handleRegisterClick() {
        Intent intent = new Intent(OnboardActivity.this, SignupActivity.class);
        startActivity(intent);
    }

}