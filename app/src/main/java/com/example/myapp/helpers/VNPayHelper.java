package com.example.myapp.helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayHelper {
    private static final String TAG = "VNPayHelper";

    // Thông tin merchant từ VNPay Sandbox
    private static final String VNPAY_TMN_CODE = "0O193WGW";
    private static final String VNPAY_HASH_SECRET = "YMOSRIL4SBTODP442BP3MLGL1OBPZTY3";
    private static final String VNPAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String VNPAY_VERSION = "2.1.0";
    private static final String VNPAY_COMMAND = "pay";

    public static void startPayment(Context context, String orderId, double amount, String orderInfo) {
        try {
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", VNPAY_VERSION);
            vnp_Params.put("vnp_Command", VNPAY_COMMAND);
            vnp_Params.put("vnp_TmnCode", VNPAY_TMN_CODE);
            vnp_Params.put("vnp_Amount", String.valueOf((long) (amount * 100))); // Convert to VND cents
            vnp_Params.put("vnp_CurrCode", "VND");

            // Generate transaction time
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            // Order Info
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "other"); // Change as needed

            // Return URL for app scheme
            vnp_Params.put("vnp_ReturnUrl", "myapp://payment_result");

            // Optional customer info
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");

            // Build query string
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    }

                    // Build query
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                    }
                    query.append('=');
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    }

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            // Append Secure Hash
            String vnp_SecureHash = hmacSHA512(hashData.toString());
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);

            // Create payment URL
            String paymentUrl = VNPAY_URL + "?" + query;

            // Open payment URL in browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl));
            context.startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error creating payment URL: " + e.getMessage());
        }
    }

    // Helper method to generate HMAC_SHA512 string
    private static String hmacSHA512(final String data) {
        try {
            if (data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = VNPayHelper.VNPAY_HASH_SECRET.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Handle payment result from VNPay
     * @param uri The callback URI from VNPay
     * @return PaymentResult object containing status and message
     */
    public static PaymentResult handlePaymentResult(Uri uri) {
        PaymentResult result = new PaymentResult();

        try {
            String vnp_ResponseCode = uri.getQueryParameter("vnp_ResponseCode");
            String vnp_TransactionStatus = uri.getQueryParameter("vnp_TransactionStatus");

            // Verify the payment response
            if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
                result.setSuccess(true);
                result.setMessage("Thanh toán thành công");
            } else {
                result.setSuccess(false);
                assert vnp_ResponseCode != null;
                result.setMessage(getErrorMessage(vnp_ResponseCode));
            }

            // Get additional info
            result.setTransactionId(uri.getQueryParameter("vnp_TransactionNo"));
            result.setAmount(Double.parseDouble(uri.getQueryParameter("vnp_Amount")) / 100); // Convert from cents
            result.setOrderId(uri.getQueryParameter("vnp_TxnRef"));

        } catch (Exception e) {
            Log.e(TAG, "Error handling payment result: " + e.getMessage());
            result.setSuccess(false);
            result.setMessage("Có lỗi xảy ra khi xử lý kết quả thanh toán");
        }

        return result;
    }

    private static String getErrorMessage(String responseCode) {
        return switch (responseCode) {
            case "07" ->
                    "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09" ->
                    "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10" ->
                    "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán.";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch.";
            case "51" ->
                    "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65" ->
                    "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì.";
            case "79" ->
                    "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.";
            default -> "Giao dịch không thành công.";
        };
    }

    public static class PaymentResult {
        private boolean success;
        private String message;
        private String transactionId;
        private String orderId;
        private double amount;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }
}