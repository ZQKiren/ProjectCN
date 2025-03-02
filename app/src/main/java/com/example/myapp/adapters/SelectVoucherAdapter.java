package com.example.myapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Voucher;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SelectVoucherAdapter extends RecyclerView.Adapter<SelectVoucherAdapter.VoucherViewHolder> {
    private List<Voucher> vouchers;
    private double orderAmount;
    private OnVoucherSelectedListener listener;
    private int selectedPosition = -1;

    public SelectVoucherAdapter(List<Voucher> vouchers, double orderAmount, OnVoucherSelectedListener listener) {
        this.vouchers = vouchers;
        this.orderAmount = orderAmount;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);
        boolean isApplicable = orderAmount >= voucher.getMinSpend();

        // Format currency
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Set discount amount
        holder.tvDiscountAmount.setText(formatter.format(voucher.getDiscountAmount()));

        // Set description
        holder.tvDescription.setText(String.format("Giảm %s",
                formatter.format(voucher.getDiscountAmount())));

        // Set min spend
        holder.tvMinSpend.setText(String.format("Đơn tối thiểu %s",
                formatter.format(voucher.getMinSpend())));

        // Set expiry date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvExpiry.setText(String.format("HSD: %s",
                sdf.format(new Date(voucher.getExpiryDate()))));

        // Set radio button
        holder.radioButton.setChecked(position == selectedPosition);

        // Set alpha based on applicability
        float alpha = isApplicable ? 1.0f : 0.5f;
        holder.itemView.setAlpha(alpha);
        holder.radioButton.setEnabled(isApplicable);

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            if (isApplicable) {
                int previousSelected = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                listener.onVoucherSelected(voucher);
            }
        });
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    public interface OnVoucherSelectedListener {
        void onVoucherSelected(Voucher voucher);
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiscountAmount, tvDescription, tvMinSpend, tvExpiry;
        RadioButton radioButton;

        VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDiscountAmount = itemView.findViewById(R.id.tvDiscountAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvMinSpend = itemView.findViewById(R.id.tvMinSpend);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            radioButton = itemView.findViewById(R.id.radioButton);
        }
    }
}
