package com.example.myapp.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapp.data.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewModel extends AndroidViewModel {

    private static final String TAG = "ProfileViewModel";
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final MutableLiveData<User> userData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false); // Trạng thái tải
    private final MutableLiveData<String> errorState = new MutableLiveData<>(); // Quản lý lỗi

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserData(); // Tải dữ liệu khi ViewModel được tạo
    }

    public LiveData<User> getUserData() {
        return userData;
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            // Bắt đầu tải, bật loading
            loadingState.setValue(true);
            errorState.setValue(null); // Xóa lỗi trước đó nếu có

            db.collection("users").document(firebaseUser.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            loadingState.setValue(false);
                            errorState.setValue("Failed to load user data: " + error.getMessage());
                            Log.e(TAG, "Failed to load user data", error);
                            userData.setValue(null);
                            return;
                        }

                        loadingState.setValue(false);

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            User user = parseUserData(documentSnapshot);
                            userData.setValue(user); // Cập nhật LiveData với dữ liệu người dùng
                            Log.d(TAG, "User data updated in real-time: " + user);
                        } else {
                            errorState.setValue("User data not found.");
                            Log.d(TAG, "User data not found for userId: " + firebaseUser.getUid());
                            userData.setValue(null);
                        }
                    });
        } else {
            userData.setValue(null);
            errorState.setValue("User not logged in.");
            Log.d(TAG, "User is not logged in.");
        }
    }

    private User parseUserData(DocumentSnapshot documentSnapshot) {
        User user = new User(
                documentSnapshot.getId(),
                documentSnapshot.getString("fullName"),
                documentSnapshot.getString("email"),
                documentSnapshot.getString("avatarUrl"),
                documentSnapshot.getString("phoneNumber")
        );
        user.setPhoneNumber(documentSnapshot.getString("phoneNumber"));
        user.setGender(documentSnapshot.getString("gender"));
        user.setRole(User.Role.fromString(documentSnapshot.getString("role")));

        // Thêm đọc điểm
        Long points = documentSnapshot.getLong("points");
        user.setPoints(points != null ? points.intValue() : 0);

        return user;
    }

    public LiveData<Integer> getOrderCount(String userId) {
        MutableLiveData<Integer> orderCountLiveData = new MutableLiveData<>();
        orderCountLiveData.setValue(0); // Khởi tạo giá trị mặc định là 0

        db.collection("orders")
                .whereEqualTo("userId", userId) // Truy vấn theo userId
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int orderCount = queryDocumentSnapshots.size(); // Lấy số lượng đơn hàng
                    orderCountLiveData.setValue(orderCount); // Cập nhật LiveData
                    Log.d(TAG, "Order count loaded: " + orderCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load order count", e);
                    orderCountLiveData.setValue(0); // Nếu lỗi, giữ giá trị mặc định là 0
                });

        return orderCountLiveData;
    }

    public LiveData<Integer> getVoucherCount(String userId) {
        MutableLiveData<Integer> voucherCountLiveData = new MutableLiveData<>();
        voucherCountLiveData.setValue(0); // Khởi tạo giá trị mặc định là 0

        db.collection("user_vouchers")
                .whereEqualTo("userId", userId)
                .whereEqualTo("used", false) // Chỉ đếm voucher chưa sử dụng
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int voucherCount = queryDocumentSnapshots.size();
                    voucherCountLiveData.setValue(voucherCount);
                    Log.d(TAG, "Voucher count loaded: " + voucherCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load voucher count", e);
                    voucherCountLiveData.setValue(0);
                });

        return voucherCountLiveData;
    }

    public void refreshUserData() {
        loadUserData(); // Tải lại dữ liệu từ Firestore
        Log.d(TAG, "Refreshing user data.");
    }
}
