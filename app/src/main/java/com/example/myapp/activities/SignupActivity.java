package com.example.myapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapp.R;
import com.example.myapp.utils.InputValidator;
import com.example.myapp.viewmodel.SignUpViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnSignUp;
    private SignUpViewModel signUpViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        setupClickListeners();

        // Khởi tạo ViewModel
        signUpViewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

        // Quan sát signUpStatus để cập nhật giao diện
        signUpViewModel.signUpStatus.observe(this, status -> {
            Toast.makeText(SignupActivity.this, status, Toast.LENGTH_SHORT).show();
            if (status.equals("Đăng ký thành công!")) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignUp = findViewById(R.id.btn_sign_up);
    }

    private void setupClickListeners() {
        btnSignUp.setOnClickListener(v -> handleSignUp());

        findViewById(R.id.tv_login).setOnClickListener(v -> finish());
    }

    public void handleSignUp() {
        String fullName = Objects.requireNonNull(etFullName.getText()).toString().trim();
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(etConfirmPassword.getText()).toString().trim();

        if (InputValidator.validateSignUpInput(etFullName, etEmail, etPassword, etConfirmPassword)) {
            signUpViewModel.signUpUser(fullName, email, password);
        }
    }
}
