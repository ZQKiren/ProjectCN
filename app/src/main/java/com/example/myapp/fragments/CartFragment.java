package com.example.myapp.fragments;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.CartAdapter;
import com.example.myapp.data.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CartFragment extends Fragment implements CartAdapter.CartItemListener {

    private List<Product> cartItems;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private Button btnPlaceOrder;
    ImageView emptyCartIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        cartItems = new ArrayList<>();


        emptyCartIcon = view.findViewById(R.id.emptyCartIcon); // Thêm view.
        AnimatorSet animation = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.anim.empty_cart_animation); // Dùng requireContext()
        animation.setTarget(emptyCartIcon);
        animation.start();

        recyclerView = view.findViewById(R.id.cart_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder);
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        cartAdapter = new CartAdapter(cartItems, userId, this, requireContext());
        recyclerView.setAdapter(cartAdapter);

        loadCartItems();

        btnPlaceOrder.setOnClickListener(v -> {
            // Lấy danh sách sản phẩm với số lượng tùy chỉnh
            List<Product> updatedCartItems = cartAdapter.getUpdatedCartItems();

            // Truyền danh sách này sang OrderFragment
            OrderFragment orderFragment = new OrderFragment();
            Bundle args = new Bundle();
            args.putParcelableArrayList("cartItems", new ArrayList<>(updatedCartItems));
            orderFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, orderFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void updateTotalPrice() {
        double totalPrice = 0.0;
        SharedPreferences preferences = requireContext().getSharedPreferences("CartPreferences", Context.MODE_PRIVATE);

        for (Product product : cartItems) {
            double productPrice = product.getPrice() * (1 - product.getOfferPercentage() / 100.0);
            int quantity = preferences.getInt(product.getId(), 1);
            totalPrice += productPrice * quantity;
        }

        TextView tvTotalPrice = requireView().findViewById(R.id.tvTotalPrice);
        tvTotalPrice.setText(formatPrice(totalPrice));
    }

    // Định dạng giá thành kiểu tiền tệ Việt Nam Đồng
    private String formatPrice(double price) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormatter.format(price);
    }

    private void loadCartItems() {
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("users").document(userId).collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartItems.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            cartItems.add(product);
                        }
                        cartAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView
                        updateCartVisibility(); // Hiển thị nút đặt hàng nếu có sản phẩm
                        updateTotalPrice(); // Cập nhật tổng tiền
                    } else {
                        Toast.makeText(getContext(), "Lỗi khi tải giỏ hàng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCartVisibility() {
        View emptyStateContainer = requireView().findViewById(R.id.emptyStateContainer);
        View summaryCard = requireView().findViewById(R.id.summaryCard); // Thêm id cho MaterialCardView tổng tiền

        if (cartItems.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            summaryCard.setVisibility(View.GONE);
            btnPlaceOrder.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            summaryCard.setVisibility(View.VISIBLE);
            btnPlaceOrder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCartItems();
    }

    @Override
    public void onQuantityChanged(String productId, int newQuantity, double newPrice) {
        double totalPrice = 0.0;
        SharedPreferences preferences = requireContext().getSharedPreferences("CartPreferences", Context.MODE_PRIVATE);

        // Lặp qua danh sách giỏ hàng để tính tổng tiền
        for (Product product : cartItems) {
            double productPrice = product.getPrice() * (1 - product.getOfferPercentage() / 100.0);

            // Lấy số lượng của từng sản phẩm từ SharedPreferences
            int quantity;
            if (product.getId().equals(productId)) {
                quantity = newQuantity; // Sử dụng số lượng mới cho sản phẩm vừa thay đổi
            } else {
                quantity = preferences.getInt(product.getId(), 1); // Lấy số lượng đã lưu cho các sản phẩm khác
            }

            totalPrice += productPrice * quantity;
        }

        // Cập nhật tổng tiền
        TextView tvTotalPrice = requireView().findViewById(R.id.tvTotalPrice);
        tvTotalPrice.setText(formatPrice(totalPrice));
    }

    @Override
    public void onItemRemoved(int position, Product product) {
        updateCartVisibility();
        updateTotalPrice();
    }
}