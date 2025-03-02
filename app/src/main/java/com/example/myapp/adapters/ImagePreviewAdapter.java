package com.example.myapp.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapp.R;

import java.util.ArrayList;
import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {
    private List<Object> imageList = new ArrayList<>();  // Có thể chứa Uri hoặc String URL
    private final Context context;
    private final OnImageDeleteListener listener;

    public interface OnImageDeleteListener {
        void onImageDelete(int position);
    }

    public ImagePreviewAdapter(Context context, OnImageDeleteListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Object image = imageList.get(position);

        // Load ảnh với Glide
        if (image instanceof Uri) {
            Glide.with(context)
                    .load((Uri) image)
                    .centerCrop()
                    .into(holder.imageView);
        } else if (image instanceof String) {
            Glide.with(context)
                    .load((String) image)
                    .centerCrop()
                    .into(holder.imageView);
        }

        // Xử lý xóa ảnh
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public void updateImages(List<Object> newImages) {
        this.imageList = newImages;
        notifyDataSetChanged();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView btnDelete;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgPreview);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
