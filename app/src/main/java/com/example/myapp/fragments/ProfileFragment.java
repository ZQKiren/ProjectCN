package com.example.myapp.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapp.R;
import com.example.myapp.activities.LoginActivity;
import com.example.myapp.data.User;
import com.example.myapp.viewmodel.ProfileViewModel;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
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
import android.widget.Toast;

public class ProfileFragment extends Fragment {

    private ProfileViewModel profileViewModel;
    private ShapeableImageView avatarImageView;
    private TextView emailTextView, nameTextView;
    private TextView ordersCountView, pointsCountView, vouchersCountView;
    private MaterialCardView statsCard;
    private AppBarLayout appBarLayout;
    private View rootView;
    private SignInClient signInClient;

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
        return rootView;
    }

    private void initGoogleSignInClient() {
        // Thay thế GoogleSignInOptions bằng BeginSignInRequest
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

            // Cập nhật các số liệu
            profileViewModel.getOrderCount(user.getId())
                    .observe(getViewLifecycleOwner(),
                            orderCount -> animateNumberChange(ordersCountView, orderCount));

            // Cập nhật số voucher
            profileViewModel.getVoucherCount(user.getId())
                    .observe(getViewLifecycleOwner(),
                            voucherCount -> animateNumberChange(vouchersCountView, voucherCount));

            // Hiển thị điểm
            animateNumberChange(pointsCountView, user.getPoints());

            // Cập nhật số voucher
            profileViewModel.getVoucherCount(user.getId())
                    .observe(getViewLifecycleOwner(),
                            voucherCount -> animateNumberChange(vouchersCountView, voucherCount));
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

        // Vouchers button
        rootView.findViewById(R.id.btn_about_us).setOnClickListener(v -> animateButtonClick(v, () ->
                navigateToFragment(new AboutUsFragment ())));

        // Trong setupClickListeners() thêm:
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
                rootView.findViewById(R.id.btn_logout)
        };

        for (View button : buttons) {
            button.setVisibility(View.VISIBLE); // Đảm bảo hiển thị nút
            button.setAlpha(1f);               // Hiển thị rõ ràng
            button.setTranslationX(0f);        // Đặt vị trí ban đầu
        }
    }

    private void showLogoutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Có", (dialogInterface, i) -> {
                    // Lấy người dùng hiện tại
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                    if (currentUser != null) {
                        // Cập nhật trạng thái offline trước khi đăng xuất
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(currentUser.getUid())
                                .update("status", "offline")
                                .addOnSuccessListener(aVoid -> {
                                    // Đăng xuất Firebase
                                    FirebaseAuth.getInstance().signOut();

                                    // Kiểm tra provider đăng nhập
                                    if (currentUser.getProviderData().stream()
                                            .anyMatch(profile -> profile.getProviderId().equals("google.com"))) {
                                        // Sử dụng SignInClient để đăng xuất Google
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
        if (profileViewModel.getUserData().getValue() == null) {
            profileViewModel.refreshUserData();
        }
    }
}