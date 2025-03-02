package com.example.myapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.myapp.R;
import com.example.myapp.data.Product;

public class ProductDetailAdminFragment extends Fragment {

    private TextView tvProductName, tvProductCategory, tvProductDescription, tvProductPrice, tvProductOffer;
    private ImageView imgProductImage;
    private TextView tvProductSizes, tvProductColors;

    public static ProductDetailAdminFragment newInstance(Product product) {
        ProductDetailAdminFragment fragment = new ProductDetailAdminFragment();
        Bundle args = new Bundle();
        args.putParcelable("product", product); // Use Parcelable
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_detail_admin, container, false);

        // Bind views
        tvProductName = view.findViewById(R.id.tvProductName);
        tvProductCategory = view.findViewById(R.id.tvProductCategory);
        tvProductDescription = view.findViewById(R.id.tvProductDescription);
        tvProductPrice = view.findViewById(R.id.tvProductPrice);
        tvProductOffer = view.findViewById(R.id.tvProductOffer);
        imgProductImage = view.findViewById(R.id.imgProductImage);
        tvProductSizes = view.findViewById(R.id.tvProductSizes);
        tvProductColors = view.findViewById(R.id.tvProductColors);

        // Retrieve Product from Arguments
        Product product = getArguments() != null ? getArguments().getParcelable("product") : null;

        // Check if product is null
        if (product != null) {
            displayProductDetails(product);
        } else {
            Toast.makeText(getContext(), "Không thể tải dữ liệu sản phẩm", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void displayProductDetails(Product product) {
        tvProductName.setText(product.getName());
        tvProductCategory.setText(product.getCategory());
        tvProductDescription.setText(product.getDescription());
        tvProductPrice.setText(getString(R.string.product_price, product.getPrice()));
        tvProductOffer.setText(getString(R.string.product_offer, product.getOfferPercentage()));

        // Display product image
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(this)
                    .load(product.getImages().get(0))  // Display the first image
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .into(imgProductImage);
        }

        // Display sizes and colors
        tvProductSizes.setText("Kích thước: " + product.getSizes().toString());
        tvProductColors.setText("Màu sắc: " + product.getColors().toString());
    }
}
