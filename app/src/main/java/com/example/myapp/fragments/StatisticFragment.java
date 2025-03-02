package com.example.myapp.fragments;

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
import com.example.myapp.adapters.StatisticAdapter;
import com.example.myapp.data.Order;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StatisticFragment extends Fragment {

    private RecyclerView recyclerView;
    private StatisticAdapter adapter;
    private List<Order> orderList;
    private FirebaseFirestore firestore;
    private TextView tvTotalOrders, tvTotalAmount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistic, container, false);

        recyclerView = view.findViewById(R.id.adminOrderRecyclerView);
        tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderList = new ArrayList<>();
        adapter = new StatisticAdapter(orderList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        loadAllOrders();

        return view;
    }

    private void loadAllOrders() {
        firestore.collection("orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        orderList.clear();
                        orderList.addAll(queryDocumentSnapshots.toObjects(Order.class));
                        adapter.notifyDataSetChanged();

                        // Tính tổng số đơn hàng và tổng tiền
                        int totalOrders = orderList.size();
                        double totalAmount = 0;
                        for (Order order : orderList) {
                            totalAmount += order.getTotalAmount();
                        }

                        tvTotalOrders.setText("Tổng số đơn hàng: " + totalOrders);
                        tvTotalAmount.setText("Tổng tiền: " + new DecimalFormat("#,### VND").format(totalAmount));
                    } else {
                        Toast.makeText(getContext(), "Không có đơn hàng nào.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("StatisticFragment", "Error fetching orders", e);
                    Toast.makeText(getContext(), "Lỗi khi tải danh sách đơn hàng.", Toast.LENGTH_SHORT).show();
                });
    }
}
