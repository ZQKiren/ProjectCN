package com.example.myapp.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.example.myapp.data.User;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserDetailFragment extends Fragment {

    private static final String TAG = "UserDetailFragment";
    private static final String ARG_USER_ID = "arg_user_id";

    private ImageView avatarImageView;
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneNumberTextView;
    private TextView genderTextView;
    private TextView roleTextView;

    private FirebaseFirestore db;
    private String userId;

    public static UserDetailFragment newInstance(String userId) {
        UserDetailFragment fragment = new UserDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId); // Lưu userId vào Bundle
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_detail, container, false);

        // Ánh xạ View
        avatarImageView = view.findViewById(R.id.avatarImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        phoneNumberTextView = view.findViewById(R.id.phoneNumberTextView);
        genderTextView = view.findViewById(R.id.genderTextView);
        roleTextView = view.findViewById(R.id.roleTextView);

        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID); // Lấy userId từ Bundle
        } else {
            Log.e(TAG, "No user ID passed to fragment");
        }

        db = FirebaseFirestore.getInstance();

        if (userId != null) {
            loadUserData(userId); // Tải dữ liệu người dùng
        } else {
            Toast.makeText(getContext(), "User ID is missing!", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = parseUserData(documentSnapshot);
                        bindData(user); // Hiển thị dữ liệu người dùng
                    } else {
                        Toast.makeText(getContext(), "User not found!", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "No user found with ID: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching user data", e);
                });
    }

    private User parseUserData(@NonNull com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {
        return new User(
                documentSnapshot.getId(),
                documentSnapshot.getString("fullName"),
                documentSnapshot.getString("email"),
                documentSnapshot.getString("avatarUrl"),
                documentSnapshot.getString("phoneNumber"),
                documentSnapshot.getString("gender"),
                User.Role.fromString(documentSnapshot.getString("role"))
        );
    }

    private void bindData(User user) {
        nameTextView.setText(user.getFullName());
        emailTextView.setText(user.getEmail());
        phoneNumberTextView.setText(user.getPhoneNumber());
        genderTextView.setText(user.getGender());
        roleTextView.setText(user.getRole().toString());
        Glide.with(requireContext())
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_placeholder_image)
                .into(avatarImageView);
    }
}
