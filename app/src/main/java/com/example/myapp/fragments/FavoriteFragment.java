package com.example.myapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.FavoriteAdapter;
import com.example.myapp.data.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FavoriteFragment extends Fragment {

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private final List<Product> favoriteProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        initViews(view);
        setupRecyclerView();
        loadFavoriteProducts();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupRecyclerView() {
        // Use LinearLayoutManager for a list view
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // Set item decoration for spacing if needed
        // recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        // Create and set adapter with click listener
        adapter = new FavoriteAdapter(requireContext(), favoriteProducts, product -> {
            navigateToProductDetail(product);
        });
        recyclerView.setAdapter(adapter);
    }

    private void navigateToProductDetail(Product product) {
        Fragment fragment = ProductDetailFragment.newInstance(product);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadFavoriteProducts() {
        // Check if user is signed in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem sản phẩm yêu thích", Toast.LENGTH_SHORT).show();
            emptyView.setText("Vui lòng đăng nhập để xem sản phẩm yêu thích");
            emptyView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            return;
        }

        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("favorites")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    favoriteProducts.clear();
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot document : value) {
                            Product product = document.toObject(Product.class);
                            favoriteProducts.add(product);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void updateEmptyView() {
        if (favoriteProducts.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}