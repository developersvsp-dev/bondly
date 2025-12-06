package com.vaibhav.bondly;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // 🔥 ALL VIEWS
    private TextInputEditText etName, etEmail, etPhone, etBio;
    private Spinner spinnerGender;
    private Button btnSave, btnEdit, btnLogout, btnChangePhoto;
    private LinearLayout editLayout, viewLayout;
    private TextView tvName, tvEmail, tvPhone, tvBio, tvTitle, tvGender;
    private ProgressBar progressBar;
    private ShapeableImageView ivProfilePic, ivProfilePicEdit;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private boolean isEditing = false;
    private String userId;
    private Uri profilePicUri;
    private String existingPhotoUrl = null;

    private ActivityResultLauncher<Intent> galleryLauncher;

    public ProfileFragment() {
        Log.d(TAG, "🔥 Constructor called");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🔥 onCreate called");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "🔥 Gallery result: code=" + result.getResultCode());
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            profilePicUri = imageUri;
                            Glide.with(this).load(imageUri).circleCrop().into(ivProfilePicEdit);
                            Toast.makeText(getContext(), "Image selected!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupClickListeners();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadProfile();
        } else {
            showLoginRequired();
        }
        return view;
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etBio = view.findViewById(R.id.etBio);
        spinnerGender = view.findViewById(R.id.spinnerGender);
        btnSave = view.findViewById(R.id.btnSave);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);

        editLayout = view.findViewById(R.id.editLayout);
        viewLayout = view.findViewById(R.id.viewLayout);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvBio = view.findViewById(R.id.tvBio);
        tvGender = view.findViewById(R.id.tvGender);
        tvTitle = view.findViewById(R.id.tvTitle);
        progressBar = view.findViewById(R.id.progressBar);
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        ivProfilePicEdit = view.findViewById(R.id.ivProfilePicEdit);

        if (etPhone != null) {
            etPhone.setEnabled(false);
            etPhone.setFocusable(false);
            etPhone.setKeyListener(null);
            etPhone.setBackground(null);
        }
    }

    private void setupClickListeners() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfile());
        }
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> toggleEditMode(true));
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logoutUser());
        }
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> openGallery());
        }
    }

    // 🔥 FIXED: PERFECT SAVE LOGIC - PRESERVES PHOTO URL
    private void saveProfile() {
        String name = etName != null ? etName.getText().toString().trim() : "";
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";
        String bio = etBio != null ? etBio.getText().toString().trim() : "";

        String gender = "";
        if (spinnerGender != null && spinnerGender.getSelectedItem() != null) {
            gender = spinnerGender.getSelectedItem().toString();
            if (gender.equals("Select Gender")) gender = "";
        }

        Log.d(TAG, "🔥 Saving - name: " + name + ", gender: " + gender);

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // 🔥 PERFECT PHOTO LOGIC
        if (profilePicUri != null) {
            // New photo selected - upload
            uploadProfilePic(name, email, phone, bio, gender);
        } else {
            // No new photo - preserve existing OR save without photo
            saveProfileToFirestore(name, email, phone, bio, gender, existingPhotoUrl);
        }
    }

    private void uploadProfilePic(String name, String email, String phone, String bio, String gender) {
        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        Log.d(TAG, "📤 Uploading to: " + fileName);
        btnSave.setText("Uploading...");

        ref.putFile(profilePicUri)
                .addOnProgressListener(snapshot -> {
                    long transferred = snapshot.getBytesTransferred();
                    long total = snapshot.getTotalByteCount();
                    Log.d(TAG, "📈 Progress: " + transferred + "/" + total);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "✅ Upload success");
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        Log.d(TAG, "✅ URL: " + uri.toString());
                        saveProfileToFirestore(name, email, phone, bio, gender, uri.toString());
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "❌ URL failed", e);
                        saveProfileToFirestore(name, email, phone, bio, gender, null);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "💥 Upload failed", e);
                    Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_LONG).show();
                    saveProfileToFirestore(name, email, phone, bio, gender, existingPhotoUrl);
                    resetSaveButton();
                });
    }

    // 🔥 FIXED: Merges with existing data - NEVER loses photoUrl
    private void saveProfileToFirestore(String name, String email, String phone, String bio, String gender, String newPhotoUrl) {
        Log.d(TAG, "💾 Saving to Firestore - newPhoto: " + newPhotoUrl);

        // 🔥 GET CURRENT DOCUMENT FIRST
        db.collection("users").document(userId)
                .collection("profile")
                .document("details")
                .get()
                .addOnSuccessListener(currentDoc -> {
                    Map<String, Object> profile = currentDoc.exists() ?
                            new HashMap<>(currentDoc.getData()) : new HashMap<>();

                    // 🔥 UPDATE ONLY SPECIFIC FIELDS - PRESERVE OTHERS
                    profile.put("name", name);
                    profile.put("email", email.isEmpty() ? null : email);
                    profile.put("phone", phone);
                    profile.put("bio", bio.isEmpty() ? null : bio);
                    profile.put("gender", gender.isEmpty() ? null : gender);

                    // 🔥 PERFECT PHOTO LOGIC
                    if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) {
                        profile.put("photoUrl", newPhotoUrl);  // New photo
                        Log.d(TAG, "🔥 New photoUrl saved: " + newPhotoUrl);
                    } // else: existing photoUrl is preserved from currentDoc

                    profile.put("updatedAt", FieldValue.serverTimestamp());

                    // 🔥 SAVE MERGED DATA
                    db.collection("users").document(userId)
                            .collection("profile")
                            .document("details")
                            .set(profile)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ FIRESTORE SUCCESS - photoUrl: " + profile.get("photoUrl"));
                                Toast.makeText(getContext(), "✅ Profile saved!", Toast.LENGTH_SHORT).show();
                                profilePicUri = null;
                                resetSaveButton();
                                loadProfile();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ FIRESTORE FAILED", e);
                                Toast.makeText(getContext(), "Save failed", Toast.LENGTH_LONG).show();
                                resetSaveButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get current data", e);
                    // Fallback: create new
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", name);
                    profile.put("email", email.isEmpty() ? null : email);
                    profile.put("phone", phone);
                    profile.put("bio", bio.isEmpty() ? null : bio);
                    profile.put("gender", gender.isEmpty() ? null : gender);
                    if (newPhotoUrl != null) profile.put("photoUrl", newPhotoUrl);
                    profile.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("users").document(userId)
                            .collection("profile")
                            .document("details")
                            .set(profile)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "✅ Profile saved!", Toast.LENGTH_SHORT).show();
                                profilePicUri = null;
                                resetSaveButton();
                                loadProfile();
                            });
                });
    }

    private void resetSaveButton() {
        if (btnSave != null) {
            btnSave.setText("Save Profile");
            btnSave.setEnabled(true);
        }
    }

    private void loadProfile() {
        Log.d(TAG, "🔥 loadProfile called");
        showLoading();
        FirebaseUser user = mAuth.getCurrentUser();
        String authPhone = user != null && user.getPhoneNumber() != null ? user.getPhoneNumber() : null;

        if (etPhone != null) {
            etPhone.setText(authPhone != null ? authPhone : "Phone not verified");
        }
        loadProfileDetails(authPhone);
    }

    private void loadProfileDetails(String authPhone) {
        Log.d(TAG, "🔥 Loading profile details");
        db.collection("users").document(userId)
                .collection("profile")
                .document("details")
                .get()
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "🔥 Profile doc exists: " + doc.exists());
                    hideLoading();
                    if (doc.exists()) {
                        showProfileView(doc, authPhone);
                    } else {
                        showEditMode();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Profile load failed", e);
                    hideLoading();
                    showEditMode();
                });
    }

    private void showProfileView(DocumentSnapshot doc, String authPhone) {
        Log.d(TAG, "🔥 Showing profile view");
        tvName.setText(doc.getString("name") != null ? doc.getString("name") : "Not set");
        tvEmail.setText(doc.getString("email") != null ? doc.getString("email") : "Not set");
        tvPhone.setText(authPhone != null ? authPhone : "Not verified");
        tvBio.setText(doc.getString("bio") != null ? doc.getString("bio") : "No bio");

        String gender = doc.getString("gender");
        if (tvGender != null) {
            if (gender != null && !gender.isEmpty()) {
                tvGender.setText(gender);
                tvGender.setVisibility(View.VISIBLE);
                Log.d(TAG, "🔥 Gender shown: " + gender);
            } else {
                tvGender.setVisibility(View.GONE);
            }
        }

        existingPhotoUrl = doc.getString("photoUrl");
        String photoUrl = existingPhotoUrl;

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().placeholder(android.R.drawable.ic_menu_gallery).into(ivProfilePic);
            Log.d(TAG, "🔥 Photo loaded: " + photoUrl);
        } else {
            ivProfilePic.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        viewLayout.setVisibility(View.VISIBLE);
        editLayout.setVisibility(View.GONE);
        tvTitle.setText("Your Profile");
        isEditing = false;
    }

    private void toggleEditMode(boolean enable) {
        isEditing = enable;
        if (enable) {
            Log.d(TAG, "🔥 Edit mode - loading existing data");
            loadProfileDataForEditing();
        } else {
            viewLayout.setVisibility(View.VISIBLE);
            editLayout.setVisibility(View.GONE);
            tvTitle.setText("Your Profile");
        }
    }

    private void loadProfileDataForEditing() {
        Log.d(TAG, "🔥 Loading data for edit mode");

        db.collection("users").document(userId)
                .collection("profile")
                .document("details")
                .get()
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "✅ Edit data loaded: " + doc.exists());
                    if (doc.exists()) {
                        if (etName != null) etName.setText(doc.getString("name") != null ? doc.getString("name") : "");
                        if (etEmail != null) etEmail.setText(doc.getString("email") != null ? doc.getString("email") : "");
                        if (etBio != null) etBio.setText(doc.getString("bio") != null ? doc.getString("bio") : "");

                        if (spinnerGender != null) {
                            String savedGender = doc.getString("gender");
                            if (savedGender != null && (savedGender.equals("Male") || savedGender.equals("Female"))) {
                                int position = savedGender.equals("Male") ? 1 : 2;
                                spinnerGender.setSelection(position, true);
                                Log.d(TAG, "✅ Gender set: " + savedGender + " (pos " + position + ")");
                            } else {
                                spinnerGender.setSelection(0);
                            }
                        }

                        if (profilePicUri != null) {
                            Glide.with(this).load(profilePicUri).circleCrop().into(ivProfilePicEdit);
                            Log.d(TAG, "✅ New photo preview");
                        } else if (existingPhotoUrl != null && !existingPhotoUrl.isEmpty() && ivProfilePicEdit != null) {
                            Glide.with(this).load(existingPhotoUrl).circleCrop().into(ivProfilePicEdit);
                            Log.d(TAG, "✅ Existing photo loaded: " + existingPhotoUrl);
                        } else {
                            ivProfilePicEdit.setImageResource(android.R.drawable.ic_menu_gallery);
                        }

                        viewLayout.setVisibility(View.GONE);
                        editLayout.setVisibility(View.VISIBLE);
                        tvTitle.setText("Edit Profile");
                        if (etName != null) etName.requestFocus();
                    } else {
                        showEditMode();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load edit data", e);
                    showEditMode();
                });
    }

    private void showEditMode() {
        viewLayout.setVisibility(View.GONE);
        editLayout.setVisibility(View.VISIBLE);
        tvTitle.setText("Complete Your Profile");
        if (etName != null) {
            etName.setText("");
            etName.requestFocus();
        }
        if (spinnerGender != null) spinnerGender.setSelection(0);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void logoutUser() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (viewLayout != null) viewLayout.setVisibility(View.GONE);
        if (editLayout != null) editLayout.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void showLoginRequired() {
        hideLoading();
        Toast.makeText(getContext(), "Please login first", Toast.LENGTH_LONG).show();
        if (tvTitle != null) tvTitle.setText("Login Required");
    }
}

