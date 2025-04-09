package com.example.myapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapp.R;
import com.example.myapp.data.User;
import com.example.myapp.adapters.ManageAccountsAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ManageAccountsFragment extends Fragment implements ManageAccountsAdapter.UserActionListener {

    private RecyclerView recyclerView;
    private ManageAccountsAdapter adapter;
    private FirebaseFirestore db;
    private final List<User> userList = new ArrayList<>();
    private TextView totalUsersCount, activeUsersCount;

    private EditText searchEditText;
    private final List<User> filteredUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_accounts, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_accounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        totalUsersCount = view.findViewById(R.id.total_users_count);
        activeUsersCount = view.findViewById(R.id.active_users_count);

        db = FirebaseFirestore.getInstance();
        adapter = new ManageAccountsAdapter(filteredUsers, this);
        recyclerView.setAdapter(adapter);

        searchEditText = view.findViewById(R.id.search_edit_text);
        searchEditText.setText("");
        setupSearch();

        loadUsersData();
        return view;
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterUsers(s.toString());
            }
        });
    }

    private void filterUsers(String query) {
        filteredUsers.clear();
        String searchText = query.toLowerCase().trim();

        for(User user : userList) {
            if(user.getFullName().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText)) {
                filteredUsers.add(user);
            }
        }

        int activeCount = countActiveUsers(filteredUsers);
        updateStats(filteredUsers.size(), activeCount);
        adapter.notifyDataSetChanged();
    }

    private int countActiveUsers(List<User> users) {
        int count = 0;
        for (User user : users) {
            if ("online".equals(user.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private void loadUsersData() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    filteredUsers.clear(); // Xóa danh sách đã lọc
                    int activeUsers = 0;

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setId(document.getId());
                            userList.add(user);
                            filteredUsers.add(user); // Thêm vào danh sách đã lọc

                            if ("online".equals(user.getStatus())) {
                                activeUsers++;
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateStats(userList.size(), activeUsers);
                })
                .addOnFailureListener(e -> showError("Không thể tải danh sách người dùng: " + e.getMessage()));
    }

    private void updateStats(int totalUsers, int activeUsers) {
        if (totalUsersCount != null && activeUsersCount != null) {
            totalUsersCount.setText(String.valueOf(totalUsers));
            activeUsersCount.setText(String.valueOf(activeUsers));
        }
    }

    @Override
    public void onUserAction(User user, String action) {
        if ("update".equals(action)) {
            // Xử lý cập nhật thông tin cơ bản nếu cần - không còn cần thiết vì adapter tự xử lý
        } else if ("delete".equals(action)) {
            deleteUser(user);
        }
    }

    @Override
    public void onRoleUpdateSuccess(User user) {
        int position = findUserPosition(user);
        if (position != -1) {
            userList.get(position).setRole(user.getRole());
            adapter.notifyItemChanged(position);
            showSuccess("Đã cập nhật quyền cho " + user.getFullName() + " thành " +
                    getRoleDisplayName(user.getRole()));
        }
    }

    @Override
    public void onRoleUpdateFailure(Exception e) {
        showError("Không thể cập nhật quyền: " + e.getMessage());
    }

    @Override
    public void onUserUpdateSuccess(User user) {
        int position = findUserPosition(user);
        if (position != -1) {
            userList.get(position).setFullName(user.getFullName());
            userList.get(position).setPhoneNumber(user.getPhoneNumber());
            userList.get(position).setGender(user.getGender());
            userList.get(position).setStatus(user.getStatus());
            userList.get(position).setPoints(user.getPoints());

            // Update the filtered list too if needed
            int filteredPosition = findUserPositionInFiltered(user);
            if (filteredPosition != -1) {
                filteredUsers.get(filteredPosition).setFullName(user.getFullName());
                filteredUsers.get(filteredPosition).setPhoneNumber(user.getPhoneNumber());
                filteredUsers.get(filteredPosition).setGender(user.getGender());
                filteredUsers.get(filteredPosition).setStatus(user.getStatus());
                filteredUsers.get(filteredPosition).setPoints(user.getPoints());
            }

            // Update the adapter and stats
            adapter.notifyDataSetChanged();
            int activeCount = countActiveUsers(filteredUsers);
            updateStats(filteredUsers.size(), activeCount);

            showSuccess("Đã cập nhật thông tin cho " + user.getFullName());
        }
    }

    @Override
    public void onUserUpdateFailure(Exception e) {
        showError("Không thể cập nhật thông tin: " + e.getMessage());
    }

    private void deleteUser(User user) {
        db.collection("users").document(user.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    int position = findUserPosition(user);
                    if (position != -1) {
                        userList.remove(position);

                        // Also remove from filtered list if present
                        int filteredPosition = findUserPositionInFiltered(user);
                        if (filteredPosition != -1) {
                            filteredUsers.remove(filteredPosition);
                        }

                        adapter.notifyDataSetChanged();
                        updateStats(filteredUsers.size(), countActiveUsers(filteredUsers));
                        showSuccess("Đã xóa người dùng thành công");
                    }
                })
                .addOnFailureListener(e -> showError("Không thể xóa người dùng: " + e.getMessage()));
    }

    private int findUserPosition(User user) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId().equals(user.getId())) {
                return i;
            }
        }
        return -1;
    }

    private int findUserPositionInFiltered(User user) {
        for (int i = 0; i < filteredUsers.size(); i++) {
            if (filteredUsers.get(i).getId().equals(user.getId())) {
                return i;
            }
        }
        return -1;
    }

    private int countActiveUsers() {
        int count = 0;
        for (User user : userList) {
            if ("online".equals(user.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private String getRoleDisplayName(User.Role role) {
        if (role == User.Role.ADMIN) {
            return "Quản trị viên";
        } else if (role == User.Role.EDITOR) {
            return "Biên tập viên";
        } else if (role == User.Role.VIEWER) {
            return "Người xem";
        } else {
            return "Người dùng";
        }
    }

    private void showSuccess(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}