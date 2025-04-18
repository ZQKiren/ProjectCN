package com.example.myapp.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Order;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class StatisticAdapter extends RecyclerView.Adapter<StatisticAdapter.OrderViewHolder> {

    private final List<Order> orders;
    private final Context context;

    // Constructor receiving order list and context
    public StatisticAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_statistic, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        // Format order date
        String orderDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(order.getOrderTime());

        // Format total amount
        String totalAmount = new DecimalFormat("#,### VND").format(order.getTotalAmount());

        // Set data to ViewHolder
        holder.tvOrderId.setText(String.format("Mã đơn: %s",
                (order.getId() != null ? order.getId() : "Không rõ")));
        holder.tvOrderDate.setText(String.format("Ngày đặt: %s", orderDate));
        holder.tvOrderTotal.setText(String.format("Tổng tiền: %s", totalAmount));
        holder.tvOrderUserName.setText(String.format("Người dùng: %s", order.getCustomerName()));
        holder.tvOrderUserEmail.setText(String.format("SĐT: %s",
                (order.getCustomerPhone() != null ? order.getCustomerPhone() : "Không rõ")));

        // Set order status with appropriate styling
        holder.tvOrderStatus.setText(order.getStatus());

        // Set background color based on order status
        int colorResId;
        switch (order.getStatus().toLowerCase()) {
            case "completed":
            case "delivered":
            case "thành công":
            case "đã giao hàng":
                colorResId = R.attr.successColor;
                break;
            case "pending":
            case "processing":
            case "đang xử lý":
            case "chờ xác nhận":
                colorResId = R.attr.warningColor;
                break;
            case "cancelled":
            case "đã hủy":
                colorResId = R.attr.errorColor;
                break;
            default:
                colorResId = R.attr.primaryColor;
                break;
        }

        // Apply the background tint based on status
        //ColorStateList colorStateList = ColorStateList.valueOf(
                //context.getResources().getColor(colorResId, context.getTheme()));
        //holder.tvOrderStatus.setBackgroundTintList(colorStateList);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvOrderId, tvOrderDate, tvOrderTotal, tvOrderUserName, tvOrderUserEmail, tvOrderStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderUserName = itemView.findViewById(R.id.tvOrderUserName);
            tvOrderUserEmail = itemView.findViewById(R.id.tvOrderUserPhone);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
        }
    }
}