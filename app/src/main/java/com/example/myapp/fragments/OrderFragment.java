package com.example.myapp.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.OrderAdapter;
import com.example.myapp.adapters.SelectVoucherAdapter;
import com.example.myapp.data.Order;
import com.example.myapp.data.Product;
import com.example.myapp.data.Voucher;
import com.example.myapp.helpers.MoMoPaymentHelper;
import com.example.myapp.helpers.VNPayHelper;
import com.example.myapp.viewmodel.ProfileViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class OrderFragment extends Fragment {

    private List<Product> cartItems;
    private TextView tvCustomerName, tvCustomerPhone, tvShipping, tvTotal, tvSubtotal, tvVoucherDiscount,tvSelectedVoucherCode,tvSelectedVoucherDesc;
    private EditText etShippingAddress, edtVoucherCode;
    private RadioGroup paymentMethodRadioGroup;
    private Button btnPlaceOrder, btnApplyVoucher;
    private FirebaseFirestore firestore;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private double voucherDiscount = 0;
    private String appliedVoucherId = null;
    private ImageButton btnRemoveVoucher;
    private LinearLayout layoutSelectedVoucher;

    private void initViews(View view) {
        tvCustomerName = view.findViewById(R.id.tvCustomerName);
        tvCustomerPhone = view.findViewById(R.id.tvCustomerPhone);
        etShippingAddress = view.findViewById(R.id.etShippingAddress);
        paymentMethodRadioGroup = view.findViewById(R.id.paymentMethodRadioGroup);
        btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder);
        tvShipping = view.findViewById(R.id.tvShipping);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvTotal = view.findViewById(R.id.tvTotal);
        edtVoucherCode = view.findViewById(R.id.edtVoucherCode);
        btnApplyVoucher = view.findViewById(R.id.btnApplyVoucher);
        tvVoucherDiscount = view.findViewById(R.id.tvVoucherDiscount);
        tvSelectedVoucherCode = view.findViewById(R.id.tvSelectedVoucherCode);
        tvSelectedVoucherDesc = view.findViewById(R.id.tvSelectedVoucherDesc);
        btnRemoveVoucher = view.findViewById(R.id.btnRemoveVoucher);
        layoutSelectedVoucher = view.findViewById(R.id.layoutSelectedVoucher);

        btnRemoveVoucher.setOnClickListener(v -> removeVoucher());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        initViews(view);

        btnApplyVoucher.setOnClickListener(v -> {
            String code = edtVoucherCode.getText().toString().trim();
            if (code.isEmpty()) {
                showVoucherDialog();
            } else {
                // Check code and get voucher from Firestore
                String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                firestore.collection("user_vouchers")
                        .whereEqualTo("code", code)
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("used", false)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                Voucher voucher = queryDocumentSnapshots.getDocuments()
                                        .get(0).toObject(Voucher.class);
                                if (voucher != null) {
                                    handleVoucherApplication(voucher);
                                }
                            } else {
                                Toast.makeText(getContext(),
                                        "Voucher không hợp lệ hoặc đã được sử dụng",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.orderItemsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load cart items from arguments
        if (getArguments() != null) {
            cartItems = getArguments().getParcelableArrayList("cartItems");

            double subTotal = 0;
            int totalItems =0;
            assert cartItems != null;
            for (Product product : cartItems) {
                double discountedPrice = product.getPrice() * (1 - product.getOfferPercentage() / 100.0);
                subTotal += discountedPrice * product.getQuantity();
                totalItems++;
            }

            // Tính phí vận chuyển
            double shippingFee = calculateShippingFee(subTotal,totalItems);

            // Tính tổng thanh toán
            double total = subTotal + shippingFee;

            // Hiển thị dữ liệu
            tvShipping.setText(formatPrice(shippingFee));
            tvSubtotal.setText(formatPrice(subTotal));
            tvTotal.setText(formatPrice(total));

        } else {
            cartItems = new ArrayList<>();
            Toast.makeText(getContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
        }

        // Set up RecyclerView adapter
        OrderAdapter adapter = new OrderAdapter(cartItems);
        recyclerView.setAdapter(adapter);

        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                tvCustomerName.setText(user.getFullName());
                tvCustomerPhone.setText(user.getPhoneNumber());
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        ImageButton btnGetCurrentLocation = view.findViewById(R.id.btnGetCurrentLocation);
        btnGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        // Handle "Place Order" button click
        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        return view;
    }

    private void showVoucherDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_voucher, null);
        dialog.setContentView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerVouchers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Lấy danh sách voucher đã đổi
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        firestore.collection("user_vouchers")
                .whereEqualTo("userId", userId)
                .whereEqualTo("used", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Voucher> vouchers = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Voucher voucher = doc.toObject(Voucher.class);
                        if (voucher != null) {
                            voucher.setId(doc.getId());
                            vouchers.add(voucher);
                        }
                    }

                    SelectVoucherAdapter adapter = new SelectVoucherAdapter(
                            vouchers,
                            calculateSubtotal(),
                            selectedVoucher -> {
                                dialog.dismiss();
                                handleVoucherApplication(selectedVoucher);
                            }
                    );
                    recyclerView.setAdapter(adapter);
                });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void removeVoucher() {
        // Reset các giá trị
        voucherDiscount = 0;
        appliedVoucherId = null;

        // Reset UI
        edtVoucherCode.setText("");
        edtVoucherCode.setEnabled(true);
        btnApplyVoucher.setEnabled(true);
        layoutSelectedVoucher.setVisibility(View.GONE);
        tvVoucherDiscount.setVisibility(View.GONE);

        // Cập nhật lại tổng tiền
        updateTotalAmount();

        Toast.makeText(getContext(), "Đã xóa voucher", Toast.LENGTH_SHORT).show();
    }

    private void handleVoucherApplication(Voucher voucher) {
        double subtotal = calculateSubtotal();
        if (subtotal >= voucher.getMinSpend()) {
            voucherDiscount = voucher.getDiscountAmount();
            appliedVoucherId = voucher.getId();

            // Hiển thị thông tin voucher đã chọn
            layoutSelectedVoucher.setVisibility(View.VISIBLE);
            tvSelectedVoucherCode.setText(voucher.getCode());
            tvSelectedVoucherDesc.setText(String.format("Giảm %s cho đơn từ %s",
                    formatPrice(voucher.getDiscountAmount()),
                    formatPrice(voucher.getMinSpend())));

            tvVoucherDiscount.setText("-" + formatPrice(voucherDiscount));
            tvVoucherDiscount.setVisibility(View.VISIBLE);

            updateTotalAmount();

            Toast.makeText(getContext(), "Áp dụng voucher thành công!", Toast.LENGTH_SHORT).show();
            edtVoucherCode.setText(voucher.getCode());
        } else {
            Toast.makeText(getContext(),
                    "Đơn hàng chưa đạt giá trị tối thiểu: " + formatPrice(voucher.getMinSpend()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalAmount() {
        double subtotal = calculateSubtotal();
        double shippingFee = calculateShippingFee(subtotal, cartItems.size());
        double total = subtotal + shippingFee - voucherDiscount;

        tvSubtotal.setText(formatPrice(subtotal));
        tvShipping.setText(formatPrice(shippingFee));
        if (voucherDiscount > 0) {
            tvVoucherDiscount.setText("-" + formatPrice(voucherDiscount));
            tvVoucherDiscount.setVisibility(View.VISIBLE);
        } else {
            tvVoucherDiscount.setVisibility(View.GONE);
        }
        tvTotal.setText(formatPrice(total));
    }

    // Thêm phương thức tính subtotal
    private double calculateSubtotal() {
        double subtotal = 0;
        for (Product product : cartItems) {
            double discountedPrice = product.getPrice() * (1 - product.getOfferPercentage() / 100.0);
            subtotal += discountedPrice * product.getQuantity();
        }
        return subtotal;
    }

    private String formatPrice(double price) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price);
    }

    private void getCurrentLocation() {
        // Kiểm tra quyền truy cập vị trí
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Yêu cầu quyền nếu chưa được cấp
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Lấy vị trí
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    Address address = addresses.get(0);
                                    String fullAddress = address.getAddressLine(0);
                                    etShippingAddress.setText(fullAddress); // Gán địa chỉ vào EditText
                                } else {
                                    Toast.makeText(getContext(), "Không tìm thấy địa chỉ từ vị trí", Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), "Lỗi khi lấy địa chỉ từ vị trí", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi lấy vị trí", Toast.LENGTH_SHORT).show());
        }
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean locationPermission = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (locationPermission != null && locationPermission) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(getContext(), "Quyền vị trí bị từ chối", Toast.LENGTH_SHORT).show();
                }
            });

    private double calculateShippingFee(double subTotal, int totalItems) {
        if (subTotal >= 2000000) {
            return 0; // Miễn phí vận chuyển nếu tổng giá trị >= 2 triệu
        } else {
            return totalItems * 15000; // 15,000 VND cho mỗi item sản phẩm
        }
    }

    private void placeOrder() {
        // Kiểm tra địa chỉ
        String shippingAddress = etShippingAddress.getText().toString().trim();
        if (shippingAddress.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy phương thức thanh toán
        int selectedPaymentMethodId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        String paymentMethod;

        if (selectedPaymentMethodId == R.id.rbVnPay) {
            paymentMethod = "Ví điện tử VnPay";
            // Xử lý thanh toán VNPay
            handleVNPayPayment(shippingAddress);
        }else if(selectedPaymentMethodId == R.id.rbMoMo) {
            paymentMethod = "Ví điện tử MoMo";
            // Xử lý thanh toán MoMo
            handleMoMoPayment(shippingAddress);
        }else {
            paymentMethod = "COD";
            // Xử lý COD
            checkInventoryAndPlaceOrder(shippingAddress, paymentMethod);
        }
    }

    private void handleVNPayPayment(String shippingAddress) {
        // Tạo document reference với ID tự động từ Firestore
        DocumentReference orderRef = firestore.collection("orders").document();

        // Tạo mã đơn hàng 7 số cho field id
        String orderNumber = generateOrderId(); // Hàm này tạo mã 7 số

        // Tính tổng tiền
        double subTotal = calculateSubtotal();
        double shippingFee = calculateShippingFee(subTotal, cartItems.size());
        double totalAmount = subTotal + shippingFee - voucherDiscount;

        // Tạo đơn hàng
        Order order = new Order();
        order.setId(orderNumber);  // Dùng mã 7 số cho field id
        order.setUserId(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
        order.setCustomerName(tvCustomerName.getText().toString());
        order.setCustomerPhone(tvCustomerPhone.getText().toString());
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod("Ví điện tử VnPay");
        order.setOrderTime(System.currentTimeMillis());
        order.setProducts(cartItems);
        order.setSubtotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setVoucherDiscount(voucherDiscount);
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT.name());

        // Lưu đơn hàng vào Firestore
        orderRef.set(order)  // orderRef đã có ID tự động của Firestore
                .addOnSuccessListener(aVoid -> {
                    // Bắt đầu thanh toán VNPay
                    String orderInfo = "Thanh toan don hang " + orderNumber;
                    VNPayHelper.startPayment(
                            requireContext(),
                            orderNumber,  // Dùng mã 7 số cho VNPay
                            totalAmount,
                            orderInfo
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Lỗi khi tạo đơn hàng: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void handleMoMoPayment(String shippingAddress) {
        DocumentReference orderRef = firestore.collection("orders").document();
        String orderNumber = generateOrderId();

        // Tính tổng tiền và chuyển về VND (không có phần thập phân)
        double subTotal = calculateSubtotal();
        double shippingFee = calculateShippingFee(subTotal, cartItems.size());
        double totalAmount = subTotal + shippingFee - voucherDiscount;
        long amount = (long) totalAmount; // Chuyển double thành long

        // Tạo đơn hàng
        Order order = new Order();
        order.setId(orderNumber);
        order.setUserId(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
        order.setCustomerName(tvCustomerName.getText().toString());
        order.setCustomerPhone(tvCustomerPhone.getText().toString());
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod("Ví điện tử MoMo");
        order.setOrderTime(System.currentTimeMillis());
        order.setProducts(cartItems);
        order.setSubtotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setVoucherDiscount(voucherDiscount);
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT.name());

        orderRef.set(order)
                .addOnSuccessListener(aVoid -> {
                    // Mở QR code thanh toán MoMo
                    MoMoPaymentHelper.startMoMoPayment(
                            requireContext(),
                            orderNumber,
                            amount
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Lỗi khi tạo đơn hàng: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void checkInventoryAndPlaceOrder(String shippingAddress, String paymentMethod) {
        WriteBatch batch = firestore.batch();
        List<String> outOfStockProducts = new ArrayList<>();
        List<DocumentSnapshot> productDocs = new ArrayList<>();

        // Tạo danh sách Promise để kiểm tra tất cả sản phẩm
        List<Task<DocumentSnapshot>> productTasks = new ArrayList<>();

        for (Product cartProduct : cartItems) {
            Task<DocumentSnapshot> productTask = firestore.collection("products")
                    .document(cartProduct.getId())
                    .get();
            productTasks.add(productTask);
        }

        // Sử dụng Tasks.whenAllSuccess để đợi tất cả các truy vấn hoàn thành
        Tasks.whenAllSuccess(productTasks)
                .addOnSuccessListener(objects -> {
                    boolean canProceed = true;

                    for (int i = 0; i < objects.size(); i++) {
                        DocumentSnapshot productDoc = (DocumentSnapshot) objects.get(i);
                        Product cartProduct = cartItems.get(i);

                        if (productDoc.exists()) {
                            long stockQuantity = productDoc.getLong("quantity");
                            int requestedQuantity = cartProduct.getQuantity();

                            if (stockQuantity < requestedQuantity) {
                                outOfStockProducts.add(cartProduct.getName());
                                canProceed = false;
                            } else {
                                // Cập nhật số lượng mới cho sản phẩm
                                batch.update(productDoc.getReference(),
                                        "quantity", stockQuantity - requestedQuantity);
                            }
                            productDocs.add(productDoc);
                        }
                    }

                    if (!canProceed) {
                        String message = "Sản phẩm hết hàng hoặc không đủ số lượng: " +
                                String.join(", ", outOfStockProducts);
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Tạo đơn hàng và thực hiện các thay đổi
                    proceedWithOrder(batch, shippingAddress, paymentMethod);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Lỗi khi kiểm tra kho hàng: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void proceedWithOrder(WriteBatch batch, String shippingAddress, String paymentMethod) {
        // Tính toán các giá trị
        double subTotal = calculateSubtotal();
        int totalItems = cartItems.size();
        double shippingFee = calculateShippingFee(subTotal, totalItems);
        double totalAmount = subTotal + shippingFee - voucherDiscount;

        int pointsEarned = (int)(totalAmount / 100000);

        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String orderId = generateOrderId();

        // Tạo đối tượng Order
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setCustomerName(tvCustomerName.getText().toString());
        order.setCustomerPhone(tvCustomerPhone.getText().toString());
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(paymentMethod);

        order.setStatus(Order.OrderStatus.PENDING.name()); // Đơn mới đặt ở trạng thái chờ xác nhận
        order.setStatus(Order.OrderStatus.PENDING.name());

        order.setOrderTime(System.currentTimeMillis());
        order.setLastUpdated(System.currentTimeMillis());

        order.setProducts(cartItems);
        order.setSubtotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setVoucherDiscount(voucherDiscount);
        order.setTotalAmount(totalAmount);
        order.setEarnedPoints(pointsEarned);

        // Thêm đơn hàng vào batch
        DocumentReference orderRef = firestore.collection("orders").document();
        batch.set(orderRef, order);

        // Cập nhật trạng thái voucher nếu có
        if (appliedVoucherId != null) {
            DocumentReference voucherRef = firestore.collection("user_vouchers")
                    .document(appliedVoucherId);
            batch.update(voucherRef, "used", true);
        }

        // Xóa giỏ hàng
        firestore.collection("users").document(userId).collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }

                    // Thực hiện tất cả các thay đổi
                    batch.commit().addOnSuccessListener(aVoid -> {
                        // Cập nhật điểm
                        updateUserPoints(totalAmount, order);
                        // Chuyển đến màn hình thành công
                        navigateToOrderSuccessFragment(orderId);
                    }).addOnFailureListener(e -> Toast.makeText(getContext(),
                            "Lỗi khi đặt hàng: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
                });
    }

    // Hàm tạo mã đơn hàng ngẫu nhiên
    private String generateOrderId() {
        Random random = new Random();
        int orderId = 1000000 + random.nextInt(9000000); // Tạo số trong khoảng [1000000, 9999999]
        return String.valueOf(orderId);
    }

    private void updateUserPoints(double totalAmount, Order order) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        int pointsEarned = (int)(totalAmount / 100000);

        DocumentReference userRef = FirebaseFirestore.getInstance()
                .collection("users").document(userId);

        userRef.get().addOnSuccessListener(doc -> {
            int currentPoints = doc.getLong("points") != null ?
                    Objects.requireNonNull(doc.getLong("points")).intValue() : 0;
            int newPoints = currentPoints + pointsEarned;

            order.setEarnedPoints(pointsEarned); // Lưu điểm vào đơn hàng

            userRef.update("points", newPoints)
                    .addOnSuccessListener(v -> Toast.makeText(getContext(),
                            "Bạn đã được cộng " + pointsEarned + " điểm!",
                            Toast.LENGTH_SHORT).show());
        });
    }

    private void navigateToOrderSuccessFragment(String orderId) {
        // Tạo instance của OrderSuccessFragment với mã đơn hàng
        OrderSuccessFragment orderSuccessFragment = OrderSuccessFragment.newInstance(orderId);

        // Chuyển sang OrderSuccessFragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, orderSuccessFragment)
                .addToBackStack(null)
                .commit();
    }
}