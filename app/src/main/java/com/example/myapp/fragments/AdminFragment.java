package com.example.myapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapp.R;
import com.example.myapp.data.User;
import com.example.myapp.viewmodel.ProfileViewModel;

public class AdminFragment extends Fragment {

    private ProfileViewModel profileViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        // Initialize ViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Observe user data and update UI based on role
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                setupUI(view, user);
            }
        });

        return view;
    }

    private void setupUI(View view, User user) {
        // Find buttons
        Button btnManageVouchers = view.findViewById(R.id.btn_manage_vouchers);
        Button btnManageProducts = view.findViewById(R.id.btn_manage_products);
        Button btnManageAccounts = view.findViewById(R.id.btn_manage_accounts);
        Button btnStatistics = view.findViewById(R.id.btn_statistics);
        Button btnManageOrders = view.findViewById(R.id.btn_manage_orders);// Thêm nút "Thống kê"

        // Check role and setup UI
        if (user.getRole() == User.Role.ADMIN) {
            // Show admin controls
            btnManageVouchers.setVisibility(View.VISIBLE);
            btnManageProducts.setVisibility(View.VISIBLE);
            btnManageAccounts.setVisibility(View.VISIBLE);
            btnStatistics.setVisibility(View.VISIBLE);
            btnManageOrders.setVisibility(View.VISIBLE);

            // Set button actions
            btnManageVouchers.setOnClickListener(v -> navigateToFragment(new ManageVouchersFragment()));
            btnManageProducts.setOnClickListener(v -> navigateToFragment(new ManageProductsFragment()));
            btnManageAccounts.setOnClickListener(v -> navigateToFragment(new ManageAccountsFragment()));
            btnStatistics.setOnClickListener(v -> navigateToFragment(new StatisticFragment()));
            btnManageOrders.setOnClickListener(v -> navigateToFragment(new ManageOrdersFragment()));

        } else {
            // Hide admin controls and show a message
            btnManageVouchers.setVisibility(View.GONE);
            btnManageProducts.setVisibility(View.GONE);
            btnManageAccounts.setVisibility(View.GONE);
            btnStatistics.setVisibility(View.GONE); // Ẩn nút "Thống kê"
            Toast.makeText(getContext(), "Bạn không có quyền truy cập vào khu vực quản trị", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }
}

