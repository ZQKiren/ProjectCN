package com.example.myapp.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.myapp.R;
import com.example.myapp.data.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<Product> cartItems;
    private final FirebaseFirestore db;
    private final String userId;
    private final HashMap<String, Integer> temporaryQuantities = new HashMap<>();
    private final CartItemListener cartItemListener;
    private static final String PREF_NAME = "CartPreferences";
    private final SharedPreferences preferences;

    public interface CartItemListener {
        void onQuantityChanged(String productId, int newQuantity, double newPrice);
        void onItemRemoved(int position, Product product);
    }

    public CartAdapter(List<Product> cartItems, String userId, CartItemListener listener, Context context) {
        this.cartItems = cartItems;
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
        this.cartItemListener = listener;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Khởi tạo số lượng từ SharedPreferences
        for (Product product : cartItems) {
            int savedQuantity = preferences.getInt(product.getId(), 1);
            temporaryQuantities.put(product.getId(), savedQuantity);
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        Product product = cartItems.get(position);
        Context context = holder.itemView.getContext();

        // Kiểm tra productId
        String productId = product.getId();
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(context, "Sản phẩm không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy số lượng từ SharedPreferences hoặc mặc định là 1
        int currentQuantity = preferences.getInt(productId, 1);
        temporaryQuantities.put(productId, currentQuantity);

        holder.tvProductName.setText(product.getName());
        holder.tvQuantity.setText(String.valueOf(currentQuantity));

        // Tính giá dựa trên số lượng hiện tại
        double finalPrice = calculateFinalPrice(product);
        double totalPrice = finalPrice * currentQuantity;
        holder.tvProductPrice.setText(formatPrice(totalPrice));

        // Tải ảnh sản phẩm
        loadProductImage(holder.imgProduct, product.getImages().get(0), context);

        // Xử lý sự kiện tăng số lượng
        holder.btnIncreaseQuantity.setOnClickListener(v -> {
            int currentQty = temporaryQuantities.getOrDefault(productId, 1);
            handleQuantityChange(holder, product, currentQty + 1);
        });

// Xử lý sự kiện giảm số lượng
        holder.btnDecreaseQuantity.setOnClickListener(v -> {
            int currentQty = temporaryQuantities.getOrDefault(productId, 1);
            if (currentQty > 1) {
                handleQuantityChange(holder, product, currentQty - 1);
            }
        });

        // Xử lý sự kiện xóa sản phẩm
        holder.btnRemoveProduct.setOnClickListener(v -> removeProduct(holder, position, product));
    }

    private double calculateFinalPrice(Product product) {
        if (product.getOfferPercentage() > 0) {
            return product.getPrice() * (1 - product.getOfferPercentage() / 100.0);
        }
        return product.getPrice();
    }

    private String formatPrice(double price) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormatter.format(price);
    }

    private void loadProductImage(ShapeableImageView imageView, String imageUrl, Context context) {
        Glide.with(context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .into(imageView);
    }

    private void handleQuantityChange(CartViewHolder holder, Product product, int newQuantity) {
        if (newQuantity > 0 && newQuantity <= product.getQuantity()) {
            // Lưu số lượng mới vào SharedPreferences
            preferences.edit().putInt(product.getId(), newQuantity).apply();

            // Cập nhật số lượng tạm thời
            temporaryQuantities.put(product.getId(), newQuantity);

            // Tính giá mới
            double finalPrice = calculateFinalPrice(product);
            double totalPrice = finalPrice * newQuantity;

            // Cập nhật giao diện
            holder.tvQuantity.setText(String.valueOf(newQuantity));
            holder.tvProductPrice.setText(formatPrice(totalPrice));

            // Thông báo cho fragment về sự thay đổi số lượng
            if (cartItemListener != null) {
                cartItemListener.onQuantityChanged(product.getId(), newQuantity, totalPrice);
            }
        } else {
            Toast.makeText(holder.itemView.getContext(),
                    "Số lượng không hợp lệ hoặc vượt quá số lượng trong kho", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeProduct(CartViewHolder holder, int position, Product product) {
        db.collection("users").document(userId).collection("cart")
                .document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Xóa số lượng đã lưu khỏi SharedPreferences
                    preferences.edit().remove(product.getId()).apply();
                    temporaryQuantities.remove(product.getId());

                    cartItems.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, cartItems.size());

                    if (cartItemListener != null) {
                        cartItemListener.onItemRemoved(position, product);
                    }

                    Toast.makeText(holder.itemView.getContext(),
                            "Đã xóa sản phẩm khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(holder.itemView.getContext(),
                        "Không thể xóa sản phẩm. Vui lòng thử lại", Toast.LENGTH_SHORT).show());
    }

    public List<Product> getUpdatedCartItems() {
        List<Product> updatedCartItems = new ArrayList<>();
        for (Product product : cartItems) {
            Product updatedProduct = new Product();

            // Copy tất cả các trường
            updatedProduct.setId(product.getId());
            updatedProduct.setName(product.getName());
            updatedProduct.setCategory(product.getCategory());
            updatedProduct.setDescription(product.getDescription());
            updatedProduct.setPrice(product.getPrice());
            updatedProduct.setImages(product.getImages());
            updatedProduct.setColors(product.getColors());
            updatedProduct.setSizes(product.getSizes());
            updatedProduct.setOfferPercentage(product.getOfferPercentage());

            // Cập nhật số lượng từ giỏ hàng
            int customQuantity = temporaryQuantities.getOrDefault(product.getId(), 1);
            updatedProduct.setQuantity(customQuantity);

            updatedCartItems.add(updatedProduct);
        }
        return updatedCartItems;
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductPrice, tvQuantity;
        ShapeableImageView imgProduct;
        MaterialButton btnRemoveProduct, btnIncreaseQuantity, btnDecreaseQuantity;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvCartProductName);
            tvProductPrice = itemView.findViewById(R.id.tvCartProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            imgProduct = itemView.findViewById(R.id.imgCartProduct);
            btnRemoveProduct = itemView.findViewById(R.id.btnRemoveProduct);
            btnIncreaseQuantity = itemView.findViewById(R.id.btnIncreaseQuantity);
            btnDecreaseQuantity = itemView.findViewById(R.id.btnDecreaseQuantity);
        }
    }
}