package com.example.myapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapp.R;
import com.example.myapp.data.User;
import com.example.myapp.fragments.UserDetailFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Objects;

public class ManageAccountsAdapter extends RecyclerView.Adapter<ManageAccountsAdapter.UserViewHolder> {

    private final List<User> users;
    private final UserActionListener actionListener;
    private final FirebaseFirestore db;

    public interface UserActionListener {
        void onUserAction(User user, String action);
        void onRoleUpdateSuccess(User user);
        void onRoleUpdateFailure(Exception e);
        void onUserUpdateSuccess(User user);
        void onUserUpdateFailure(Exception e);
    }

    public ManageAccountsAdapter(List<User> users, UserActionListener actionListener) {
        this.users = users;
        this.actionListener = actionListener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_accounts, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);

        holder.itemView.setOnClickListener(v -> {
            FragmentManager fragmentManager = ((FragmentActivity) holder.itemView.getContext()).getSupportFragmentManager();
            UserDetailFragment fragment = UserDetailFragment.newInstance(user.getId());
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView emailTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);

            itemView.setOnLongClickListener(v -> {
                User user = users.get(getAdapterPosition());
                showUserOptions(user);
                return true;
            });

            itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            itemView.setBackgroundColor(Color.LTGRAY);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            itemView.setBackgroundColor(Color.TRANSPARENT);
                            break;
                    }
                    return false;
                }
            });
        }

        private void showUserOptions(User user) {
            String[] options = {"Cập nhật quyền", "Cập nhật thông tin", "Xóa tài khoản"};
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Chọn thao tác")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showRoleUpdateDialog(user);
                        } else if (which == 1) {
                            showEditUserDialog(user);
                        } else if (which == 2) {
                            actionListener.onUserAction(user, "delete");
                        }
                    })
                    .setNeutralButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        }

        private void showEditUserDialog(User user) {
            Context context = itemView.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_edit_user, null);
            builder.setView(dialogView);

            // Initialize form fields
            TextInputEditText editFullName = dialogView.findViewById(R.id.edit_full_name);
            TextInputEditText editEmail = dialogView.findViewById(R.id.edit_email);
            TextInputEditText editPhone = dialogView.findViewById(R.id.edit_phone);
            TextInputEditText editPoints = dialogView.findViewById(R.id.edit_points);
            RadioGroup genderRadioGroup = dialogView.findViewById(R.id.gender_radio_group);
            RadioGroup statusRadioGroup = dialogView.findViewById(R.id.status_radio_group);
            RadioButton radioMale = dialogView.findViewById(R.id.radio_male);
            RadioButton radioFemale = dialogView.findViewById(R.id.radio_female);
            RadioButton radioActive = dialogView.findViewById(R.id.radio_active);
            RadioButton radioInactive = dialogView.findViewById(R.id.radio_inactive);

            // Set existing user data to form fields
            editFullName.setText(user.getFullName());
            editEmail.setText(user.getEmail());
            editPhone.setText(user.getPhoneNumber());
            editPoints.setText(String.valueOf(user.getPoints()));

            // Set gender selection
            if ("male".equalsIgnoreCase(user.getGender())) {
                radioMale.setChecked(true);
            } else if ("female".equalsIgnoreCase(user.getGender())) {
                radioFemale.setChecked(true);
            }

            // Set status selection
            if ("online".equals(user.getStatus())) {
                radioActive.setChecked(true);
            } else {
                radioInactive.setChecked(true);
            }

            builder.setPositiveButton("Lưu", null); // We'll set this later to prevent auto-dismiss
            builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();

            // Show dialog
            dialog.show();

            // Override the positive button click to validate before dismissing
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Validate form data
                String fullName = Objects.requireNonNull(editFullName.getText()).toString().trim();
                if (fullName.isEmpty()) {
                    editFullName.setError("Vui lòng nhập họ tên");
                    return;
                }

                // Get form values
                String phoneNumber = Objects.requireNonNull(editPhone.getText()).toString().trim();
                String pointsStr = Objects.requireNonNull(editPoints.getText()).toString().trim();
                int points = pointsStr.isEmpty() ? 0 : Integer.parseInt(pointsStr);

                String gender = radioMale.isChecked() ? "male" : "female";
                String status = radioActive.isChecked() ? "online" : "offline";

                // Create updated user object
                user.setFullName(fullName);
                user.setPhoneNumber(phoneNumber);
                user.setGender(gender);
                user.setStatus(status);
                user.setPoints(points);

                // Update user in Firestore
                updateUserInFirestore(user, dialog);
            });
        }

        private void updateUserInFirestore(User user, AlertDialog dialog) {
            db.collection("users").document(user.getId())
                    .update(
                            "fullName", user.getFullName(),
                            "phoneNumber", user.getPhoneNumber(),
                            "gender", user.getGender(),
                            "status", user.getStatus(),
                            "points", user.getPoints()
                    )
                    .addOnSuccessListener(aVoid -> {
                        dialog.dismiss();
                        actionListener.onUserUpdateSuccess(user);
                        Toast.makeText(itemView.getContext(),
                                "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        actionListener.onUserUpdateFailure(e);
                        Toast.makeText(itemView.getContext(),
                                "Lỗi khi cập nhật thông tin: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }

        private void showRoleUpdateDialog(User user) {
            User.Role currentRole = user.getRole();
            User.Role[] roles = User.Role.values();
            String[] roleNames = new String[roles.length];
            int currentRoleIndex = 0;

            for (int i = 0; i < roles.length; i++) {
                roleNames[i] = getRoleDisplayName(roles[i]);
                if (roles[i] == currentRole) {
                    currentRoleIndex = i;
                }
            }

            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Cập nhật quyền người dùng")
                    .setSingleChoiceItems(roleNames, currentRoleIndex, (dialog, which) -> {
                        User.Role newRole = roles[which];
                        if (newRole != currentRole) {
                            updateUserRole(user, newRole);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }

        private String getRoleDisplayName(User.Role role) {
            return switch (role) {
                case ADMIN -> "Quản trị viên";
                case EDITOR -> "Biên tập viên";
                case VIEWER -> "Người xem";
                default -> "Người dùng";
            };
        }

        private void updateUserRole(User user, User.Role newRole) {
            db.collection("users")
                    .document(user.getId())
                    .update("role", newRole.toString())
                    .addOnSuccessListener(aVoid -> {
                        user.setRole(newRole);
                        actionListener.onRoleUpdateSuccess(user);
                        Toast.makeText(itemView.getContext(),
                                "Cập nhật quyền thành công", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        actionListener.onRoleUpdateFailure(e);
                        Toast.makeText(itemView.getContext(),
                                "Lỗi khi cập nhật quyền: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }

        void bind(User user) {
            nameTextView.setText(user.getFullName());
            emailTextView.setText(user.getEmail());
            Glide.with(itemView.getContext())
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(avatarImageView);

            // Cập nhật trạng thái
            TextView statusBadge = itemView.findViewById(R.id.status_badge);
            if ("online".equals(user.getStatus())) {
                statusBadge.setText("Hoạt động");
                statusBadge.setTextColor(Color.GREEN);
            } else {
                statusBadge.setText("Ngoại tuyến");
                statusBadge.setTextColor(Color.RED);
            }
        }
    }
}