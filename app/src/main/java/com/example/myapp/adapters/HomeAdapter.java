package com.example.myapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.data.Product;
import com.example.myapp.fragments.ProductDetailFragment;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ProductViewHolder> {

    private final List<Product> productList;

    public HomeAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof FragmentActivity) {
                ProductDetailFragment fragment = ProductDetailFragment.newInstance(product);
                FragmentTransaction transaction = ((FragmentActivity) v.getContext())
                        .getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content_frame, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView productName;
        private final TextView productPrice;
        private final TextView productOriginalPrice;
        private final ImageView productImage;
        private final TextView productDiscount;
        private final ProgressBar loadingIndicator;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productOriginalPrice = itemView.findViewById(R.id.product_original_price);
            productImage = itemView.findViewById(R.id.product_image);
            productDiscount = itemView.findViewById(R.id.product_discount);
            loadingIndicator = itemView.findViewById(R.id.loading_indicator);
        }

        public void bind(Product product) {
            productName.setText(product.getName());

            // Tính toán giá sau khi giảm giá nếu có giảm giá
            double originalPrice = product.getPrice();
            double finalPrice = originalPrice;

            if (product.getOfferPercentage() > 0) {
                finalPrice = originalPrice * (1 - product.getOfferPercentage() / 100.0);

                // Hiển thị giá gốc (bị gạch ngang)
                productOriginalPrice.setVisibility(View.VISIBLE);
                productOriginalPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(originalPrice));

                // Hiển thị mức giảm giá
                productDiscount.setVisibility(View.VISIBLE);
                productDiscount.setText(String.format("Giảm %d%%", product.getOfferPercentage()));
            } else {
                // Ẩn giá gốc và giảm giá nếu không có giảm giá
                productOriginalPrice.setVisibility(View.GONE);
                productDiscount.setVisibility(View.GONE);
            }

            // Hiển thị giá đã giảm (hoặc giá gốc nếu không giảm giá)
            String priceFormatted = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(finalPrice);
            productPrice.setText(priceFormatted);

            // Hiển thị ảnh đầu tiên trong danh sách images nếu có
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                loadingIndicator.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(product.getImages().get(0))
                        .into(productImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                loadingIndicator.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
                                loadingIndicator.setVisibility(View.GONE);
                                productImage.setImageResource(R.drawable.ic_placeholder_image);
                            }
                        });
            } else {
                loadingIndicator.setVisibility(View.GONE);
                productImage.setImageResource(R.drawable.ic_placeholder_image);
            }
        }
    }
}