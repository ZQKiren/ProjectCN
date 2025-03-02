package com.example.myapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Voucher;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ManageVouchersAdapter extends RecyclerView.Adapter<ManageVouchersAdapter.VoucherViewHolder> {
    private final List<Voucher> vouchers;
    private final Context context;
    private final OnVoucherActionListener listener;

    public interface OnVoucherActionListener {
        void onVoucherEdit(Voucher voucher);
        void onVoucherDelete(Voucher voucher);
    }

    public ManageVouchersAdapter(Context context, List<Voucher> vouchers, OnVoucherActionListener listener) {
        this.context = context;
        this.vouchers = vouchers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);
        holder.bind(voucher);

        holder.itemView.setOnLongClickListener(v -> {
            showActionDialog(voucher);
            return true;
        });
    }

    private void showActionDialog(Voucher voucher) {
        new AlertDialog.Builder(context)
                .setItems(new String[]{"Cập nhật", "Xóa"}, (dialog, which) -> {
                    if (which == 0) {
                        listener.onVoucherEdit(voucher);
                    } else {
                        listener.onVoucherDelete(voucher);
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvDescription, tvDiscountAmount, tvMinSpend, tvPointsRequired;

        VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDiscountAmount = itemView.findViewById(R.id.tvDiscountAmount);
            tvMinSpend = itemView.findViewById(R.id.tvMinSpend);
            tvPointsRequired = itemView.findViewById(R.id.tvPointsRequired);
        }

        void bind(Voucher voucher) {
            tvCode.setText(voucher.getCode());
            tvDescription.setText(voucher.getDescription());
            tvDiscountAmount.setText(String.format("Giảm: %s", formatPrice(voucher.getDiscountAmount())));
            tvMinSpend.setText(String.format("Đơn tối thiểu: %s", formatPrice(voucher.getMinSpend())));
            tvPointsRequired.setText(String.format("%d điểm", voucher.getPointsRequired()));
        }

        private String formatPrice(double price) {
            return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price);
        }
    }
}
