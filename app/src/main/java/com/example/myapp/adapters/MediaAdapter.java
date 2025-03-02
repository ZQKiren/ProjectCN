package com.example.myapp.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapp.R;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {
    private final List<String> mediaUrls;

    public MediaAdapter(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        String mediaUrl = mediaUrls.get(position);
        Glide.with(holder.itemView.getContext())
                .load(mediaUrl)
                .into(holder.imageViewMedia);
    }

    @Override
    public int getItemCount() {
        return mediaUrls.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMedia;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewMedia = itemView.findViewById(R.id.imageViewMedia);
        }
    }
}

