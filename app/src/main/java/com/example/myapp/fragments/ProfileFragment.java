package com.example.myapp.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapp.R;
import com.example.myapp.activities.LoginActivity;
import com.example.myapp.data.User;
import com.example.myapp.utils.ThemeUtils;
import com.example.myapp.viewmodel.ProfileViewModel;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.card.MaterialCardView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AlertDialog;
import android.view.animation.AccelerateDecelerateInterpolator;

public class ProfileFragment extends Fragment {

    private ProfileViewModel profileViewModel;
    private ShapeableImageView avatarImageView;
    private TextView emailTextView, nameTextView;
    private TextView ordersCountView, pointsCountView, vouchersCountView;
    private MaterialCardView statsCard;
    private AppBarLayout appBarLayout;
    private View rootView;
    private SignInClient signInClient;
    private ImageView themeToggleButton;
    private MaterialButton darkModeButton;

    private static final String TAG = "ProfileFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews();
        setupViewModel();
        setupClickListeners();
        setupAppBarBehavior();
        startEntryAnimations();
        setupThemeToggle();
        return rootView;
    }

    private void initGoogleSignInClient() {
        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        signInClient = Identity.getSignInClient(requireContext());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGoogleSignInClient();
    }

    private void initViews() {
        avatarImageView = rootView.findViewById(R.id.profile_avatar);
        emailTextView = rootView.findViewById(R.id.profile_email);
        nameTextView = rootView.findViewById(R.id.profile_name);
        ordersCountView = rootView.findViewById(R.id.orders_count);
        pointsCountView = rootView.findViewById(R.id.points_count);
        vouchersCountView = rootView.findViewById(R.id.vouchers_count);
        statsCard = rootView.findViewById(R.id.stats_card);
        appBarLayout = rootView.findViewById(R.id.appBarLayout);
        themeToggleButton = rootView.findViewById(R.id.theme_toggle_button);
        darkModeButton = rootView.findViewById(R.id.btn_dark_mode);

        // Prepare views for entry animations
        avatarImageView.setScaleX(0f);
        avatarImageView.setScaleY(0f);
        statsCard.setTranslationY(100f);
        statsCard.setAlpha(0f);
    }

    private void setupViewModel() {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), this::updateUI);
    }

    private void updateUI(User user) {
        if (user != null) {
            animateTextChange(nameTextView, user.getFullName());
            animateTextChange(emailTextView, user.getEmail());

            // Load avatar
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getAvatarUrl())
                        .transition(DrawableTransitionOptions.withCrossFade(500))
                        .error(R.drawable.ic_placeholder_image)
                        .into(avatarImageView);
            }

            // Update statistics
            profileViewModel.getOrderCount(user.getId())
                    .observe(getViewLifecycleOwner(),
                            orderCount -> animateNumberChange(ordersCountView, orderCount));

            // Update voucher count
            profileViewModel.getVoucherCount(user.getId())
                    .observe(getViewLifecycleOwner(),
                            voucherCount -> animateNumberChange(vouchersCountView, voucherCount));

            // Display points
            animateNumberChange(pointsCountView, user.getPoints());
        }
    }

    private void animateNumberChange(TextView textView, int newValue) {
        int oldValue = 0;
        try {
            oldValue = Integer.parseInt(textView.getText().toString());
        } catch (NumberFormatException ignored) {}

        ValueAnimator animator = ValueAnimator.ofInt(oldValue, newValue);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation ->
                textView.setText(String.valueOf(animation.getAnimatedValue())));
        animator.start();
    }

    private void setupAppBarBehavior() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            float scrollPercentage = Math.abs(verticalOffset) / (float) appBarLayout.getTotalScrollRange();

            // Animate avatar scale and translation
            float scale = 1 - (scrollPercentage * 0.4f);
            float translationY = -verticalOffset / 2f;
            avatarImageView.setScaleX(scale);
            avatarImageView.setScaleY(scale);
            avatarImageView.setTranslationY(translationY);

            // Fade text based on scroll
            float textAlpha = 1 - (scrollPercentage * 1.5f);
            nameTextView.setAlpha(Math.max(0, textAlpha));
            emailTextView.setAlpha(Math.max(0, textAlpha));
        });
    }

    private void setupThemeToggle() {
        try {
            // Initialize theme icons based on current state
            updateThemeIcons();

            // Set up the click listener for header button (without animation that could cause issues)
            themeToggleButton.setOnClickListener(v -> {
                Log.d(TAG, "Theme toggle button clicked");
                toggleDarkMode();
            });

            // Set up the click listener for list button
            darkModeButton.setOnClickListener(v -> {
                Log.d(TAG, "Dark mode button clicked");
                // Simple scale animation without complex callbacks
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                            toggleDarkMode();
                        })
                        .start();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up theme toggle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void toggleDarkMode() {
        try {
            // Get current mode
            boolean isDarkMode = ThemeUtils.isDarkModeActive(requireContext());

            // Log the action
            Log.d(TAG, "Toggling dark mode. Current mode: " + (isDarkMode ? "dark" : "light"));

            // Show toast before toggling
            Toast.makeText(requireContext(),
                    isDarkMode ? "Đang chuyển sang chế độ sáng..." : "Đang chuyển sang chế độ tối...",
                    Toast.LENGTH_SHORT).show();

            // Toggle the theme
            ThemeUtils.toggleDarkMode(requireContext());

            // Don't try to update icons or animate here -
            // the activity will be recreated and these views will no longer exist
        } catch (Exception e) {
            Log.e(TAG, "Error toggling dark mode: " + e.getMessage());
            Toast.makeText(requireContext(), "Lỗi khi chuyển đổi chế độ", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateThemeIcons() {
        try {
            boolean isDarkMode = ThemeUtils.isDarkModeActive(requireContext());

            // Set simple resources without complex drawable operations
            themeToggleButton.setImageResource(isDarkMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);

            // Update button text
            darkModeButton.setText(isDarkMode ? "Chế độ sáng" : "Chế độ tối");

            // Use setCompoundDrawablesWithIntrinsicBounds instead of setIcon for safer operation
            darkModeButton.setIcon(getResources().getDrawable(
                    isDarkMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode,
                    requireContext().getTheme()));
        } catch (Exception e) {
            Log.e(TAG, "Error updating theme icons: " + e.getMessage());
        }
    }

    private void animateThemeButtonClick() {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(themeToggleButton, View.SCALE_X, 0.8f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(themeToggleButton, View.SCALE_Y, 0.8f);
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(themeToggleButton, View.SCALE_X, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(themeToggleButton, View.SCALE_Y, 1f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(themeToggleButton, View.ROTATION, 0f, 180f);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(scaleDownX, scaleDownY);
        scaleDown.setDuration(100);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(scaleUpX, scaleUpY, rotate);
        scaleUp.setDuration(300);
        scaleUp.setInterpolator(new OvershootInterpolator());

        scaleDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Update icon before starting scale up animation
                updateThemeIcons();
                scaleUp.start();
            }
        });

        scaleDown.start();
    }

    private void setupClickListeners() {
        // Profile edit button
        rootView.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> animateButtonClick(v, () ->
                navigateToFragment(new EditProfileFragment())));

        // Order history button
        rootView.findViewById(R.id.btn_order_history).setOnClickListener(v -> animateButtonClick(v, () ->
                navigateToFragment(new OrderHistoryFragment())));

        // Vouchers button
        rootView.findViewById(R.id.btn_vouchers).setOnClickListener(v -> animateButtonClick(v, () ->
                navigateToFragment(new VoucherFragment())));

        // About us button
        rootView.findViewById(R.id.btn_about_us).setOnClickListener(v -> animateButtonClick(v, () ->
                navigateToFragment(new AboutUsFragment())));

        // Favorites button
        rootView.findViewById(R.id.btn_favorite).setOnClickListener(v ->
                animateButtonClick(v, () -> navigateToFragment(new FavoriteFragment())));

        // Logout button
        rootView.findViewById(R.id.btn_logout).setOnClickListener(v -> animateButtonClick(v, this::showLogoutDialog));

        // Avatar click animation
        avatarImageView.setOnClickListener(v -> {
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(v, View.SCALE_X, 0.9f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 0.9f);
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(v, View.SCALE_X, 1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f);

            AnimatorSet scaleDown = new AnimatorSet();
            scaleDown.play(scaleDownX).with(scaleDownY);
            scaleDown.setDuration(100);

            AnimatorSet scaleUp = new AnimatorSet();
            scaleUp.play(scaleUpX).with(scaleUpY);
            scaleUp.setDuration(100);

            scaleDown.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    scaleUp.start();
                }
            });

            scaleDown.start();
        });
    }

    private void animateButtonClick(View view, Runnable onClickAction) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(onClickAction)
                                .start())
                .start();
    }

    private void animateTextChange(TextView textView, String newText) {
        // Fade out animation
        textView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    // Change the text once faded out
                    textView.setText(newText);
                    // Fade in animation
                    textView.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }

    private void startEntryAnimations() {
        // Animate avatar
        avatarImageView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Animate stats card
        statsCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animate buttons sequentially
        View[] buttons = {
                rootView.findViewById(R.id.btn_edit_profile),
                rootView.findViewById(R.id.btn_order_history),
                rootView.findViewById(R.id.btn_vouchers),
                rootView.findViewById(R.id.btn_favorite),
                rootView.findViewById(R.id.btn_about_us),
                rootView.findViewById(R.id.btn_dark_mode), // Add the dark mode button to animation
                rootView.findViewById(R.id.btn_logout)
        };

        for (View button : buttons) {
            button.setVisibility(View.VISIBLE);
            button.setAlpha(1f);
            button.setTranslationX(0f);
        }
    }

    private void showLogoutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Có", (dialogInterface, i) -> {
                    // Get current user
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                    if (currentUser != null) {
                        // Update offline status before logging out
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(currentUser.getUid())
                                .update("status", "offline")
                                .addOnSuccessListener(aVoid -> {
                                    // Firebase logout
                                    FirebaseAuth.getInstance().signOut();

                                    // Check login provider
                                    if (currentUser.getProviderData().stream()
                                            .anyMatch(profile -> profile.getProviderId().equals("google.com"))) {
                                        // Use SignInClient for Google logout
                                        signInClient.signOut()
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(requireContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(requireContext(), "Không thể đăng xuất khỏi Google!", Toast.LENGTH_SHORT).show();
                                                    }
                                                    navigateToLogin();
                                                });
                                    } else if (currentUser.getProviderData().stream()
                                            .anyMatch(profile -> profile.getProviderId().equals("facebook.com"))) {
                                        LoginManager.getInstance().logOut();
                                        Toast.makeText(requireContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
                                        navigateToLogin();
                                    } else {
                                        navigateToLogin();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "Lỗi khi cập nhật trạng thái: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    navigateToLogin();
                                });
                    } else {
                        Toast.makeText(requireContext(), "Không thể xác định người dùng hiện tại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Không", null)
                .create();

        dialog.show();
    }
    private void navigateToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update theme icons when returning to fragment
        if (themeToggleButton != null && darkModeButton != null) {
            updateThemeIcons();
        }

        if (profileViewModel.getUserData().getValue() == null) {
            profileViewModel.refreshUserData();
        }
    }
}
