package com.example.myapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.example.myapp.R;
import com.example.myapp.data.User;
import com.example.myapp.fragments.ForgotPasswordFragment;
import com.example.myapp.utils.InputValidator;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle, btnFacebook;
    private TextView tvForgotPassword, tvSignUp;
    private FirebaseAuth mAuth;
    private LottieAnimationView loadingAnimation;
    private FirebaseFirestore db;
    private SignInClient signInClient;
    private CallbackManager callbackManager;

    private MaterialCheckBox cbRememberLogin;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";

    // Launcher for Google Sign-In
    private final ActivityResultLauncher<IntentSenderRequest> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                try {
                    if (result.getResultCode() == RESULT_OK) {
                        SignInCredential credential = signInClient.getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();
                        if (idToken != null) {
                            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                            signInWithGoogleCredential(firebaseCredential);
                        }
                    }
                } catch (Exception e) {
                    showError("Google Sign In error: " + e.getMessage());
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        signInClient = Identity.getSignInClient(this);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Only check for logged-in user if "Remember Login" was enabled
        if (sharedPreferences.getBoolean(KEY_REMEMBER_LOGIN, false)) {
            checkIfUserLoggedIn();
        } else {
            // If not remembering login, sign out any existing session
            FirebaseAuth.getInstance().signOut();
        }
    }

    private void checkIfUserLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        }
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google);
        btnFacebook = findViewById(R.id.btn_facebook);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvSignUp = findViewById(R.id.tv_sign_up);
        loadingAnimation = findViewById(R.id.loading_animation);
        cbRememberLogin = findViewById(R.id.cb_remember_login);

        cbRememberLogin.setChecked(sharedPreferences.getBoolean(KEY_REMEMBER_LOGIN, false));
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        btnGoogle.setOnClickListener(v -> handleGoogleSignIn());
        btnFacebook.setOnClickListener(v -> handleFacebookSignIn());
        tvForgotPassword.setOnClickListener(v -> navigateToForgotPassword());
        tvSignUp.setOnClickListener(v -> navigateToSignUp());
    }

    private void handleLogin() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (InputValidator.validateEmailAndPassword(etEmail, etPassword)) {
            btnLogin.setVisibility(View.GONE);
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        loadingAnimation.cancelAnimation();
                        loadingAnimation.setVisibility(View.GONE);
                        btnLogin.setVisibility(View.VISIBLE);
                        if (task.isSuccessful()) {
                            sharedPreferences.edit()
                                    .putBoolean(KEY_REMEMBER_LOGIN, cbRememberLogin.isChecked())
                                    .apply();
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Cập nhật trạng thái online
                                updateStatus(firebaseUser.getUid());

                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void checkIfUserExistsInFirestore(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        navigateToMain();
                    } else {
                        saveOrUpdateGoogleUserToFirestore(firebaseUser, this::navigateToMain);
                    }
                })
                .addOnFailureListener(e -> showError("Kiểm tra dữ liệu người dùng thất bại: " + e.getMessage()));
    }


    private void saveOrUpdateGoogleUserToFirestore(FirebaseUser firebaseUser, Runnable onSuccess) {
        if (firebaseUser == null) {
            Toast.makeText(this, "User data is null!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy thông tin người dùng
        String userId = firebaseUser.getUid();
        String fullName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String avatarUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";
        String phoneNumber = firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : "";

        // Tạo đối tượng User
        User user = new User(userId, fullName, email, avatarUrl, phoneNumber);

        // Cập nhật hoặc tạo mới
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save or update user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateStatus(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("status", "online")
                .addOnSuccessListener(aVoid -> Log.d("StatusUpdate", "User status updated to " + "online"))
                .addOnFailureListener(e -> Log.e("StatusUpdate", "Failed to update status: " + e.getMessage()));
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.error_color))
                .show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Xóa ngăn xếp
        intent.putExtra("loadFromProfileViewModel", true); // Gửi tín hiệu để MainActivity sử dụng ViewModel
        startActivity(intent);
        finish();
    }

    private void navigateToForgotPassword() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ForgotPasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
    }

    private void handleGoogleSignIn() {
        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        signInClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    try {
                        IntentSenderRequest intentSenderRequest =
                                new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender())
                                        .build();
                        signInLauncher.launch(intentSenderRequest);
                    } catch (Exception e) {
                        showError("Không thể khởi động Google Sign In: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> showError("Google Sign In thất bại: " + e.getMessage()));
    }

    private void signInWithGoogleCredential(AuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        sharedPreferences.edit()
                                .putBoolean(KEY_REMEMBER_LOGIN, cbRememberLogin.isChecked())
                                .apply();
                        checkIfUserExistsInFirestore(firebaseUser);
                    }
                })
                .addOnFailureListener(e ->
                        showError("Xác thực thất bại: " + e.getMessage()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Chỉ xử lý callback cho Facebook login
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleFacebookSignIn() {
        // Show loading
        loadingAnimation.setVisibility(View.VISIBLE);
        btnFacebook.setVisibility(View.GONE);

        // Set up Facebook Login callback
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        loadingAnimation.setVisibility(View.GONE);
                        btnFacebook.setVisibility(View.VISIBLE);
                        showError("Đăng nhập Facebook đã bị hủy");
                    }

                    @Override
                    public void onError(@NonNull FacebookException error) {
                        loadingAnimation.setVisibility(View.GONE);
                        btnFacebook.setVisibility(View.VISIBLE);
                        showError("Lỗi đăng nhập Facebook: " + error.getMessage());
                    }
                });

        // Request email and public profile permissions
        LoginManager.getInstance().logInWithReadPermissions(this,
                Arrays.asList("email", "public_profile"));
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        sharedPreferences.edit()
                                .putBoolean(KEY_REMEMBER_LOGIN, cbRememberLogin.isChecked())
                                .apply();
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save user to Firestore
                            saveFacebookUserToFirestore(user);
                        }
                    } else {
                        loadingAnimation.setVisibility(View.GONE);
                        btnFacebook.setVisibility(View.VISIBLE);
                        showError("Xác thực Facebook thất bại: " +
                                Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void saveFacebookUserToFirestore(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists, update status and navigate
                        updateStatus(userId);
                        navigateToMain();
                    } else {
                        // New user, create profile
                        User user = new User(
                                userId,
                                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "",
                                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "",
                                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                                firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : ""
                        );

                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    updateStatus(userId);
                                    navigateToMain();
                                })
                                .addOnFailureListener(e -> {
                                    loadingAnimation.setVisibility(View.GONE);
                                    btnFacebook.setVisibility(View.VISIBLE);
                                    showError("Lỗi lưu thông tin: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    loadingAnimation.setVisibility(View.GONE);
                    btnFacebook.setVisibility(View.VISIBLE);
                    showError("Lỗi kiểm tra thông tin: " + e.getMessage());
                });
    }


}