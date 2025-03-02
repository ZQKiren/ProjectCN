package com.example.myapp.fragments;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.VoucherAdapter;
import com.example.myapp.data.Voucher;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AvailableVouchersFragment extends Fragment {
    private static final String TAG = "AvailableVouchersFragment";
    private RecyclerView recyclerView;
    private View emptyState;
    private MaterialButton btnShopNow;
    private List<Voucher> voucherList;
    private VoucherAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    private int userPoints;

    public static AvailableVouchersFragment newInstance(String userId, int userPoints) {
        AvailableVouchersFragment fragment = new AvailableVouchersFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putInt("userPoints", userPoints);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            userPoints = getArguments().getInt("userPoints");
        }
        db = FirebaseFirestore.getInstance();
        voucherList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_available_vouchers, container, false);
        initViews(view);
        loadAvailableVouchers();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        btnShopNow = view.findViewById(R.id.btnShopNow);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        btnShopNow.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new HomeFragment())
                    .commit();
        });
    }

    private void loadAvailableVouchers() {
        // Đầu tiên lấy danh sách voucher đã đổi của user
        db.collection("user_vouchers")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(userVoucherSnapshots -> {
                    // Tạo list các mã voucher đã đổi
                    List<String> redeemedVoucherCodes = new ArrayList<>();
                    for (DocumentSnapshot doc : userVoucherSnapshots) {
                        Voucher voucher = doc.toObject(Voucher.class);
                        if (voucher != null && voucher.getCode() != null) {
                            redeemedVoucherCodes.add(voucher.getCode());
                        }
                    }

                    // Load các voucher có thể đổi
                    db.collection("vouchers")
                            .whereGreaterThan("expiryDate", System.currentTimeMillis())
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                voucherList.clear();
                                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                    Voucher voucher = doc.toObject(Voucher.class);
                                    if (voucher != null) {
                                        voucher.setId(doc.getId());
                                        // Chỉ thêm voucher chưa đổi
                                        if (!redeemedVoucherCodes.contains(voucher.getCode())) {
                                            voucherList.add(voucher);
                                        }
                                    }
                                }
                                updateUI();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading available vouchers", e);
                                showError("Không thể tải danh sách voucher");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user vouchers", e);
                    showError("Không thể kiểm tra voucher đã đổi");
                });
    }

    private void updateUI() {
        if (voucherList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            adapter = new VoucherAdapter(requireContext(), voucherList, userId, userPoints);
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}

