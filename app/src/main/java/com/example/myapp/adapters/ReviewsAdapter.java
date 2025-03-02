package com.example.myapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapp.R;
import com.example.myapp.data.Review;

import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
    private final List<Review> reviews;

    public ReviewsAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.tvUserName.setText(review.getUserName());
        holder.tvComment.setText(review.getComment());
        holder.ratingBar.setRating(review.getRating());

        // Hiển thị avatar
        if (review.getUserAvatarUrl() != null && !review.getUserAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(review.getUserAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(holder.imgUserAvatar);
        } else {
            holder.imgUserAvatar.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Xử lý RecyclerView cho media
        if (review.getMediaUrls() != null && !review.getMediaUrls().isEmpty()) {
            holder.recyclerViewMedia.setVisibility(View.VISIBLE);
            holder.recyclerViewMedia.setLayoutManager(new LinearLayoutManager(
                    holder.recyclerViewMedia.getContext(),
                    LinearLayoutManager.HORIZONTAL, // Hiển thị danh sách ngang
                    false
            ));
            MediaAdapter mediaAdapter = new MediaAdapter(review.getMediaUrls());
            holder.recyclerViewMedia.setAdapter(mediaAdapter);
        } else {
            holder.recyclerViewMedia.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvComment;
        RatingBar ratingBar;
        ImageView imgUserAvatar;
        RecyclerView recyclerViewMedia;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvComment = itemView.findViewById(R.id.tvComment);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            recyclerViewMedia = itemView.findViewById(R.id.recyclerViewMedia);
        }
    }
}
