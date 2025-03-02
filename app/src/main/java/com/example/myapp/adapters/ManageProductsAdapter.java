package com.example.myapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapp.R;
import com.example.myapp.data.Product;
import com.example.myapp.fragments.ProductDetailAdminFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageProductsAdapter extends RecyclerView.Adapter<ManageProductsAdapter.ProductViewHolder> {

    private final List<Product> productList;
    private final Context context;
    private final OnProductUpdateListener updateListener;

    // Map để lưu trữ ID tài liệu Firebase tương ứng với mỗi sản phẩm
    private final Map<Product, String> productIdMap = new HashMap<>();

    public ManageProductsAdapter(Context context, List<Product> productList, OnProductUpdateListener updateListener) {
        this.context = context;
        this.productList = productList;
        this.updateListener = updateListener;
    }

    public interface OnProductUpdateListener {
        void onUpdateProduct(Product product, String productId);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_products, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvProductName.setText(product.getName());
        holder.tvProductCategory.setText(product.getCategory());

        // Định dạng giá gốc
        int originalPrice = (int) product.getPrice(); // Giá gốc (trong trường hợp đã có)
        int discountedPrice = originalPrice;

        // Tính giá sau giảm nếu có giảm giá
        if (product.getOfferPercentage() > 0) {
            discountedPrice = (int) (originalPrice * (1 - product.getOfferPercentage() / 100.0));
        }

        // Hiển thị giá gốc (ẩn nếu không có giảm giá)
        if (product.getOfferPercentage() > 0) {
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(String.format("%,d đ", originalPrice));
        } else {
            holder.tvOriginalPrice.setVisibility(View.GONE);
        }

        // Hiển thị giá đã giảm
        holder.tvProductPrice.setText(String.format("%,d đ", discountedPrice));

        // Load ảnh sản phẩm
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(context)
                    .load(product.getImages().get(0))
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .into(holder.imgProductImage);
        } else {
            holder.imgProductImage.setImageResource(R.drawable.ic_placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            ProductDetailAdminFragment productDetailAdminFragment = ProductDetailAdminFragment.newInstance(product);

            if (context instanceof FragmentActivity) {
                FragmentTransaction transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content_frame, productDetailAdminFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            } else {
                Toast.makeText(context, "Không thể mở chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showOptionsDialog(product, position);
            return true;
        });
    }


    private void showOptionsDialog(Product product, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Chọn hành động")
                .setItems(new CharSequence[]{"Cập nhật", "Xóa"}, (dialog, which) -> {
                    String productId = productIdMap.get(product); // Lấy ID sản phẩm từ `productIdMap`

                    if (productId == null) {
                        Toast.makeText(context, "Không tìm thấy ID sản phẩm để thực hiện thao tác", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (which == 0) {
                        // Xử lý cập nhật sản phẩm
                        updateListener.onUpdateProduct(product, productId); // Gửi cả `product` và `productId` tới listener
                    } else if (which == 1) {
                        // Xử lý xóa sản phẩm
                        deleteProduct(product, position, productId);
                    }
                })
                .show();
    }

    private void deleteProduct(Product product, int position, String productId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Lấy danh sách URL ảnh từ sản phẩm
        List<String> imageUrls = product.getImages();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            // Xóa từng ảnh trong Storage
            for (String imageUrl : imageUrls) {
                deleteImageFromStorage(imageUrl);
            }
        }

        db.collection("products").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    productList.remove(position);
                    productIdMap.remove(product); // Xóa sản phẩm khỏi `productIdMap`
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Xóa sản phẩm thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Lỗi khi xóa sản phẩm", Toast.LENGTH_SHORT).show());
    }

    private void deleteImageFromStorage(String imageUrl) {
        StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

        photoRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Ảnh đã được xóa thành công
                    Toast.makeText(context, "Xóa sản phẩm thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Lỗi khi xóa ảnh
                    Toast.makeText(context, "Lỗi khi xóa sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductCategory, tvProductPrice, tvOriginalPrice;
        ImageView imgProductImage;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductCategory = itemView.findViewById(R.id.tvProductCategory);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            imgProductImage = itemView.findViewById(R.id.imgProductImage);
        }
    }

    // Phương thức để thêm Product vào adapter và lưu ID tài liệu tương ứng
    public void addProduct(Product product, String productId) {
        productList.add(product);
        productIdMap.put(product, productId); // Lưu ID vào `productIdMap`
        notifyDataSetChanged();
    }
}
