package com.example.myapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Order;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class StatisticAdapter extends RecyclerView.Adapter<StatisticAdapter.OrderViewHolder> {

    private final List<Order> orders; // Danh sách đơn hàng

    // Constructor nhận danh sách đơn hàng
    public StatisticAdapter(List<Order> orders) {
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

        // Định dạng ngày tháng
        String orderDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(order.getOrderTime());

        // Định dạng số tiền
        String totalAmount = new DecimalFormat("#,### VND").format(order.getTotalAmount());

        // Gán dữ liệu vào ViewHolder
        holder.tvOrderId.setText("Mã đơn: " + (order.getId() != null ? order.getId() : "Không rõ"));
        holder.tvOrderDate.setText("Ngày đặt: " + orderDate);
        holder.tvOrderTotal.setText("Tổng tiền: " + totalAmount);
        holder.tvOrderUserName.setText("Người dùng: " + order.getCustomerName());
        holder.tvOrderUserEmail.setText("SĐT: " + (order.getCustomerPhone() != null ? order.getCustomerPhone() : "Không rõ"));
        holder.tvOrderStatus.setText("Trạng thái: " + order.getStatus());
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
