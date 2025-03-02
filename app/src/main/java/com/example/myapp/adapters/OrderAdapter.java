package com.example.myapp.adapters;

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

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderItemViewHolder> {

    private final List<Product> products;

    public OrderAdapter(List<Product> products) {
        this.products = products;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        Product product = products.get(position);

        // Tính giá sau khi giảm giá (giá đã chiết khấu)
        double discountedPrice = product.getPrice() * (1 - product.getOfferPercentage() / 100.0);

        // Lấy tổng giá của sản phẩm (giá đã chiết khấu * số lượng)
        double totalProductPrice = discountedPrice * product.getQuantity();

        // Hiển thị thông tin
        holder.productName.setText(product.getName());

        // Hiển thị tổng giá của sản phẩm (đã chiết khấu và nhân số lượng)
        holder.productPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(totalProductPrice));

        // Hiển thị số lượng sản phẩm
        holder.productQuantity.setText(String.format("x%d", product.getQuantity()));

        // Hiển thị hình ảnh sản phẩm
        Glide.with(holder.itemView.getContext())
                .load(product.getImages().get(0))
                .placeholder(R.drawable.ic_placeholder_image)
                .into(holder.productImage);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productQuantity;
        ImageView productImage;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.tvOrderProductName);
            productPrice = itemView.findViewById(R.id.tvOrderProductPrice);
            productImage = itemView.findViewById(R.id.imgOrderProduct);
            productQuantity = itemView.findViewById(R.id.tvOrderProductQuantity);
        }
    }
}
