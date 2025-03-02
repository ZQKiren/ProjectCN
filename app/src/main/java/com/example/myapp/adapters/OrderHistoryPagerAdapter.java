package com.example.myapp.adapters;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.myapp.data.Order;
import com.example.myapp.fragments.OrderListFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class OrderHistoryPagerAdapter extends FragmentStateAdapter {
    private final String userId;

    public OrderHistoryPagerAdapter(FragmentActivity fa) {
        super(fa);
        this.userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);

        switch (position) {
            case 0:
                args.putString("status", null); // Tất cả
                break;
            case 1:
                args.putString("status", Order.OrderStatus.PENDING.name());
                break;
            case 2:
                args.putString("status", Order.OrderStatus.CONFIRMED.name());
                break;
            case 3:
                args.putString("status", Order.OrderStatus.PROCESSING.name());
                break;
            case 4:
                args.putString("status", Order.OrderStatus.SHIPPING.name());
                break;
            case 5:
                args.putString("status", Order.OrderStatus.DELIVERED.name());
                break;
            case 6:
                args.putString("status", Order.OrderStatus.CANCELLED.name());
                break;
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 7;
    }
}
