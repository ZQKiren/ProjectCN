package com.example.myapp.helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class MoMoPaymentHelper {
    private static final String TAG = "MoMoPaymentHelper";
    private static final String MOMO_PHONE = "0382146458"; // Thay bằng số điện thoại MoMo của bạn

    public static void startMoMoPayment(Context context, String orderId, long amount) {
        try {
            // Tạo link QR MoMo
            String momoLink = String.format("https://me-uat.mservice.com.vn/me/m8IbTzspiktXT5fosGfaC1",
                    MOMO_PHONE,
                    amount,
                    orderId);

            // Mở link trong browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(momoLink));
            context.startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error starting MoMo payment: " + e.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final String transactionId;
        private final String orderId;
        private final long amount;

        public PaymentResult(Uri uri) {
            // Xử lý kết quả từ MoMo
            String resultCode = uri.getQueryParameter("resultCode");
            this.message = uri.getQueryParameter("message");
            this.transactionId = uri.getQueryParameter("transId");
            this.orderId = uri.getQueryParameter("orderId");
            String amountStr = uri.getQueryParameter("amount");

            this.success = "0".equals(resultCode); // 0 là thành công
            this.amount = amountStr != null ? Long.parseLong(amountStr) : 0;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getTransactionId() { return transactionId; }
        public String getOrderId() { return orderId; }
        public long getAmount() { return amount; }
    }
}