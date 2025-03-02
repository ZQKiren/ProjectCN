package com.example.myapp.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.ManageVouchersAdapter;
import com.example.myapp.data.Voucher;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageVouchersFragment extends Fragment implements ManageVouchersAdapter.OnVoucherActionListener {
    private RecyclerView recyclerView;
    private View emptyStateContainer;
    private ManageVouchersAdapter adapter;
    private final List<Voucher> voucherList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_vouchers, container, false);

        initViews(view);
        setupToolbar(view);
        loadVouchers();

        return view;
    }

    private void initViews(View view) {
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerViewVouchers);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageVouchersAdapter(requireContext(), voucherList, this);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v ->
                showAddVoucherDialog());
    }

    private void loadVouchers() {
        db.collection("vouchers")
                .get()
                .addOnSuccessListener(documents -> {
                    voucherList.clear();
                    for (DocumentSnapshot doc : documents) {
                        Voucher voucher = doc.toObject(Voucher.class);
                        if (voucher != null) {
                            voucher.setId(doc.getId());
                            voucherList.add(voucher);
                        }
                    }
                    updateUI();
                });
    }

    private void updateUI() {
        if (voucherList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void setupToolbar(View view) {
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    @Override
    public void onVoucherEdit(Voucher voucher) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, UpdateVoucherFragment.newInstance(voucher))
                .addToBackStack(null)
                .commit();
    }

    private void showAddVoucherDialog() {
        // Chuyển đến AddVoucherFragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new AddVoucherFragment())
                .addToBackStack(null)
                .commit();
    }

    private void showEditVoucherDialog(Voucher voucher) {
        AddVoucherFragment fragment = new AddVoucherFragment();
        Bundle args = new Bundle();
        args.putParcelable("voucher", (Parcelable) voucher); // Đảm bảo Voucher implements Parcelable
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onVoucherDelete(Voucher voucher) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa voucher này?")
                .setPositiveButton("Xóa", (dialog, which) -> db.collection("vouchers")
                        .document(voucher.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            voucherList.remove(voucher);
                            updateUI();
                            Toast.makeText(requireContext(),
                                    "Đã xóa voucher", Toast.LENGTH_SHORT).show();
                        }))
                .setNegativeButton("Hủy", null)
                .show();
    }
}
