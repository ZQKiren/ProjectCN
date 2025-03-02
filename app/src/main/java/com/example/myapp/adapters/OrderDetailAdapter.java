package com.example.myapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.myapp.R;
import com.example.myapp.data.Product;

import java.text.NumberFormat;
import java.util.Locale;

public class OrderDetailAdapter extends ListAdapter<Product, OrderDetailAdapter.OrderItemViewHolder> {

    public OrderDetailAdapter() {
        super(new DiffUtil.ItemCallback<Product>() {
            @Override
            public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
                return oldItem.getQuantity() == newItem.getQuantity()
                        && oldItem.getPrice() == newItem.getPrice();
            }
        });
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProduct;
        private final TextView tvProductName;
        private final TextView tvProductPrice;
        private final TextView tvQuantity;
        private final TextView tvSubtotal;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
        }

        void bind(Product product) {
            tvProductName.setText(product.getName());
            tvQuantity.setText("x" + product.getQuantity());

            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            // Tính giá sau giảm giá
            double discountedPrice = product.getPrice() * (1 - product.getOfferPercentage() / 100.0);
            tvProductPrice.setText(currencyFormatter.format(discountedPrice));

            // Tính tổng tiền cho sản phẩm
            double subtotal = discountedPrice * product.getQuantity();
            tvSubtotal.setText(currencyFormatter.format(subtotal));

            // Load ảnh sản phẩm
            if (!product.getImages().isEmpty()) {
                Glide.with(ivProduct)
                        .load(product.getImages().get(0))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_placeholder_image)
                        .into(ivProduct);
            }
        }
    }
}
