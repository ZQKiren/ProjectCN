package com.example.myapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.R;
import com.example.myapp.viewmodel.ProfileViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class EditProfileFragment extends Fragment {

    private ProfileViewModel profileViewModel;
    private TextInputEditText fullNameEditText, phoneNumberEditText;
    private MaterialTextView emailTextView;
    private AutoCompleteTextView genderAutoComplete;
    private ShapeableImageView avatarImageView;
    private FloatingActionButton cameraButton;
    private ProgressBar loadingProgressBar;
    private MaterialButton saveButton;
    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        initializeFirebase();
        initializeViews(view);
        setupToolbar(view);
        setupViewModel();
        setupGenderDropdown();
        setupClickListeners();
        return view;
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initializeViews(View view) {
        fullNameEditText = view.findViewById(R.id.profile_full_name);
        phoneNumberEditText = view.findViewById(R.id.profile_phone);
        emailTextView = view.findViewById(R.id.profile_email);
        genderAutoComplete = view.findViewById(R.id.profile_gender_spinner);
        avatarImageView = view.findViewById(R.id.profile_avatar);
        cameraButton = view.findViewById(R.id.camera_icon);
        loadingProgressBar = view.findViewById(R.id.profile_loading);
        saveButton = view.findViewById(R.id.btn_save);
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupViewModel() {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        loadingProgressBar.setVisibility(View.VISIBLE);

        profileViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (user != null) {
                fullNameEditText.setText(user.getFullName());
                phoneNumberEditText.setText(user.getPhoneNumber());
                emailTextView.setText(user.getEmail());

                if (user.getGender() != null) {
                    genderAutoComplete.setText(user.getGender(), false);
                }

                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Picasso.get()
                            .load(user.getAvatarUrl())
                            .placeholder(R.drawable.ic_placeholder_image)
                            .error(R.drawable.ic_placeholder_image)
                            .into(avatarImageView);
                }
            } else {
                Toast.makeText(getContext(), "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupGenderDropdown() {
        // Create custom adapter with the custom dropdown layout for dark mode support
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                requireContext(),
                R.layout.item_dropdown,
                getResources().getStringArray(R.array.gender_options)
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // Ensure text is visible in both light and dark modes
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(
                            getResources().getColor(android.R.color.system_neutral1_900, requireContext().getTheme())
                    );
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                // Ensure dropdown text is visible in both light and dark modes
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(
                            getResources().getColor(android.R.color.system_neutral1_900, requireContext().getTheme())
                    );
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(R.layout.item_dropdown);
        genderAutoComplete.setAdapter(adapter);
    }

    private void setupClickListeners() {
        View.OnClickListener chooseImageListener = v -> openImageChooser();
        avatarImageView.setOnClickListener(chooseImageListener);
        cameraButton.setOnClickListener(chooseImageListener);
        saveButton.setOnClickListener(v -> saveUserInfo());
    }


    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Picasso.get()
                                .load(selectedImageUri)
                                .placeholder(R.drawable.ic_placeholder_image)
                                .error(R.drawable.ic_placeholder_image)
                                .into(avatarImageView);
                    }
                }
            });

    private void saveUserInfo() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        String fullName = Objects.requireNonNull(fullNameEditText.getText()).toString().trim();
        String phoneNumber = Objects.requireNonNull(phoneNumberEditText.getText()).toString().trim();
        String gender = genderAutoComplete.getText().toString();

        if (selectedImageUri != null) {
            uploadAvatarToStorage(selectedImageUri, downloadUrl -> {
                updateUserProfile(fullName, phoneNumber, gender, downloadUrl);
                saveButton.setEnabled(true);
            });
        } else {
            String avatarUrl = Objects.requireNonNull(profileViewModel.getUserData().getValue()).getAvatarUrl();
            updateUserProfile(fullName, phoneNumber, gender, avatarUrl);
            saveButton.setEnabled(true);
        }
    }

    private void uploadAvatarToStorage(Uri avatarUri, OnAvatarUploadListener listener) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && avatarUri != null) {
            String userId = firebaseUser.getUid();
            StorageReference avatarRef = storage.getReference().child("avatars/" + userId + ".jpg");

            avatarRef.putFile(avatarUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw Objects.requireNonNull(task.getException());
                        }
                        return avatarRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> listener.onUploadSuccess(downloadUri.toString()))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Không thể tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        loadingProgressBar.setVisibility(View.GONE); // Ẩn loading nếu upload thất bại
                        saveButton.setEnabled(true); // Re-enable the save button
                    });
        }
    }

    private void updateUserProfile(String fullName, String phoneNumber, String gender, String avatarUrl) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid())
                    .update("fullName", fullName,
                            "phoneNumber", phoneNumber,
                            "gender", gender,
                            "avatarUrl", avatarUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Thông tin đã được cập nhật", Toast.LENGTH_SHORT).show();
                        loadingProgressBar.setVisibility(View.GONE);
                        navigateToProfileFragment();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Không thể cập nhật thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        loadingProgressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                    });
        }
    }

    private void navigateToProfileFragment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.content_frame, new ProfileFragment())
                .addToBackStack(null)
                .commit();
    }

    // Listener for avatar upload success
    public interface OnAvatarUploadListener {
        void onUploadSuccess(String downloadUrl);
    }
}