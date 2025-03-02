package com.example.myapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Order;

import java.util.List;

public class ManageOrdersAdapter extends RecyclerView.Adapter<ManageOrdersAdapter.OrderViewHolder> {
    private final List<Order> orders;
    private final OnStatusUpdateListener listener;

    public interface OnStatusUpdateListener {
        void onStatusUpdate(String orderId, Order.OrderStatus newStatus);
    }

    public ManageOrdersAdapter(List<Order> orders, OnStatusUpdateListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_orders, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.tvOrderId.setText("Đơn hàng #" + order.getId());

        ArrayAdapter<Order.OrderStatus> statusAdapter = new ArrayAdapter<>(
                holder.itemView.getContext(),
                android.R.layout.simple_spinner_item,
                Order.OrderStatus.values()
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        holder.spinnerStatus.setAdapter(statusAdapter);

        // Lấy vị trí của status hiện tại trong enum
        int currentStatusPosition = -1;
        for(int i = 0; i < Order.OrderStatus.values().length; i++) {
            if(Order.OrderStatus.values()[i].name().equals(order.getStatus())) {
                currentStatusPosition = i;
                break;
            }
        }
        holder.spinnerStatus.setSelection(currentStatusPosition);

        holder.spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Order.OrderStatus newStatus = Order.OrderStatus.values()[position];
                if (!newStatus.name().equals(order.getStatus())) {
                    listener.onStatusUpdate(order.getId(), newStatus);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId;
        Spinner spinnerStatus;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            spinnerStatus = itemView.findViewById(R.id.spinnerStatus);
        }
    }
}