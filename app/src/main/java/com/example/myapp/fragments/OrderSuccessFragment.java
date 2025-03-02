package com.example.myapp.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.myapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderSuccessFragment extends Fragment {

    private static final String ARG_ORDER_ID = "order_id";
    private String orderId;
    private TextView tvOrderId, tvPointsEarned;
    private LottieAnimationView successAnimation;
    private MaterialButton btnBackToHome, btnViewOrder;
    private FirebaseFirestore db;

    public OrderSuccessFragment() {}

    public static OrderSuccessFragment newInstance(String orderId) {
        OrderSuccessFragment fragment = new OrderSuccessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString(ARG_ORDER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_success, container, false);

        initViews(view);
        loadOrderDetails();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        tvOrderId = view.findViewById(R.id.tvOrderId);
        tvPointsEarned = view.findViewById(R.id.tvPointsEarned);
        successAnimation = view.findViewById(R.id.successAnimation);
        btnBackToHome = view.findViewById(R.id.btnBackToHome);
        btnViewOrder = view.findViewById(R.id.btnViewOrder);

        // Khởi tạo animation
        successAnimation.setAnimation(R.raw.success_animation);
        successAnimation.playAnimation();

        // Hiển thị mã đơn hàng
        tvOrderId.setText(getString(R.string.order_id_format, orderId));
    }

    private void loadOrderDetails() {
        db.collection("orders")
                .whereEqualTo("id", orderId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot orderDoc = queryDocumentSnapshots.getDocuments().get(0);
                        double totalAmount = orderDoc.getDouble("totalAmount");

                        // Tính điểm: 1 điểm cho mỗi 100,000 VND
                        int pointsEarned = (int)(totalAmount / 100000);

                        // Hiển thị điểm tích lũy
                        tvPointsEarned.setText(getString(R.string.points_earned_format, pointsEarned));
                        tvPointsEarned.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Không thể tải thông tin đơn hàng",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        // Quay về trang chủ
        btnBackToHome.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new HomeFragment())
                    .commit();
        });

        btnViewOrder.setOnClickListener(v -> {
            OrderDetailFragment detailFragment = OrderDetailFragment.newInstance(orderId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
}