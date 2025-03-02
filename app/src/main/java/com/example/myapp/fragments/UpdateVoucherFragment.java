package com.example.myapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.myapp.R;
import com.example.myapp.data.Voucher;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class UpdateVoucherFragment extends Fragment {
    private EditText edtCode, edtDescription, edtDiscountAmount, edtMinSpend, edtPointsRequired, edtExpiryDate;
    private FirebaseFirestore db;
    private Calendar selectedDate;
    private MaterialButton btnUpdateVoucher;
    private Voucher voucher;

    public static UpdateVoucherFragment newInstance(Voucher voucher) {
        UpdateVoucherFragment fragment = new UpdateVoucherFragment();
        Bundle args = new Bundle();
        args.putParcelable("voucher", voucher);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_voucher, container, false);

        initViews(view);
        loadVoucherData();
        setupDatePicker();
        setupUpdateButton();

        return view;
    }

    private void initViews(View view) {
        db = FirebaseFirestore.getInstance();
        selectedDate = Calendar.getInstance();

        edtCode = view.findViewById(R.id.edtCode);
        edtDescription = view.findViewById(R.id.edtDescription);
        edtDiscountAmount = view.findViewById(R.id.edtDiscountAmount);
        edtMinSpend = view.findViewById(R.id.edtMinSpend);
        edtPointsRequired = view.findViewById(R.id.edtPointsRequired);
        edtExpiryDate = view.findViewById(R.id.edtExpiryDate);
        btnUpdateVoucher = view.findViewById(R.id.btnUpdateVoucher);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("Cập nhật voucher");
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadVoucherData() {
        voucher = getArguments() != null ? (Voucher) getArguments().getParcelable("voucher") : null;
        if (voucher != null) {
            edtCode.setText(voucher.getCode());
            edtDescription.setText(voucher.getDescription());
            edtDiscountAmount.setText(String.valueOf(voucher.getDiscountAmount()));
            edtMinSpend.setText(String.valueOf(voucher.getMinSpend()));
            edtPointsRequired.setText(String.valueOf(voucher.getPointsRequired()));

            selectedDate.setTimeInMillis(voucher.getExpiryDate());
            updateDateDisplay();
        }
    }

    private void setupDatePicker() {
        edtExpiryDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
    }

    private void updateDateDisplay() {
        String dateString = String.format(Locale.getDefault(), "%02d/%02d/%d",
                selectedDate.get(Calendar.DAY_OF_MONTH),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.YEAR));
        edtExpiryDate.setText(dateString);
    }

    private void setupUpdateButton() {
        btnUpdateVoucher.setOnClickListener(v -> updateVoucher());
    }

    private void updateVoucher() {
        if (!validateInput()) return;

        voucher.setCode(edtCode.getText().toString().trim());
        voucher.setDescription(edtDescription.getText().toString().trim());
        voucher.setDiscountAmount(Double.parseDouble(edtDiscountAmount.getText().toString().trim()));
        voucher.setMinSpend(Double.parseDouble(edtMinSpend.getText().toString().trim()));
        voucher.setPointsRequired(Integer.parseInt(edtPointsRequired.getText().toString().trim()));
        voucher.setExpiryDate(selectedDate.getTimeInMillis());

        db.collection("vouchers")
                .document(voucher.getId())
                .set(voucher)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cập nhật voucher thành công", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean validateInput() {
        if (edtCode.getText().toString().trim().isEmpty() ||
                edtDescription.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
