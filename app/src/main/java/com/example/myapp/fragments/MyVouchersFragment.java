package com.example.myapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

// MyVouchersFragment.java
public class MyVouchersFragment extends Fragment {
    private static final String TAG = "MyVouchersFragment";
    private RecyclerView recyclerView;
    private View emptyState;
    private MaterialButton btnShopNow;
    private List<Voucher> myVoucherList;
    private VoucherAdapter adapter;
    private FirebaseFirestore db;
    private String userId;

    public static MyVouchersFragment newInstance(String userId) {
        MyVouchersFragment fragment = new MyVouchersFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }
        db = FirebaseFirestore.getInstance();
        myVoucherList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_available_vouchers, container, false);
        initViews(view);
        loadMyVouchers();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        btnShopNow = view.findViewById(R.id.btnShopNow);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        btnShopNow.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new HomeFragment())
                .commit());
    }

    private void loadMyVouchers() {
        db.collection("user_vouchers")
                .whereEqualTo("userId", userId)
                .whereEqualTo("used", false)  // Thêm điều kiện này để chỉ lấy voucher chưa sử dụng
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myVoucherList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Voucher voucher = doc.toObject(Voucher.class);
                        if (voucher != null) {
                            voucher.setId(doc.getId());
                            myVoucherList.add(voucher);
                        }
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user vouchers", e);
                    showError();
                });
    }

    private void updateUI() {
        if (myVoucherList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            adapter = new VoucherAdapter(requireContext(), myVoucherList, userId, 0);
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showError() {
        Toast.makeText(requireContext(), "Không thể tải voucher của bạn", Toast.LENGTH_SHORT).show();
    }
}
