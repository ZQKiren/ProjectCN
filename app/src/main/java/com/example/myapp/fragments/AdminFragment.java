package com.example.myapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapp.R;
import com.example.myapp.data.User;
import com.example.myapp.viewmodel.ProfileViewModel;
import com.google.android.material.button.MaterialButton;

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
        MaterialButton btnManageVouchers = view.findViewById(R.id.btn_manage_vouchers);
        MaterialButton btnManageProducts = view.findViewById(R.id.btn_manage_products);
        MaterialButton btnManageAccounts = view.findViewById(R.id.btn_manage_accounts);
        MaterialButton btnStatistics = view.findViewById(R.id.btn_statistics);
        MaterialButton btnManageOrders = view.findViewById(R.id.btn_manage_orders);

        // Check role and setup UI
        if (user.getRole() == User.Role.ADMIN) {
            // Show admin controls
            btnManageVouchers.setVisibility(View.VISIBLE);
            btnManageProducts.setVisibility(View.VISIBLE);
            btnManageAccounts.setVisibility(View.VISIBLE);
            btnStatistics.setVisibility(View.VISIBLE);
            btnManageOrders.setVisibility(View.VISIBLE);

            // Set button actions with animations
            setupButtonWithAnimation(btnManageVouchers, new ManageVouchersFragment());
            setupButtonWithAnimation(btnManageProducts, new ManageProductsFragment());
            setupButtonWithAnimation(btnManageAccounts, new ManageAccountsFragment());
            setupButtonWithAnimation(btnStatistics, new StatisticFragment());
            setupButtonWithAnimation(btnManageOrders, new ManageOrdersFragment());

        } else {
            // Hide admin controls and show a message
            btnManageVouchers.setVisibility(View.GONE);
            btnManageProducts.setVisibility(View.GONE);
            btnManageAccounts.setVisibility(View.GONE);
            btnStatistics.setVisibility(View.GONE);
            btnManageOrders.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Bạn không có quyền truy cập vào khu vực quản trị", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtonWithAnimation(MaterialButton button, Fragment destinationFragment) {
        button.setOnClickListener(v -> {
            // Apply click animation
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() ->
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .withEndAction(() -> navigateToFragment(destinationFragment))
                                    .start())
                    .start();
        });
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
}