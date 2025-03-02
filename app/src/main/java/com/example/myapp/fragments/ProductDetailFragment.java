package com.example.myapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapp.R;
import com.example.myapp.adapters.MediaPreviewAdapter;
import com.example.myapp.adapters.ProductImagesAdapter;
import com.example.myapp.adapters.ReviewsAdapter;
import com.example.myapp.data.Product;
import com.example.myapp.data.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProductDetailFragment extends Fragment {

    private Product product;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ViewPager2 viewPagerProductImages;
    private Button btnAddToCart, btnSubmitReview;
    private EditText etReviewComment;
    private MaterialButton btnAddToFavorite;
    private RatingBar ratingBar;
    private TextView tvProductName, tvProductCategory, tvProductDescription, tvProductOriginalPrice, tvProductPrice, tvProductOffer, tvProductSizes, tvProductColors, tvAverageRating;
    private RecyclerView recyclerViewReviews, recyclerViewMediaPreview;
    private List<Review> reviews;
    RatingBar ratingBarAverage;
    private ReviewsAdapter reviewsAdapter;
    ImageView imgAddMedia;
    private final List<Uri> mediaPreviewList = new ArrayList<>();
    private MediaPreviewAdapter mediaPreviewAdapter; // Adapter để hiển thị ảnh/video

    public static ProductDetailFragment newInstance(Product product) {
        ProductDetailFragment fragment = new ProductDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable("product", product);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_detail, container, false);

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Lấy thông tin sản phẩm từ Bundle
        product = getArguments() != null ? getArguments().getParcelable("product") : null;

        // Ánh xạ các thành phần giao diện
        viewPagerProductImages = view.findViewById(R.id.viewPagerProductImages);
        tvProductName = view.findViewById(R.id.tvProductName);
        tvProductCategory = view.findViewById(R.id.tvProductCategory);
        tvProductDescription = view.findViewById(R.id.tvProductDescription);
        tvProductOriginalPrice = view.findViewById(R.id.tvProductOriginalPrice);
        tvProductPrice = view.findViewById(R.id.tvProductPrice);
        tvProductOffer = view.findViewById(R.id.tvProductOffer);
        tvProductSizes = view.findViewById(R.id.tvProductSizes);
        tvProductColors = view.findViewById(R.id.tvProductColors);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);
        btnAddToFavorite = view.findViewById(R.id.btnAddToFavorite);
        ratingBar = view.findViewById(R.id.ratingBar);
        etReviewComment = view.findViewById(R.id.etReviewComment);
        btnSubmitReview = view.findViewById(R.id.btnSubmitReview);
        tvAverageRating = view.findViewById(R.id.tvAverageRating);
        recyclerViewReviews = view.findViewById(R.id.recyclerViewReviews);
        imgAddMedia = view.findViewById(R.id.imgAddMedia);
        recyclerViewMediaPreview = view.findViewById(R.id.recyclerViewMediaPreview);
        tvAverageRating = view.findViewById(R.id.tvAverageRating);
        ratingBarAverage = view.findViewById(R.id.ratingBarAverage);


        // Hiển thị thông tin sản phẩm
        if (product != null) {
            // Hiển thị ảnh sản phẩm
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                ProductImagesAdapter adapter = new ProductImagesAdapter(getContext(), product.getImages());
                viewPagerProductImages.setAdapter(adapter);
            }

            // Hiển thị thông tin chi tiết sản phẩm
            tvProductName.setText(product.getName());
            tvProductCategory.setText(product.getCategory());
            tvProductDescription.setText(product.getDescription());

            // Hiển thị giá
            if (product.getOfferPercentage() > 0) {
                double originalPrice = product.getPrice();
                double discountedPrice = originalPrice * (1 - product.getOfferPercentage() / 100.0);

                tvProductOriginalPrice.setVisibility(View.VISIBLE);
                tvProductOriginalPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(originalPrice));

                tvProductPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(discountedPrice));
                tvProductOffer.setVisibility(View.VISIBLE);
                tvProductOffer.setText(String.format("Giảm %d%%", product.getOfferPercentage()));
            } else {
                tvProductOriginalPrice.setVisibility(View.GONE);
                tvProductOffer.setVisibility(View.GONE);
                tvProductPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(product.getPrice()));
            }

            // Hiển thị kích thước và màu sắc
            tvProductSizes.setText(MessageFormat.format("Kích thước: {0}", String.join(", ", product.getSizes())));
            tvProductColors.setText(MessageFormat.format("Màu sắc: {0}", String.join(", ", product.getColors())));

            // Xử lý sự kiện nút
            btnAddToCart.setOnClickListener(v -> addToCart());
            btnAddToFavorite.setOnClickListener(v -> toggleFavorite());
        }
        reviews = new ArrayList<>();
        reviewsAdapter = new ReviewsAdapter(reviews);
        recyclerViewReviews.setAdapter(reviewsAdapter);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));

        // Thiết lập RecyclerView cho media
        mediaPreviewAdapter = new MediaPreviewAdapter(mediaPreviewList);
        recyclerViewMediaPreview.setAdapter(mediaPreviewAdapter);
        recyclerViewMediaPreview.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Xử lý nút thêm media
        imgAddMedia.setOnClickListener(v -> openMediaPicker());

        // Xử lý nút gửi đánh giá
        btnSubmitReview.setOnClickListener(v -> submitReview());

        // Lấy dữ liệu đánh giá và nhận xét
        fetchReviews();
        checkFavoriteStatus();

        return view;
    }

    private void openMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Cho phép chọn nhiều tệp
        startActivityForResult(intent, 1002);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    mediaPreviewList.add(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                mediaPreviewList.add(uri);
            }
            mediaPreviewAdapter.notifyDataSetChanged();
        }
    }

    private void toggleFavorite() {
        if (auth.getCurrentUser() == null || product == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để thực hiện chức năng này", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DocumentReference favoriteRef = db.collection("users").document(userId)
                .collection("favorites").document(product.getId());

        favoriteRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Remove from favorites
                favoriteRef.delete().addOnSuccessListener(aVoid -> {
                    updateFavoriteButton(false);
                    Toast.makeText(getContext(), "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                });
            } else {
                // Add to favorites
                favoriteRef.set(product).addOnSuccessListener(aVoid -> {
                    updateFavoriteButton(true);
                    Toast.makeText(getContext(), "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addToCart() {
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid(); // Lấy UID người dùng đã đăng nhập

        // Thực hiện lưu sản phẩm vào giỏ hàng
        db.collection("users").document(userId).collection("cart")
                .document(product.getId())
                .set(product)
                .addOnSuccessListener(aVoid -> {
                    // Thông báo thêm vào giỏ hàng thành công
                    Toast.makeText(getContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi có lỗi xảy ra
                    Toast.makeText(getContext(), "Lỗi khi thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void checkFavoriteStatus() {
        if (auth.getCurrentUser() == null || product == null) return;

        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("favorites")
                .document(product.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isFavorite = documentSnapshot.exists();
                    updateFavoriteButton(isFavorite);
                });
    }

    private void updateFavoriteButton(boolean isFavorite) {
        int color = getResources().getColor(isFavorite ? R.color.primary : R.color.text_secondary);

        btnAddToFavorite.setIconTint(ColorStateList.valueOf(color));
        btnAddToFavorite.setStrokeColor(ColorStateList.valueOf(color));
    }

    private void submitReview() {
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        // Hiển thị ProgressBar
        ProgressBar progressBar = requireView().findViewById(R.id.progressBarLoading);
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userName = documentSnapshot.getString("fullName");
                String userAvatarUrl = documentSnapshot.getString("avatarUrl");
                float rating = ratingBar.getRating();
                String comment = etReviewComment.getText().toString().trim();

                if (rating == 0) {
                    progressBar.setVisibility(View.GONE); // Ẩn ProgressBar
                    Toast.makeText(getContext(), "Vui lòng chọn số sao để đánh giá!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (comment.isEmpty() && mediaPreviewList.isEmpty()) {
                    progressBar.setVisibility(View.GONE); // Ẩn ProgressBar
                    Toast.makeText(getContext(), "Vui lòng nhập nhận xét hoặc thêm ảnh/video!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Review review = new Review(userId, userName, userAvatarUrl, rating, comment);

                if (mediaPreviewList.isEmpty()) {
                    saveReviewToFirestore(review, new ArrayList<>(), progressBar);
                    return;
                }

                uploadMediaFiles(mediaPreviewList, urls -> {
                    review.setMediaUrls(urls);
                    saveReviewToFirestore(review, urls, progressBar);
                });
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE); // Ẩn ProgressBar khi có lỗi
            Toast.makeText(getContext(), "Không thể lấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        });
    }

    private void uploadMediaFiles(List<Uri> mediaUris, OnMediaUploadCompleteListener listener) {
        List<String> uploadedUrls = new ArrayList<>();
        if (mediaUris.isEmpty()) {
            listener.onUploadComplete(uploadedUrls);
            return;
        }

        for (Uri uri : mediaUris) {
            String fileName = "reviews/" + System.currentTimeMillis() + "_" + Objects.requireNonNull(uri.getLastPathSegment());
            FirebaseStorage.getInstance().getReference(fileName).putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw Objects.requireNonNull(task.getException());
                        }
                        return FirebaseStorage.getInstance().getReference(fileName).getDownloadUrl();
                    })
                    .addOnSuccessListener(uriResult -> {
                        uploadedUrls.add(uriResult.toString());
                        if (uploadedUrls.size() == mediaUris.size()) { // Khi tất cả các tệp đã được tải lên
                            listener.onUploadComplete(uploadedUrls);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Không thể tải lên tệp!", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        }
    }

    // Interface để xử lý kết quả tải lên
    private interface OnMediaUploadCompleteListener {
        void onUploadComplete(List<String> uploadedUrls);
    }

    private void saveReviewToFirestore(Review review, List<String> mediaUrls, ProgressBar progressBar) {
        db.collection("products").document(product.getId()).collection("reviews")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE); // Ẩn ProgressBar sau khi thành công
                    Toast.makeText(getContext(), "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                    review.setMediaUrls(mediaUrls);
                    reviews.add(review);
                    reviewsAdapter.notifyDataSetChanged();
                    calculateAverageRating();

                    ratingBar.setRating(0);
                    etReviewComment.setText("");
                    mediaPreviewList.clear();
                    mediaPreviewAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE); // Ẩn ProgressBar khi thất bại
                    Toast.makeText(getContext(), "Lỗi khi gửi đánh giá!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void fetchReviews() {
        db.collection("products").document(product.getId()).collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviews.clear();
                    reviews.addAll(queryDocumentSnapshots.toObjects(Review.class));
                    reviewsAdapter.notifyDataSetChanged();
                    calculateAverageRating(); // Cập nhật đánh giá trung bình
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Không thể lấy nhận xét", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void calculateAverageRating() {
        if (reviews.isEmpty()) {
            tvAverageRating.setText("0.0/5.0");
            ratingBarAverage.setRating(0);
            return;
        }

        float sum = 0;
        for (Review review : reviews) {
            sum += review.getRating();
        }
        float average = sum / reviews.size();

        // Cập nhật text hiển thị
        tvAverageRating.setText(String.format("%.1f/5.0", average));

        // Cập nhật RatingBar
        ratingBarAverage.setRating(average);
    }
}

