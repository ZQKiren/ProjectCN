package com.example.myapp.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Order implements Parcelable {
    private String id;
    private String userId;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String paymentMethod;
    private String status = OrderStatus.PENDING.name(); // Chỉ giữ lại trường này
    private long orderTime;
    private List<Product> products;
    private double subtotal;
    private double shippingFee;
    private double totalAmount;
    private double voucherDiscount;
    private String cancelReason;
    private String note;
    private long lastUpdated;
    private int earnedPoints; // Thời gian cập nhật trạng thái cuối// Tổng thanh toán (subtotal + shippingFee)

    // Constructors
    public Order() {}

    public Order(String id, String userId, String customerName, String customerPhone,
                 String shippingAddress, String paymentMethod, String status,
                 long orderTime, List<Product> products, double subtotal,
                 double shippingFee, double totalAmount) {
        this.id = id;
        this.userId = userId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.orderTime = orderTime;
        this.products = products;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
    }

    // Parcelable implementation
    protected Order(Parcel in) {
        id = in.readString();
        userId = in.readString();
        customerName = in.readString();
        customerPhone = in.readString();
        shippingAddress = in.readString();
        paymentMethod = in.readString();
        orderTime = in.readLong();
        subtotal = in.readDouble();
        shippingFee = in.readDouble();
        totalAmount = in.readDouble();
        products = in.createTypedArrayList(Product.CREATOR);
        cancelReason = in.readString();
        note = in.readString();
        lastUpdated = in.readLong();
    }

    public static final Creator<Order> CREATOR = new Creator<>() {
        @Override
        public Order createFromParcel(Parcel in) {
            return new Order(in);
        }

        @Override
        public Order[] newArray(int size) {
            return new Order[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(customerName);
        dest.writeString(customerPhone);
        dest.writeString(shippingAddress);
        dest.writeString(paymentMethod);
        dest.writeLong(orderTime);
        dest.writeDouble(subtotal);
        dest.writeDouble(shippingFee);
        dest.writeDouble(totalAmount);
        dest.writeTypedList(products);
        dest.writeString(cancelReason);
        dest.writeString(note);
        dest.writeLong(lastUpdated);
    }

    public enum OrderStatus {
        PENDING("Chờ xác nhận", "Bạn vừa đặt đơn hàng này"),
        PENDING_PAYMENT("Chờ thanh toán", "Đang chờ thanh toán"),
        CONFIRMED("Đã xác nhận", "Đơn hàng đã được xác nhận"),
        PROCESSING("Đang xử lý", "Shop đang chuẩn bị hàng"),
        SHIPPING("Đang giao hàng", "Đơn hàng đang được giao đến bạn"),
        DELIVERED("Đã giao hàng", "Đơn hàng đã giao thành công"),
        CANCELLED("Đã hủy", "Đơn hàng đã bị hủy"),
        RETURNED("Trả hàng/Hoàn tiền", "Đơn hàng đang được xử lý hoàn trả");

        private final String displayText;
        private final String description;

        OrderStatus(String displayText, String description) {
            this.displayText = displayText;
            this.description = description;
        }

        public String getDisplayText() {
            return displayText;
        }

        public String getDescription() {
            return description;
        }

        public static OrderStatus fromString(String text) {
            try {
                return OrderStatus.valueOf(text.toUpperCase());
            } catch (IllegalArgumentException e) {
                return PENDING;
            }
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getOrderTime() { return orderTime; }
    public void setOrderTime(long orderTime) { this.orderTime = orderTime; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public int getEarnedPoints() { return earnedPoints; }
    public void setEarnedPoints(int points) { this.earnedPoints = points; }

    public double getVoucherDiscount() {
        return voucherDiscount;
    }

    public void setVoucherDiscount(double voucherDiscount) {
        this.voucherDiscount = voucherDiscount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}


