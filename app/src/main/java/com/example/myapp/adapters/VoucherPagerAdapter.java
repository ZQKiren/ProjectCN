package com.example.myapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.myapp.fragments.AvailableVouchersFragment;
import com.example.myapp.fragments.MyVouchersFragment;

public class VoucherPagerAdapter extends FragmentStateAdapter {
    private final String userId;  // Đổi thành String
    private final int userPoints;

    public VoucherPagerAdapter(FragmentActivity fa, String userId, int userPoints) {
        super(fa);
        this.userId = userId;     // Không cần chuyển đổi
        this.userPoints = userPoints;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> AvailableVouchersFragment.newInstance(userId, userPoints);  // Truyền trực tiếp userId
            case 1 -> MyVouchersFragment.newInstance(userId);  // Truyền trực tiếp userId
            default -> throw new IllegalStateException("Invalid position " + position);
        };
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
