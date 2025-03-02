package com.example.myapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.myapp.utils.InputValidator;
import com.example.myapp.data.Product;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddProductFragment extends Fragment {

    private TextInputEditText etProductName, etCategory, etDescription;
    private TextInputEditText etPrice, etDiscount, etSizes, etQuantity;

    private Button btnChooseColors;
    private TextView tvSelectedColors;
    private final List<String> selectedColors = new ArrayList<>();

    private Button btnChooseImages;
    private TextView tvImageCount;
    private RecyclerView recyclerViewImages;
    private ImagePreviewAdapter imageAdapter;
    private ImageView btnClearImages;
    private final List<Uri> imageUris = new ArrayList<>();
    private final List<String> images = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;


    private Button btnSaveProduct;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_product, container, false);
        initViews(view);
        setupImageSystem();
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        initProductInfoViews(view);
        initColorViews(view);
        initImageViews(view);
        initActionViews(view);
    }

    private void initProductInfoViews(View view) {
        etProductName = view.findViewById(R.id.etProductName);
        etCategory = view.findViewById(R.id.etCategory);
        etDescription = view.findViewById(R.id.etDescription);
        etPrice = view.findViewById(R.id.etPrice);
        etDiscount = view.findViewById(R.id.etDiscount);
        etSizes = view.findViewById(R.id.etSizes);
        etQuantity = view.findViewById(R.id.etQuantity);
    }

    private void initColorViews(View view) {
        btnChooseColors = view.findViewById(R.id.btnChooseColors);
        tvSelectedColors = view.findViewById(R.id.tvSelectedColors);
    }

    private void initImageViews(View view) {
        btnChooseImages = view.findViewById(R.id.btnChooseImages);
        tvImageCount = view.findViewById(R.id.tvImageCount);
        recyclerViewImages = view.findViewById(R.id.recyclerViewImages);
        btnClearImages = view.findViewById(R.id.btnClearImages);
    }

    private void initActionViews(View view) {
        btnSaveProduct = view.findViewById(R.id.btnSaveProduct);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupImageSystem() {
        setupImagePickerLauncher();
        setupRecyclerView();
    }

    private void setupClickListeners() {
        setupColorClickListener();
        setupImageClickListeners();
        setupSaveClickListener();
    }

    private void setupColorClickListener() {
        btnChooseColors.setOnClickListener(v -> chooseColors());
    }

    private void setupImageClickListeners() {
        btnChooseImages.setOnClickListener(v -> chooseImages());
        btnClearImages.setOnClickListener(v -> showClearImagesDialog());
    }

    private void setupSaveClickListener() {
        btnSaveProduct.setOnClickListener(v -> {
            if (validateInput()) {
                uploadImagesAndSaveProduct();
            }
        });
    }

    private boolean validateInput() {
        if (!InputValidator.validateProduct(
                etProductName,
                etCategory,
                etDescription,
                etPrice,
                etDiscount,
                etSizes,
                etQuantity,
                selectedColors,
                imageUris
        )) {
            return false;
        }

        if (selectedColors.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn ít nhất một màu", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn ít nhất một ảnh", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setupRecyclerView() {
        imageAdapter = new ImagePreviewAdapter(requireContext(), position -> {
            imageUris.remove(position);
            updateImagesPreview();
        });

        recyclerViewImages.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        recyclerViewImages.setAdapter(imageAdapter);
    }

    private void showClearImagesDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh")
                .setMessage("Bạn có chắc muốn xóa tất cả ảnh không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    imageUris.clear();
                    updateImagesPreview();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateImagesPreview() {
        List<Object> previewList = new ArrayList<>(imageUris);
        imageAdapter.updateImages(previewList);
        tvImageCount.setText(previewList.size() + " hình đã chọn");
        btnClearImages.setVisibility(previewList.size() > 0 ? View.VISIBLE : View.GONE);
    }

    // Phương thức chọn màu
    private void chooseColors() {
        final String[] colorOptions = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#000000", "#FFFFFF"};
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

    private void updateSelectedColorsDisplay() {
        if (selectedColors.isEmpty()) {
            tvSelectedColors.setText("Màu đã chọn: Không có");
        } else {
            tvSelectedColors.setText("Màu đã chọn: " + String.join(", ", selectedColors));
        }
    }

    private void chooseImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(intent);
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
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

    private void uploadImagesAndSaveProduct() {
        progressBar.setVisibility(View.VISIBLE);
        btnSaveProduct.setEnabled(false);

        saveProductToFirestore(productId -> {
            if (imageUris.isEmpty()) {
                updateProductWithImages(productId);
                return;
            }

            for (Uri uri : imageUris) {
                uploadImageToStorage(uri, productId);
            }
        });
    }

    private void uploadImageToStorage(Uri imageUri, String productId) {
        String fileName = "products/" + productId + "/" + UUID.randomUUID().toString();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uriResult -> {
                    images.add(uriResult.toString());

                    // Kiểm tra nếu tất cả ảnh đã được tải lên
                    if (images.size() == imageUris.size()) {
                        updateProductWithImages(productId);
                    }
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSaveProduct.setEnabled(true);
                });
    }

    private void saveProductToFirestore(OnProductIdGenerated callback) {
        String name = Objects.requireNonNull(etProductName.getText()).toString().trim();
        String category = Objects.requireNonNull(etCategory.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        double price = Double.parseDouble(Objects.requireNonNull(etPrice.getText()).toString().trim());
        int discount = Integer.parseInt(Objects.requireNonNull(etDiscount.getText()).toString().trim());
        List<String> sizes = Arrays.asList(Objects.requireNonNull(etSizes.getText()).toString().split(","));
        int quantity = Integer.parseInt(Objects.requireNonNull(etQuantity.getText()).toString().trim());

        // Tạo đối tượng sản phẩm chưa có ID
        Product product = new Product(name, category, description, price, discount, sizes, selectedColors, new ArrayList<>(), quantity);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    String productId = documentReference.getId(); // Lấy ID sản phẩm
                    product.setId(productId);

                    // Lưu lại sản phẩm với ID
                    db.collection("products").document(productId).set(product)
                            .addOnSuccessListener(aVoid -> callback.onGenerated(productId))
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Cập nhật sản phẩm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                btnSaveProduct.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Thêm sản phẩm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSaveProduct.setEnabled(true);
                });
    }

    private void updateProductWithImages(String productId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products").document(productId)
                .update("images", images) // Cập nhật danh sách URL ảnh
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSaveProduct.setEnabled(true);

                    if (getActivity() != null) {
                        getActivity().onBackPressed(); // Quay lại màn hình trước
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Cập nhật ảnh sản phẩm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSaveProduct.setEnabled(true);
                });
    }

    public interface OnProductIdGenerated {
        void onGenerated(String productId);
    }
}
