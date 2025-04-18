package com.example.myapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.StatisticAdapter;
import com.example.myapp.data.Order;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StatisticFragment extends Fragment {

    private RecyclerView recyclerView;
    private StatisticAdapter adapter;
    private List<Order> orderList;
    private FirebaseFirestore firestore;
    private TextView tvTotalOrders, tvTotalAmount;
    private ProgressBar progressBar;

    private static final String TAG = "StatisticFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistic, container, false);

        // Initialize views
        initViews(view);

        // Setup toolbar
        setupToolbar(view);

        // Setup RecyclerView
        setupRecyclerView();

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Load data
        loadAllOrders();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.adminOrderRecyclerView);
        tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);

        // Add progress bar if not already in layout
        progressBar = view.findViewById(R.id.progressBar);
        if (progressBar == null) {
            // You might want to add a ProgressBar to your layout
            // For now, we'll handle the null case gracefully
        }
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderList = new ArrayList<>();
        adapter = new StatisticAdapter(requireContext(), orderList);
        recyclerView.setAdapter(adapter);

        // Add item decoration for spacing if needed
        // recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    private void loadAllOrders() {
        // Show loading state
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        firestore.collection("orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        orderList.clear();

                        // Process each order document
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Order order = document.toObject(Order.class);
                            // Make sure the order ID is set from document ID
                            if (order.getId() == null) {
                                order.setId(document.getId());
                            }
                            orderList.add(order);
                        }

                        adapter.notifyDataSetChanged();

                        // Calculate statistics
                        calculateStatistics();
                    } else {
                        showEmptyState("Không có đơn hàng nào.");
                    }

                    // Hide loading state
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching orders", e);
                    Toast.makeText(getContext(), "Lỗi khi tải danh sách đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    // Hide loading state
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void calculateStatistics() {
        int totalOrders = orderList.size();
        double totalAmount = 0;

        for (Order order : orderList) {
            totalAmount += order.getTotalAmount();
        }

        // Format and display totals
        DecimalFormat formatter = new DecimalFormat("#,### VND");
        tvTotalOrders.setText(String.format("Tổng số đơn hàng: %d", totalOrders));
        tvTotalAmount.setText(String.format("Tổng tiền: %s", formatter.format(totalAmount)));
    }

    private void showEmptyState(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        tvTotalOrders.setText("Tổng số đơn hàng: 0");
        tvTotalAmount.setText("Tổng tiền: 0 VND");
    }
}