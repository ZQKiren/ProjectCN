package com.example.myapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.myapp.data.User;
import com.example.myapp.data.User.Role;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpViewModel extends AndroidViewModel {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    public MutableLiveData<String> signUpStatus = new MutableLiveData<>();

    public SignUpViewModel(@NonNull Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void signUpUser(String fullName, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid(); // Lấy UID từ Firebase Authentication
                            // Tạo đối tượng User với các thông tin cần thiết
                            User user = new User(userId, fullName, email, "","");
                            user.setRole(Role.USER); // Thiết lập vai trò mặc định là "USER"
                            saveUserData(user); // Lưu đối tượng User vào Firestore
                        }
                    } else {
                        signUpStatus.setValue("Đăng ký thất bại: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void saveUserData(User user) {
        db.collection("users").document(user.getId()) // Dùng ID tài liệu từ Firebase
                .set(user)
                .addOnSuccessListener(aVoid -> signUpStatus.setValue("Đăng ký thành công!"))
                .addOnFailureListener(e -> signUpStatus.setValue("Lỗi khi lưu dữ liệu: " + e.getMessage()));
    }
}
