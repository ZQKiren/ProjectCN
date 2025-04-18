package com.example.myapp.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapp.R;
import com.example.myapp.data.Product;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> products;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public FavoriteAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);

        // Load product image with placeholder and error handling
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(context)
                    .load(product.getImages().get(0))
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .centerCrop()
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Set product name
        holder.tvProductName.setText(product.getName());

        // Format and display price
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        if (product.getOfferPercentage() > 0) {
            double originalPrice = product.getPrice();
            double discountedPrice = originalPrice * (1 - product.getOfferPercentage() / 100.0);

            // Set the discounted price
            holder.tvProductPrice.setText(formatter.format(discountedPrice));

            // Set the original price with strikethrough
            holder.tvProductOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvProductOriginalPrice.setText(formatter.format(originalPrice));
            holder.tvProductOriginalPrice.setPaintFlags(
                    holder.tvProductOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Show the discount percentage
            holder.tvProductOffer.setVisibility(View.VISIBLE);
            holder.tvProductOffer.setText(String.format("-%d%%", product.getOfferPercentage()));
        } else {
            // No discount, so show only the regular price
            holder.tvProductPrice.setText(formatter.format(product.getPrice()));
            holder.tvProductOriginalPrice.setVisibility(View.GONE);
            holder.tvProductOffer.setVisibility(View.GONE);
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvProductName;
        TextView tvProductPrice;
        TextView tvProductOriginalPrice;
        TextView tvProductOffer;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductOriginalPrice = itemView.findViewById(R.id.tvProductOriginalPrice);
            tvProductOffer = itemView.findViewById(R.id.tvProductOffer);
        }
    }
}