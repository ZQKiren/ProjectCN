package com.example.myapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.view.MenuItem;

import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.R;
import com.example.myapp.data.Order;
import com.example.myapp.data.User;
import com.example.myapp.fragments.AdminFragment;
import com.example.myapp.fragments.CartFragment;
import com.example.myapp.fragments.HomeFragment;
import com.example.myapp.fragments.OrderSuccessFragment;
import com.example.myapp.fragments.ProfileFragment;
import com.example.myapp.fragments.SearchFragment;
import com.example.myapp.helpers.VNPayHelper;
import com.example.myapp.utils.ThemeUtils;
import com.example.myapp.viewmodel.ProfileViewModel;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


public class MainActivity extends AppCompatActivity {

    private ProfileViewModel profileViewModel;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);

        ThemeUtils.initTheme(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeUserRole();
    }

    private void observeUserRole() {
        // Tải dữ liệu người dùng từ Firestore ngay khi mở ứng dụng
        profileViewModel.refreshUserData(); // Đảm bảo dữ liệu được cập nhật từ Firestore
        profileViewModel.getUserData().observe(this, user -> {
            if (user != null) {
                updateAdminMenuVisibility(user);
            }
        });
    }

    private void updateAdminMenuVisibility(User user) {
        // Hiển thị hoặc ẩn mục menu "Quản trị" dựa trên vai trò của người dùng
        bottomNavigationView.getMenu().findItem(R.id.nav_admin).setVisible(user.getRole() == User.Role.ADMIN);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;

        if (item.getItemId() == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (item.getItemId() == R.id.nav_search) {
            selectedFragment = new SearchFragment();
        } else if (item.getItemId() == R.id.nav_cart) {
            selectedFragment = new CartFragment();
        } else if (item.getItemId() == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        } else if (item.getItemId() == R.id.nav_admin) {
            selectedFragment = new AdminFragment();
        }
        return loadFragment(selectedFragment);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out) // Hiệu ứng mượt
                    .replace(R.id.content_frame, fragment)
                    .commit();
            return true;
        }
        return false;
    }
    
    public void setBottomNavigationViewSelected(int itemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(itemId);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        Uri data = intent.getData();
        if (data != null && "myapp".equals(data.getScheme())) {
            VNPayHelper.PaymentResult result = VNPayHelper.handlePaymentResult(data);

            if (result.isSuccess()) {
                // Cập nhật trạng thái đơn hàng thành công
                FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(result.getOrderId())
                        .update(
                                "orderStatus", Order.OrderStatus.PENDING.name(),
                                "status", Order.OrderStatus.PENDING.name(),
                                "lastUpdated", System.currentTimeMillis()
                        )
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                            // Chuyển đến màn hình thành công
                            navigateToOrderSuccess(result.getOrderId());
                        });
            } else {
                // Thanh toán thất bại
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
                // Có thể cập nhật trạng thái đơn hàng thành thất bại hoặc hủy
                FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(result.getOrderId())
                        .update(
                                "orderStatus", Order.OrderStatus.CANCELLED.name(),
                                "status", Order.OrderStatus.CANCELLED.name(),
                                "cancelReason", "Thanh toán thất bại",
                                "lastUpdated", System.currentTimeMillis()
                        );
            }
        }
    }

    private void navigateToOrderSuccess(String orderId) {
        // Chuyển đến OrderSuccessFragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, OrderSuccessFragment.newInstance(orderId))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Kiểm tra nếu không phải configuration change và không lưu phiên đăng nhập
        if (!isChangingConfigurations() && !sharedPreferences.getBoolean(KEY_REMEMBER_LOGIN, false)) {
            // Đăng xuất khỏi Firebase Auth
            FirebaseAuth.getInstance().signOut();

            // Đăng xuất khỏi Facebook nếu đã tích hợp
            LoginManager.getInstance().logOut();

            // Đăng xuất khỏi Google Sign-In nếu đã tích hợp
            Identity.getSignInClient(this).signOut();

            // Chuyển về màn hình đăng nhập
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
