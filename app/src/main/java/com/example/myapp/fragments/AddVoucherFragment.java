package com.example.myapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.myapp.R;
import com.example.myapp.data.Voucher;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class AddVoucherFragment extends Fragment {
    private TextInputEditText edtCode, edtDescription, edtDiscountAmount, edtMinSpend, edtPointsRequired, edtExpiryDate;
    private FirebaseFirestore db;
    private Calendar selectedDate;
    private MaterialButton btnAddVoucher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_voucher, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        selectedDate = Calendar.getInstance();

        // Initialize views
        initViews(view);
        setupToolbar(view);
        setupDatePicker();
        setupAddButton();

        return view;
    }

    private void initViews(View view) {
        try {
            edtCode = view.findViewById(R.id.edtCode);
            edtDescription = view.findViewById(R.id.edtDescription);
            edtDiscountAmount = view.findViewById(R.id.edtDiscountAmount);
            edtMinSpend = view.findViewById(R.id.edtMinSpend);
            edtPointsRequired = view.findViewById(R.id.edtPointsRequired);
            edtExpiryDate = view.findViewById(R.id.edtExpiryDate);
            btnAddVoucher = view.findViewById(R.id.btnAddVoucher);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khởi tạo view: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        }
    }

    private void setupDatePicker() {
        edtExpiryDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        String dateString = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        edtExpiryDate.setText(dateString);
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupAddButton() {
        btnAddVoucher.setOnClickListener(v -> addVoucher());
    }

    private void addVoucher() {
        try {
            // Get input values
            String code = Objects.requireNonNull(edtCode.getText()).toString().trim();
            String description = Objects.requireNonNull(edtDescription.getText()).toString().trim();
            String discountAmountStr = Objects.requireNonNull(edtDiscountAmount.getText()).toString().trim();
            String minSpendStr = Objects.requireNonNull(edtMinSpend.getText()).toString().trim();
            String pointsRequiredStr = Objects.requireNonNull(edtPointsRequired.getText()).toString().trim();

            // Validate inputs
            if (code.isEmpty() || description.isEmpty() || discountAmountStr.isEmpty()
                    || minSpendStr.isEmpty() || pointsRequiredStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse values
            double discountAmount = Double.parseDouble(discountAmountStr);
            double minSpend = Double.parseDouble(minSpendStr);
            int pointsRequired = Integer.parseInt(pointsRequiredStr);

            // Create voucher object
            Voucher voucher = new Voucher(
                    code,
                    description,
                    discountAmount,
                    minSpend,
                    selectedDate.getTimeInMillis(),
                    pointsRequired
            );

            // Save to Firestore
            db.collection("vouchers")
                    .add(voucher)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Thêm voucher thành công", Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}