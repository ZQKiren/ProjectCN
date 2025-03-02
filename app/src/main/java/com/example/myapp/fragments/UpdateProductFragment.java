package com.example.myapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.adapters.ImagePreviewAdapter;
import com.example.myapp.data.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class UpdateProductFragment extends Fragment {

    private EditText etProductName, etCategory, etDescription, etPrice, etDiscount, etSizes, etQuantity;
    private Button btnChooseColors, btnChooseImages, btnUpdateProduct;
    private TextView tvSelectedColors, tvImageCount;
    private ProgressBar progressBar;

    private Product product;
    private String productId; // ID của sản phẩm trong Firestore
    private final List<String> selectedColors = new ArrayList<>();
    private final List<Uri> imageUris = new ArrayList<>();
    private final List<String> images = new ArrayList<>(); // URLs của ảnh sau khi tải lên
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private RecyclerView recyclerViewImages;
    private ImagePreviewAdapter imageAdapter;
    private ImageView btnClearImages;
    private boolean hasNewImages = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_product, container, false);

        initializeViews(view);
        setupImagePickerLauncher();

        // Retrieve `product` and `productId` from the Bundle using Parcelable
        Bundle bundle = getArguments();
        if (bundle != null) {
            product = bundle.getParcelable("product");
            productId = bundle.getString("productId");

            if (product != null && productId != null) {
                displayProductData();
            } else {
                Toast.makeText(getContext(), "Không thể tải dữ liệu sản phẩm", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        }

        btnChooseColors.setOnClickListener(v -> chooseColors());
        btnChooseImages.setOnClickListener(v -> chooseImages());
        btnUpdateProduct.setOnClickListener(v -> updateProductWithImages());

        return view;
    }

    private void initializeViews(View view) {
        etProductName = view.findViewById(R.id.etProductName);
        etCategory = view.findViewById(R.id.etCategory);
        etDescription = view.findViewById(R.id.etDescription);
        etPrice = view.findViewById(R.id.etPrice);
        etDiscount = view.findViewById(R.id.etDiscount);
        etSizes = view.findViewById(R.id.etSizes);
        etQuantity = view.findViewById(R.id.etQuantity);
        btnChooseColors = view.findViewById(R.id.btnChooseColors);
        btnChooseImages = view.findViewById(R.id.btnChooseImages);
        btnUpdateProduct = view.findViewById(R.id.btnUpdateProduct);
        tvSelectedColors = view.findViewById(R.id.tvSelectedColors);
        tvImageCount = view.findViewById(R.id.tvImageCount);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerViewImages = view.findViewById(R.id.recyclerViewImages);
        btnClearImages = view.findViewById(R.id.btnClearImages);

        // Setup RecyclerView
        imageAdapter = new ImagePreviewAdapter(requireContext(), position -> {
            // Xử lý xóa một ảnh
            if (hasNewImages) {
                imageUris.remove(position);
            } else {
                images.remove(position);
            }
            updateImagesPreview();
        });

        recyclerViewImages.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        recyclerViewImages.setAdapter(imageAdapter);

        btnClearImages.setOnClickListener(v -> showClearImagesDialog());
    }

    private void showClearImagesDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh")
                .setMessage("Bạn có chắc muốn xóa tất cả ảnh không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    imageUris.clear();
                    images.clear();
                    hasNewImages = false;
                    updateImagesPreview();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateImagesPreview() {
        List<Object> previewList = new ArrayList<>();
        if (hasNewImages) {
            previewList.addAll(imageUris);
        } else {
            previewList.addAll(images);
        }

        imageAdapter.updateImages(previewList);
        tvImageCount.setText(previewList.size() + " hình đã chọn");
        btnClearImages.setVisibility(previewList.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private void displayProductData() {
        // Hiển thị thông tin sản phẩm
        etProductName.setText(product.getName());
        etCategory.setText(product.getCategory());
        etDescription.setText(product.getDescription());
        etPrice.setText(String.valueOf(product.getPrice()));
        etDiscount.setText(String.valueOf(product.getOfferPercentage()));
        etSizes.setText(String.join(", ", product.getSizes()));
        etQuantity.setText(String.valueOf(product.getQuantity()));

        // Hiển thị màu sắc
        selectedColors.addAll(product.getColors());
        updateSelectedColorsDisplay();

        // Hiển thị số lượng ảnh
        images.addAll(product.getImages());
        updateImagesPreview();
        tvImageCount.setText(images.size() + " hình đã chọn");
    }

    private void updateProductWithImages() {
        if (productId == null) {
            Toast.makeText(getContext(), "ID sản phẩm không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpdateProduct.setEnabled(false);

        // Xóa tất cả ảnh cũ trước khi tải ảnh mới
        deleteOldImages(() -> {
            if (imageUris.isEmpty()) {
                // Nếu không có ảnh mới, chỉ cập nhật sản phẩm
                saveProductToFirestore();
                return;
            }

            // Tải ảnh mới lên Firebase Storage
            final int totalImages = imageUris.size();
            final List<String> newImageUrls = new ArrayList<>();

            for (Uri uri : imageUris) {
                // Tạo tên file ngẫu nhiên trong thư mục sản phẩm
                String fileName = "products/" + productId + "/" + UUID.randomUUID().toString();
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);

                storageRef.putFile(uri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                            newImageUrls.add(downloadUrl.toString());

                            if (newImageUrls.size() == totalImages) {
                                images.clear();
                                images.addAll(newImageUrls); // Thêm URL mới vào danh sách ảnh
                                saveProductToFirestore();
                            }
                        }))
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            btnUpdateProduct.setEnabled(true);
                        });
            }
        });
    }

    private void deleteOldImages(Runnable onComplete) {
        if (images.isEmpty()) {
            // Không có ảnh cũ, tiếp tục
            onComplete.run();
            return;
        }

        final int[] deletedCount = {0};
        for (String imageUrl : images) {
            FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete()
                    .addOnSuccessListener(aVoid -> {
                        deletedCount[0]++;
                        if (deletedCount[0] == images.size()) {
                            onComplete.run(); // Khi tất cả ảnh đã được xóa, tiếp tục
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Xóa ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveProductToFirestore() {
        // Cập nhật dữ liệu sản phẩm
        product.setName(etProductName.getText().toString().trim());
        product.setCategory(etCategory.getText().toString().trim());
        product.setDescription(etDescription.getText().toString().trim());
        product.setPrice(Double.parseDouble(etPrice.getText().toString().trim()));
        product.setOfferPercentage(Integer.parseInt(etDiscount.getText().toString().trim()));
        product.setSizes(Arrays.asList(etSizes.getText().toString().split(",")));
        product.setColors(selectedColors);
        product.setImages(images); // Cập nhật URL ảnh mới
        product.setQuantity(Integer.parseInt(etQuantity.getText().toString().trim()));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products").document(productId)
                .set(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cập nhật sản phẩm thành công", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnUpdateProduct.setEnabled(true);

                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Cập nhật sản phẩm thất bại", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnUpdateProduct.setEnabled(true);
                });
    }

    private void updateSelectedColorsDisplay() {
        if (selectedColors.isEmpty()) {
            tvSelectedColors.setText("Màu đã chọn: Không có");
        } else {
            tvSelectedColors.setText("Màu đã chọn: " + String.join(", ", selectedColors));
        }
    }

    private void chooseColors() {
        final String[] colorNames = {"Đỏ", "Xanh Lá", "Xanh Dương", "Vàng", "Hồng", "Xanh Ngọc", "Cam", "Đen", "Trắng"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn màu sắc");
        builder.setItems(colorNames, (dialog, which) -> {
            String selectedColor = colorNames[which];
            if (!selectedColors.contains(selectedColor)) {
                selectedColors.add(selectedColor);
                updateSelectedColorsDisplay();
            } else {
                Toast.makeText(getContext(), "Màu đã chọn trước đó!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void chooseImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(intent);
    }

    // Cập nhật phương thức setupImagePickerLauncher
    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        hasNewImages = true;
                        imageUris.clear();

                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                imageUris.add(imageUri);
                            }
                        } else if (result.getData().getData() != null) {
                            Uri imageUri = result.getData().getData();
                            imageUris.add(imageUri);
                        }

                        updateImagesPreview();
                    }
                }
        );
    }
}
