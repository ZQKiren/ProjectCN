package com.example.myapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Order;
import com.example.myapp.fragments.OrderDetailFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    public interface OnCancelOrderListener {
        void onCancelOrder(Order order);
    }

    private final List<Order> orders;
    private final FragmentActivity activity;
    private final OnCancelOrderListener cancelListener;

    public OrderHistoryAdapter(List<Order> orders, FragmentActivity activity, OnCancelOrderListener cancelListener) {
        this.orders = orders;
        this.activity = activity;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String orderDate = order.getOrderTime() != 0 ?
                sdf.format(order.getOrderTime()) : "Không rõ ngày";

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String totalAmount = order.getTotalAmount() != 0 ?
                currencyFormatter.format(order.getTotalAmount()) : "0đ";

        holder.tvOrderId.setText(String.format("#%s", order.getId()));
        holder.tvOrderDate.setText(String.format("Ngày đặt: %s", orderDate));
        holder.tvOrderStatus.setText(Order.OrderStatus.fromString(order.getStatus()).getDisplayText());
        holder.tvOrderTotal.setText(String.format("Tổng tiền: %s", totalAmount));

        // Set status color
        setStatusColor(holder.tvOrderStatus, Order.OrderStatus.fromString(order.getStatus()));

        holder.cardView.setOnClickListener(v -> navigateToOrderDetail(order.getId()));

        // Check cancel button visibility
        Order.OrderStatus status = Order.OrderStatus.fromString(order.getStatus());
        if (status == Order.OrderStatus.PENDING || status == Order.OrderStatus.CONFIRMED) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> showCancelDialog(order));
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }
    }

    private void showCancelDialog(Order order) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_cancel_order, null);
        RadioGroup rgCancelReasons = dialogView.findViewById(R.id.rgCancelReasons);
        TextInputLayout tilOtherReason = dialogView.findViewById(R.id.tilOtherReason);
        TextInputEditText etOtherReason = dialogView.findViewById(R.id.etOtherReason);

        // Lắng nghe sự kiện chọn radio button
        rgCancelReasons.setOnCheckedChangeListener((group, checkedId) -> {
            tilOtherReason.setVisibility(
                    checkedId == R.id.rbReasonOther ? View.VISIBLE : View.GONE
            );
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Hủy đơn hàng")
                .setView(dialogView)
                .setPositiveButton("Hủy đơn", null) // Set null để xử lý riêng
                .setNegativeButton("Không", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                // Lấy lý do hủy đơn
                String reason;
                int selectedId = rgCancelReasons.getCheckedRadioButtonId();

                if (selectedId == -1) {
                    Toast.makeText(activity, "Vui lòng chọn lý do hủy đơn", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedId == R.id.rbReasonOther) {
                    reason = Objects.requireNonNull(etOtherReason.getText()).toString().trim();
                    if (reason.isEmpty()) {
                        tilOtherReason.setError("Vui lòng nhập lý do");
                        return;
                    }
                } else {
                    RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                    reason = selectedRadioButton.getText().toString();
                }

                order.setCancelReason(reason);
                cancelOrder(order);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void cancelOrder(Order order) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Tìm document của đơn hàng
        db.collection("orders")
                .whereEqualTo("id", order.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentReference orderRef = querySnapshot.getDocuments().get(0).getReference();
                        String userId = order.getUserId();
                        int pointsToRefund = order.getEarnedPoints();


                        WriteBatch batch = db.batch();


                        batch.update(orderRef,
                                "status", Order.OrderStatus.CANCELLED.name(),
                                "cancelReason", order.getCancelReason(),
                                "lastUpdated", System.currentTimeMillis()
                        );


                        if (pointsToRefund > 0) {
                            DocumentReference userRef = db.collection("users").document(userId);
                            batch.update(userRef, "points", FieldValue.increment(-pointsToRefund));
                        }


                        if (order.getVoucherDiscount() > 0) {

                            db.collection("user_vouchers")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("used", true)
                                    .get()
                                    .addOnSuccessListener(voucherSnapshot -> {
                                        if (!voucherSnapshot.isEmpty()) {

                                            DocumentReference voucherRef = voucherSnapshot.getDocuments().get(0).getReference();
                                            batch.update(voucherRef, "used", false);
                                        }

                                        String notificationId = db.collection("notifications").document().getId();

                                        Map<String, Object> notification = new HashMap<>();
                                        notification.put("id", notificationId); // Thêm ID vào data
                                        notification.put("title", "Đơn hàng bị hủy");
                                        notification.put("message", String.format("Đơn hàng #%s đã bị hủy\nLý do: %s",
                                                order.getId(), order.getCancelReason()));
                                        notification.put("type", "CANCELLED");
                                        notification.put("orderId", order.getId());
                                        notification.put("timestamp", FieldValue.serverTimestamp());
                                        notification.put("isRead", false);
                                        notification.put("forAdmin", true);

                                        batch.set(db.collection("notifications").document(notificationId), notification);

                                        // Thực hiện tất cả các thao tác
                                        batch.commit()
                                                .addOnSuccessListener(v -> {
                                                    String message = "Đã hủy đơn hàng";
                                                    if (pointsToRefund > 0) {
                                                        message += String.format(" và hoàn lại %d điểm", pointsToRefund);
                                                    }
                                                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(activity, "Lỗi: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show()
                                                );
                                    });
                        } else {
                            // Nếu không có voucher thì commit batch luôn
                            // 4. Tạo thông báo
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("title", "Đơn hàng bị hủy");
                            notification.put("message", String.format("Đơn hàng #%s đã bị hủy\nLý do: %s",
                                    order.getId(), order.getCancelReason()));
                            notification.put("type", "CANCELLED");
                            notification.put("orderId", order.getId());
                            notification.put("timestamp", FieldValue.serverTimestamp());
                            notification.put("isRead", false);
                            notification.put("forAdmin", true);

                            batch.set(db.collection("notifications").document(), notification);

                            batch.commit()
                                    .addOnSuccessListener(v -> {
                                        String message = "Đã hủy đơn hàng";
                                        if (pointsToRefund > 0) {
                                            message += String.format(" và hoàn lại %d điểm", pointsToRefund);
                                        }
                                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(activity, "Lỗi: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show()
                                    );
                        }
                    }
                });
    }

    private void setStatusColor(TextView tvStatus, Order.OrderStatus status) {
        int colorResId = switch (status) {
            case PENDING -> R.color.status_pending;
            case PROCESSING -> R.color.status_processing;
            case CONFIRMED -> R.color.status_confirmed;    // Màu xanh lục cho Đã xác nhận
            case SHIPPING -> R.color.status_shipping;
            case DELIVERED -> R.color.status_delivered;
            case CANCELLED -> R.color.status_cancelled;
            default -> R.color.status_default;
        };
        tvStatus.setTextColor(tvStatus.getContext().getResources().getColor(colorResId));
    }

    private void navigateToOrderDetail(String orderId) {
        OrderDetailFragment detailFragment = OrderDetailFragment.newInstance(orderId);
        activity.getSupportFragmentManager()
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
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvOrderId, tvOrderDate, tvOrderStatus, tvOrderTotal;
        private final Button btnCancel;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}