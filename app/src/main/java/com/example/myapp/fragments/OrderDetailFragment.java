package com.example.myapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.OrderDetailAdapter;
import com.example.myapp.data.Product;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OrderDetailFragment extends Fragment {
    private static final String ARG_ORDER_ID = "orderId";
    private String orderId;
    private FirebaseFirestore db;

    // Views
    private TextView tvOrderId, tvOrderTime, tvOrderStatus, tvVoucherDiscount;
    private TextView tvCustomerName, tvCustomerPhone, tvShippingAddress;
    private TextView tvSubtotal, tvShippingFee, tvTotalAmount, tvPaymentMethod;
    private RecyclerView rvOrderItems;
    private OrderDetailAdapter adapter;
    private View voucherDiscountLayout;

    public static OrderDetailFragment newInstance(String orderId) {
        OrderDetailFragment fragment = new OrderDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString(ARG_ORDER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_detail, container, false);

        initViews(view);
        setupToolbar(view);
        loadOrderDetails();

        return view;
    }

    private void initViews(View view) {
        // Initialize all TextViews
        tvOrderId = view.findViewById(R.id.tvOrderId);
        tvOrderTime = view.findViewById(R.id.tvOrderTime);
        tvOrderStatus = view.findViewById(R.id.tvOrderStatus);
        tvCustomerName = view.findViewById(R.id.tvCustomerName);
        tvCustomerPhone = view.findViewById(R.id.tvCustomerPhone);
        tvShippingAddress = view.findViewById(R.id.tvShippingAddress);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvShippingFee = view.findViewById(R.id.tvShippingFee);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod);
        tvVoucherDiscount = view.findViewById(R.id.tvVoucherDiscount);
        voucherDiscountLayout = view.findViewById(R.id.voucherDiscountLayout);

        // Setup RecyclerView
        rvOrderItems = view.findViewById(R.id.rvOrderItems);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderDetailAdapter();
        rvOrderItems.setAdapter(adapter);
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadOrderDetails() {
        db.collection("orders")
                .whereEqualTo("id", orderId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        updateOrderInfo(document);
                        updateOrderItems(document);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi khi tải thông tin đơn hàng", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateOrderInfo(DocumentSnapshot document) {
        // Update order info
        tvOrderId.setText(getString(R.string.order_id_prefix, document.getString("id")));
        tvOrderStatus.setText(getString(R.string.order_status_prefix, document.getString("status")));

        // Format and display order time
        Long orderTime = document.getLong("orderTime");
        if (orderTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String formattedTime = sdf.format(new Date(orderTime));
            tvOrderTime.setText(getString(R.string.order_time_prefix, formattedTime));
        }

        // Update shipping info
        tvCustomerName.setText(getString(R.string.customer_name_prefix, document.getString("customerName")));
        tvCustomerPhone.setText(getString(R.string.customer_phone_prefix, document.getString("customerPhone")));
        tvShippingAddress.setText(getString(R.string.shipping_address_prefix, document.getString("shippingAddress")));

        // Update payment info
        Double subtotal = document.getDouble("subtotal");
        Double shippingFee = document.getDouble("shippingFee");
        Double voucherDiscount = document.getDouble("voucherDiscount");
        Double totalAmount = document.getDouble("totalAmount");
        String paymentMethod = document.getString("paymentMethod");

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        tvSubtotal.setText(subtotal != null ? currencyFormatter.format(subtotal) : "0đ");
        tvShippingFee.setText(shippingFee != null ? currencyFormatter.format(shippingFee) : "0đ");

        // Hiển thị giảm giá voucher
        if (voucherDiscount != null && voucherDiscount > 0) {
            tvVoucherDiscount.setText("-" + currencyFormatter.format(voucherDiscount));
            voucherDiscountLayout.setVisibility(View.VISIBLE); // Sử dụng layout trực tiếp
        } else {
            voucherDiscountLayout.setVisibility(View.GONE);
        }

        tvTotalAmount.setText(totalAmount != null ? currencyFormatter.format(totalAmount) : "0đ");
        tvPaymentMethod.setText(getString(R.string.payment_method_prefix, paymentMethod));
    }

    @SuppressWarnings("unchecked")
    private void updateOrderItems(DocumentSnapshot document) {
        List<Map<String, Object>> products = (List<Map<String, Object>>) document.get("products");
        if (products != null) {
            List<Product> productList = new ArrayList<>();
            for (Map<String, Object> productMap : products) {
                Product product = new Product();
                product.setId((String) productMap.get("id"));
                product.setName((String) productMap.get("name"));
                product.setPrice(((Number) Objects.requireNonNull(productMap.get("price"))).doubleValue());
                product.setQuantity(((Number) Objects.requireNonNull(productMap.get("quantity"))).intValue());
                product.setOfferPercentage(((Number) Objects.requireNonNull(productMap.get("offerPercentage"))).intValue());
                if (productMap.get("images") instanceof List) {
                    product.setImages((List<String>) productMap.get("images"));
                }
                productList.add(product);
            }
            adapter.submitList(productList);
        }
    }
}
