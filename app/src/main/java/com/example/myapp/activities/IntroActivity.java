package com.example.myapp.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapp.R;
import com.airbnb.lottie.LottieAnimationView;

public class IntroActivity extends AppCompatActivity {

    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView sloganTextView;
    private View loadingContainer;
    private LottieAnimationView particlesAnimation;

    private static final long ANIMATION_DURATION = 1000;
    private static final long SCREEN_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Hide status bar and action bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupGradientText();
        startAnimations();
        scheduleNextScreen();
    }

    private void initializeViews() {
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        sloganTextView = findViewById(R.id.sloganTextView);
        loadingContainer = findViewById(R.id.loadingContainer);
        particlesAnimation = findViewById(R.id.particlesAnimation);

        // Set initial alpha to 0 for fade-in animation
        logoImageView.setAlpha(0f);
        appNameTextView.setAlpha(0f);
        sloganTextView.setAlpha(0f);
        loadingContainer.setAlpha(0f);
    }

    private void setupGradientText() {
        // Create gradient shader for slogan text
        int[] colors = new int[]{
                Color.parseColor("#FFFFFF"),     // Trắng
                Color.parseColor("#E0F7FA")      // Xanh nhạt
        };

        float[] positions = new float[]{0f, 1f};  // Gradient positions

        LinearGradient gradient = new LinearGradient(
                0f, 0f,
                sloganTextView.getPaint().measureText(sloganTextView.getText().toString()),
                sloganTextView.getLineHeight(),
                colors,
                positions,
                Shader.TileMode.CLAMP);

        sloganTextView.getPaint().setShader(gradient);

        // Thêm shadow cho text
        sloganTextView.setShadowLayer(4f, 0f, 2f, Color.parseColor("#80000000"));

        sloganTextView.invalidate();
    }

    private void startAnimations() {
        // Logo animation
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
                ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(logoImageView, "scaleX", 0.3f, 1f),
                ObjectAnimator.ofFloat(logoImageView, "scaleY", 0.3f, 1f)
        );
        logoAnim.setInterpolator(new OvershootInterpolator());
        logoAnim.setDuration(ANIMATION_DURATION);

        // Text animations with sequence
        ObjectAnimator nameAnim = ObjectAnimator.ofFloat(appNameTextView, "alpha", 0f, 1f);
        nameAnim.setStartDelay(ANIMATION_DURATION / 2);
        nameAnim.setDuration(ANIMATION_DURATION / 2);

        ObjectAnimator sloganAnim = ObjectAnimator.ofFloat(sloganTextView, "alpha", 0f, 1f);
        sloganAnim.setStartDelay(ANIMATION_DURATION);
        sloganAnim.setDuration(ANIMATION_DURATION / 2);

        // Loading container fade in
        ObjectAnimator loadingAnim = ObjectAnimator.ofFloat(loadingContainer, "alpha", 0f, 1f);
        loadingAnim.setStartDelay(ANIMATION_DURATION * 3/2);
        loadingAnim.setDuration(ANIMATION_DURATION / 2);

        // Play all animations together
        AnimatorSet allAnimations = new AnimatorSet();
        allAnimations.playTogether(logoAnim, nameAnim, sloganAnim, loadingAnim);
        allAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        allAnimations.start();

        // Start particles animation
        particlesAnimation.playAnimation();
    }

    private void scheduleNextScreen() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(IntroActivity.this, OnboardActivity.class);
            startActivity(intent);

            // Add custom transition animation
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            finish();
        }, SCREEN_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (particlesAnimation != null) {
            particlesAnimation.cancelAnimation();
        }
    }
}