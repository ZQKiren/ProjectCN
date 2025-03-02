package com.example.myapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapp.R;
import com.example.myapp.activities.LoginActivity;
import com.example.myapp.utils.InputValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ForgotPasswordFragment extends Fragment {

    private TextInputEditText etEmail;
    private MaterialButton btnResetPassword;
    private TextView tvBackToLogin;
    private CircularProgressIndicator progressBar;
    private FirebaseAuth mAuth;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_forgot_password, container, false);
        initViews();
        setupListeners();
        return rootView;
    }

    private void initViews() {
        mAuth = FirebaseAuth.getInstance();
        etEmail = rootView.findViewById(R.id.et_email);
        btnResetPassword = rootView.findViewById(R.id.btn_reset_password);
        tvBackToLogin = rootView.findViewById(R.id.tv_back_to_login);
        progressBar = rootView.findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnResetPassword.setOnClickListener(v -> handleResetPassword());
        tvBackToLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void handleResetPassword() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();

        if (!InputValidator.validateEmail(etEmail)) {
            showError(getString(R.string.error_invalid_email));
            return;
        }

        showLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        showSuccessAndNavigateBack();
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void showLoading(boolean show) {
        btnResetPassword.setEnabled(!show);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        etEmail.setEnabled(!show);
    }

    private void handleError(Exception exception) {
        String message = exception != null ? exception.getMessage() : getString(R.string.error_unknown);
        if (message != null) {
            if (message.contains("no user record")) {
                showError(getString(R.string.error_email_not_found));
            } else {
                showError(getString(R.string.error_reset_password, message));
            }
        }
    }

    private void showSuccessAndNavigateBack() {
        Snackbar.make(rootView, R.string.success_reset_email_sent, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_color))
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        navigateToLogin();
                    }
                })
                .show();
    }

    private void showError(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color))
                .show();
    }

    private void navigateToLogin() {
        if (isAdded()) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }
}