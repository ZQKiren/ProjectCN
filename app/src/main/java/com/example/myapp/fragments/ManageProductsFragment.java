package com.example.myapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.ManageProductsAdapter;
import com.example.myapp.data.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageProductsFragment extends Fragment implements ManageProductsAdapter.OnProductUpdateListener {

    private RecyclerView recyclerViewProducts;
    private ManageProductsAdapter manageProductsAdapter;
    private final List<Product> productList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_products, container, false);

        recyclerViewProducts = view.findViewById(R.id.recyclerViewProducts);
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize ManageProductsAdapter and set listener
        manageProductsAdapter = new ManageProductsAdapter(getContext(), productList, this);
        recyclerViewProducts.setAdapter(manageProductsAdapter);

        loadProductsFromFirestore();

        // Handle Floating Action Button
        FloatingActionButton fabAddProduct = view.findViewById(R.id.fabAddProduct);
        fabAddProduct.setOnClickListener(v -> openAddProductFragment());

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void openAddProductFragment() {
        AddProductFragment addFragment = new AddProductFragment();

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, addFragment); // Ensure R.id.content_frame is correct
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void loadProductsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            String productId = document.getId(); // Retrieve productId from Firestore
                            product.setId(productId); // Set ID to Product

                            // Add product to adapter
                            manageProductsAdapter.addProduct(product, productId);
                        }
                        manageProductsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Lỗi khi tải sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onUpdateProduct(Product product, String productId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("product", product); // Use Parcelable instead of Serializable
        bundle.putString("productId", productId); // Pass productId from Firestore

        UpdateProductFragment updateFragment = new UpdateProductFragment();
        updateFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, updateFragment); // Ensure R.id.content_frame is the correct container ID
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
