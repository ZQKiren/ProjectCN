package com.example.myapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.ManageOrdersAdapter;
import com.example.myapp.data.Notification;
import com.example.myapp.data.Order;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ManageOrdersFragment extends Fragment implements ManageOrdersAdapter.OnStatusUpdateListener {
    private RecyclerView recyclerView;
    private ManageOrdersAdapter adapter;
    private List<Order> orderList;
    private ListenerRegistration orderListener;
    private ListenerRegistration notificationListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderList = new ArrayList<>(); // Khởi tạo list

        loadOrders();
        setupNotificationListener();
        return view;
    }

    private void setupNotificationListener() {
        notificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("forAdmin", true)
                .whereEqualTo("type", "CANCELLED")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || !isAdded()) return;

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Notification notification = dc.getDocument().toObject(Notification.class);
                                if (!notification.isRead()) {
                                    showNotification(notification);
                                }
                            }
                        }
                    }
                });
    }

    private void showNotification(Notification notification) {
        try {
            View coordinatorLayout = requireView().findViewById(R.id.coordinator_layout);
            if (coordinatorLayout != null) {
                Snackbar.make(coordinatorLayout, notification.getMessage(), Snackbar.LENGTH_LONG)
//                        .setAction("Xem chi tiết", v -> {
//                            navigateToOrderDetail(notification.getOrderId());
//                            markNotificationAsRead(notification.getId());
//                        })
                        .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                        .show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to Toast if Snackbar fails
            Toast.makeText(getContext(), notification.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void markNotificationAsRead(String notificationId) {
        if (notificationId == null) {
            return; // Tránh crash khi ID null
        }
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnFailureListener(e -> {
                    // Log lỗi nếu cần
                    Toast.makeText(getContext(), "Không thể cập nhật trạng thái thông báo", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOrders() {
        // Thiết lập real-time listener
        orderListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .orderBy("orderTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(),
                                "Lỗi khi tải đơn hàng: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        orderList.clear();
                        orderList.addAll(snapshots.toObjects(Order.class));
                        if (adapter == null) {
                            adapter = new ManageOrdersAdapter(orderList, this);
                            recyclerView.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    public void onStatusUpdate(String orderId, Order.OrderStatus newStatus) {
        FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("id", orderId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        querySnapshot.getDocuments().get(0).getReference()
                                .update(
                                        "status", newStatus.name(),
                                        "lastUpdated", System.currentTimeMillis()
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(),
                                            "Cập nhật trạng thái thành công",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(),
                                            "Lỗi: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void navigateToOrderDetail(String orderId) {
        // Tạo và chuyển đến fragment chi tiết đơn hàng
        OrderDetailFragment detailFragment = OrderDetailFragment.newInstance(orderId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.content_frame, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) {
            orderListener.remove();
        }
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}
