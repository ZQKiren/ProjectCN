package com.example.myapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Voucher;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.example.myapp.fragments.VoucherFragment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private final List<Voucher> vouchers;
    private final FirebaseFirestore db;
    private final String userId;
    private final int userPoints;
    private final Context context;

    public VoucherAdapter(Context context, List<Voucher> vouchers, String userId, int userPoints) {
        this.context = context;
        this.vouchers = vouchers;
        this.userId = userId;
        this.userPoints = userPoints;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);

        // Format currency
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String discountText = formatter.format(voucher.getDiscountAmount());
        String minSpendText = formatter.format(voucher.getMinSpend());

        // Set texts
        holder.tvDiscountAmount.setText(discountText);
        holder.tvDescription.setText(String.format("Giảm %s cho đơn từ %s",
                discountText, minSpendText));
        holder.tvPointsRequired.setText(voucher.getPointsRequired() + " điểm");

        // Format expiry date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String expiryDate = sdf.format(new Date(voucher.getExpiryDate()));
        holder.tvExpiry.setText("HSD: " + expiryDate);

        // Show code if voucher is owned
        if (voucher.getUserId() != null && voucher.getUserId().equals(userId)) {
            holder.tvCode.setVisibility(View.VISIBLE);
            holder.tvCode.setText(voucher.getCode());
            holder.btnRedeem.setVisibility(View.GONE);
        } else {
            holder.tvCode.setVisibility(View.GONE);
            holder.btnRedeem.setVisibility(View.VISIBLE);

            boolean canRedeem = userPoints >= voucher.getPointsRequired();
            holder.btnRedeem.setEnabled(canRedeem);
            holder.btnRedeem.setText(canRedeem ? "Đổi voucher" : "Không đủ điểm");

            holder.btnRedeem.setOnClickListener(v -> redeemVoucher(voucher));
        }
    }

    private void redeemVoucher(Voucher voucher) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Xác nhận đổi voucher")
                .setMessage(String.format("Bạn có muốn dùng %d điểm để đổi voucher này?",
                        voucher.getPointsRequired()))
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    WriteBatch batch = db.batch();

                    // Update user points
                    DocumentReference userRef = db.collection("users").document(userId);
                    batch.update(userRef, "points", userPoints - voucher.getPointsRequired());

                    // Create new voucher for user
                    voucher.setUserId(userId);
                    voucher.setUsed(false);
                    DocumentReference voucherRef = db.collection("user_vouchers").document();
                    batch.set(voucherRef, voucher);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Đổi voucher thành công!",
                                        Toast.LENGTH_SHORT).show();

                                // Xóa voucher này khỏi danh sách có thể đổi
                                vouchers.remove(voucher);
                                notifyDataSetChanged();

                                // Refresh Fragment để cập nhật cả hai danh sách
                                if (context instanceof VoucherFragment.VoucherRefreshListener) {
                                    ((VoucherFragment.VoucherRefreshListener) context).onVoucherRedeemed();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Lỗi: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return vouchers != null ? vouchers.size() : 0;
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiscountAmount, tvDescription, tvExpiry, tvCode, tvPointsRequired;
        MaterialButton btnRedeem;

        VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDiscountAmount = itemView.findViewById(R.id.tvDiscountAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvPointsRequired = itemView.findViewById(R.id.tvPointsRequired);
            btnRedeem = itemView.findViewById(R.id.btnRedeem);
        }
    }
}
