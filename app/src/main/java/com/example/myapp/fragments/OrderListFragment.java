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
import com.example.myapp.adapters.OrderHistoryAdapter;
import com.example.myapp.data.Order;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class OrderListFragment extends Fragment implements OrderHistoryAdapter.OnCancelOrderListener {
    private RecyclerView recyclerView;
    private OrderHistoryAdapter adapter;
    private List<Order> orderList;
    private String userId;
    private String status;
    private ListenerRegistration orderListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            status = getArguments().getString("status");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        orderList = new ArrayList<>();
        adapter = new OrderHistoryAdapter(orderList, requireActivity(), this);
        recyclerView.setAdapter(adapter);

        loadOrders();
        return view;
    }

    @Override
    public void onCancelOrder(Order order) {
        FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("id", order.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentReference orderRef = querySnapshot.getDocuments().get(0).getReference();
                        String userId = order.getUserId();
                        int pointsToRefund = order.getEarnedPoints();

                        // Cập nhật trạng thái đơn hàng
                        WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        batch.update(orderRef,
                                "status", Order.OrderStatus.CANCELLED.name(),
                                "lastUpdated", System.currentTimeMillis()
                        );

                        // Hoàn điểm nếu đơn hàng có điểm thưởng
                        if (pointsToRefund > 0) {
                            DocumentReference userRef = FirebaseFirestore.getInstance()
                                    .collection("users").document(userId);

                            batch.update(userRef, "points",
                                    FieldValue.increment(-pointsToRefund));
                        }

                        batch.commit()
                                .addOnSuccessListener(v -> Toast.makeText(getContext(),
                                        "Đã hủy đơn hàng và hoàn lại " + pointsToRefund + " điểm",
                                        Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(),
                                        "Lỗi: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void loadOrders() {
        Query query = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userId", userId);

        if (status != null) {
            query = query.whereEqualTo("status", status);
        }

        // Đảm bảo thứ tự sắp xếp phù hợp với index
        orderListener = query
                .orderBy("orderTime", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("OrderListFragment", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        orderList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                orderList.add(order);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) {
            orderListener.remove();
        }
    }

}
