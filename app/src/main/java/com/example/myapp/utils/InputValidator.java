package com.example.myapp.utils;

import android.util.Patterns;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Objects;

public class InputValidator {

    public static boolean validateEmail(TextInputEditText emailField) {
        String email = Objects.requireNonNull(emailField.getText()).toString().trim();

        if (email.isEmpty()) {
            emailField.setError("Vui lòng nhập email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Định dạng email không hợp lệ");
            return false;
        }

        return true;
    }

    public static boolean validateEmailAndPassword(TextInputEditText emailField, TextInputEditText passwordField) {
        String email = Objects.requireNonNull(emailField.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordField.getText()).toString().trim();

        if (email.isEmpty()) {
            emailField.setError("Email không được để trống");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Vui lòng nhập email hợp lệ");
            return false;
        }

        if (password.isEmpty()) {
            passwordField.setError("Mật khẩu không được để trống");
            return false;
        }

        if (password.length() < 6) {
            passwordField.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return false;
        }

        return true;
    }

    public static boolean validateSignUpInput(TextInputEditText fullNameField, TextInputEditText emailField,
                                              TextInputEditText passwordField, TextInputEditText confirmPasswordField) {
        String fullName = Objects.requireNonNull(fullNameField.getText()).toString().trim();
        String email = Objects.requireNonNull(emailField.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordField.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordField.getText()).toString().trim();

        if (fullName.isEmpty()) {
            fullNameField.setError("Vui lòng nhập họ tên");
            return false;
        }

        if (!validateEmailAndPassword(emailField, passwordField)) {
            return false; // Đã kiểm tra email và mật khẩu
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordField.setError("Vui lòng xác nhận mật khẩu");
            return false;
        }

        if (!confirmPassword.equals(password)) {
            confirmPasswordField.setError("Mật khẩu xác nhận không khớp với mật khẩu đã nhập");
            return false;
        }
        return true;
    }

    // Thêm phương thức validate sản phẩm mới
    public static boolean validateProduct(
            TextInputEditText nameField,
            TextInputEditText categoryField,
            TextInputEditText descriptionField,
            TextInputEditText priceField,
            TextInputEditText discountField,
            TextInputEditText sizesField,
            TextInputEditText quantityField,
            List<String> colors,
            List<?> images) {

        // Validate tên sản phẩm
        String name = Objects.requireNonNull(nameField.getText()).toString().trim();
        if (name.isEmpty()) {
            nameField.setError("Vui lòng nhập tên sản phẩm");
            return false;
        }

        // Validate danh mục
        String category = Objects.requireNonNull(categoryField.getText()).toString().trim();
        if (category.isEmpty()) {
            categoryField.setError("Vui lòng nhập danh mục");
            return false;
        }

        // Validate mô tả
        String description = Objects.requireNonNull(descriptionField.getText()).toString().trim();
        if (description.isEmpty()) {
            descriptionField.setError("Vui lòng nhập mô tả sản phẩm");
            return false;
        }

        // Validate giá
        try {
            String price = Objects.requireNonNull(priceField.getText()).toString().trim();
            double priceValue = Double.parseDouble(price);
            if (priceValue <= 0) {
                priceField.setError("Giá phải lớn hơn 0");
                return false;
            }
        } catch (NumberFormatException e) {
            priceField.setError("Vui lòng nhập giá hợp lệ");
            return false;
        }

        // Validate giảm giá
        try {
            String discount = Objects.requireNonNull(discountField.getText()).toString().trim();
            if (!discount.isEmpty()) {
                int discountValue = Integer.parseInt(discount);
                if (discountValue < 0 || discountValue > 100) {
                    discountField.setError("Giảm giá phải từ 0 đến 100%");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            discountField.setError("Vui lòng nhập % giảm giá hợp lệ");
            return false;
        }

        // Validate kích thước
        String sizes = Objects.requireNonNull(sizesField.getText()).toString().trim();
        if (sizes.isEmpty()) {
            sizesField.setError("Vui lòng nhập kích thước");
            return false;
        }

        // Validate số lượng
        try {
            String quantity = Objects.requireNonNull(quantityField.getText()).toString().trim();
            int quantityValue = Integer.parseInt(quantity);
            if (quantityValue <= 0) {
                quantityField.setError("Số lượng phải lớn hơn 0");
                return false;
            }
        } catch (NumberFormatException e) {
            quantityField.setError("Vui lòng nhập số lượng hợp lệ");
            return false;
        }

        // Validate màu sắc
        if (colors.isEmpty()) {
            return false;
        }

        // Validate ảnh
        if (images.isEmpty()) {
            return false;
        }

        return true;
    }

    // Helper method để kiểm tra số
    public static boolean isValidNumber(String value) {
        try {
            double number = Double.parseDouble(value);
            return number > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

