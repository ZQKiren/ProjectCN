package com.example.myapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapp.R;
import com.example.myapp.adapters.VoucherAdapter;
import com.example.myapp.adapters.VoucherPagerAdapter;
import com.example.myapp.data.Voucher;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class VoucherFragment extends Fragment {

    private static final String TAG = "VoucherFragment";
    private List<Voucher> voucherList;
    private FirebaseFirestore db;
    private TextView tvUserPoints;
    private VoucherAdapter myVouchersAdapter;
    private List<Voucher> myVoucherList;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private int userPoints = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voucher, container, false);
        Log.d(TAG, "Fragment initialized");

        initViews(view);
        setupToolbar(view);
        loadUserPoints();

        return view;
    }

    private void initViews(View view) {
        db = FirebaseFirestore.getInstance();
        voucherList = new ArrayList<>();
        myVoucherList = new ArrayList<>();

        // Khởi tạo views
        tvUserPoints = view.findViewById(R.id.tvUserPoints);
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);

        // Sau khi đã tìm tabLayout mới add tabs
        TabLayout.Tab availableTab = tabLayout.newTab().setText("Voucher có thể đổi");
        TabLayout.Tab myVouchersTab = tabLayout.newTab().setText("Voucher của tôi");
        tabLayout.addTab(availableTab);
        tabLayout.addTab(myVouchersTab);
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadUserPoints() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long points = documentSnapshot.getLong("points");
                        userPoints = points != null ? points.intValue() : 0;
                        tvUserPoints.setText(String.valueOf(userPoints));
                        setupViewPager(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading points", e);
                    showError();
                });
    }

    private void setupViewPager(String userId) {
        VoucherPagerAdapter adapter = new VoucherPagerAdapter(requireActivity(), userId, userPoints);
        viewPager.setAdapter(adapter);

        // Link TabLayout with ViewPager2
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Voucher có thể đổi");
                    } else {
                        tab.setText("Voucher của tôi");
                    }
                }
        );
        mediator.attach();
    }

    public interface VoucherRefreshListener {
        void onVoucherRedeemed();
    }

    private void showError() {
        Toast.makeText(requireContext(), "Không thể tải điểm tích lũy", Toast.LENGTH_SHORT).show();
    }
}
