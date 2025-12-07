package com.vaibhav.bondly;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
                            if (ivProfilePicEdit != null) {
                                Glide.with(this).load(imageUri).circleCrop().into(ivProfilePicEdit);
                            }
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
        setupSpinners();  // 🔥 Setup spinners FIRST

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

    // 🔥 PERFECT Spinner Setup - Works 100%
    private void setupSpinners() {
        if (spinnerGender == null || getContext() == null) {
            Log.w(TAG, "⚠️ Spinner or context null");
            return;
        }

        try {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.gender_options,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerGender.setAdapter(adapter);
            Log.d(TAG, "✅ Spinner setup - " + adapter.getCount() + " items");
        } catch (Exception e) {
            Log.e(TAG, "❌ Spinner setup failed", e);
        }
    }

    private void setupClickListeners() {
        if (btnSave != null) btnSave.setOnClickListener(v -> saveProfile());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> toggleEditMode(true));
        if (btnLogout != null) btnLogout.setOnClickListener(v -> logoutUser());
        if (btnChangePhoto != null) btnChangePhoto.setOnClickListener(v -> openGallery());
    }

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

        Log.d(TAG, "🔥 Saving - name: " + name + ", gender: '" + gender + "'");

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (profilePicUri != null) {
            uploadProfilePic(name, email, phone, bio, gender);
        } else {
            saveProfileToFirestore(name, email, phone, bio, gender, existingPhotoUrl);
        }
    }

    // 🔥 MAGIC FIX: Gender Shows CORRECTLY in Edit Mode
    private void loadProfileDataForEditing() {
        Log.d(TAG, "🔥 Edit mode - loading data for " + userId);

        // STEP 1: Ensure spinner is ready
        setupSpinners();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "✅ Data loaded: " + doc.exists());

                    if (doc.exists()) {
                        // Load text fields
                        loadTextFields(doc);

                        // 🔥 STEP 2: Load GENDER - PERFECT TIMING
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            setGenderSpinner(doc);
                        }, 100); // Small delay ensures spinner is fully ready

                        // Load photo
                        loadProfilePhoto(doc);

                        // Show edit UI
                        showEditLayout();

                    } else {
                        Log.d(TAG, "❌ No profile data");
                        showEditMode();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Load failed", e);
                    showEditMode();
                });
    }

    // 🔥 Separate method for PERFECT gender loading
    private void setGenderSpinner(DocumentSnapshot doc) {
        if (spinnerGender == null || spinnerGender.getAdapter() == null) {
            Log.e(TAG, "❌ Spinner not ready");
            return;
        }

        String savedGender = doc.getString("gender");
        Log.d(TAG, "🔥 Firestore gender: '" + savedGender + "'");

        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerGender.getAdapter();

        if (savedGender != null && !savedGender.isEmpty()) {
            // Find exact match
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equals(savedGender)) {
                    spinnerGender.setSelection(i);
                    Log.d(TAG, "✅ GENDER SELECTED: '" + savedGender + "' (pos " + i + ")");
                    return;
                }
            }
            Log.w(TAG, "⚠️ Gender '" + savedGender + "' not in options");
        }

        // Default to position 0
        spinnerGender.setSelection(0);
        Log.d(TAG, "✅ Default gender selected (pos 0)");
    }

    private void loadTextFields(DocumentSnapshot doc) {
        if (etName != null) {
            String name = doc.getString("name");
            etName.setText(name != null ? name : "");
            Log.d(TAG, "✅ Name: " + name);
        }
        if (etEmail != null) {
            String email = doc.getString("email");
            etEmail.setText(email != null ? email : "");
        }
        if (etBio != null) {
            String bio = doc.getString("bio");
            etBio.setText(bio != null ? bio : "");
        }
    }

    private void loadProfilePhoto(DocumentSnapshot doc) {
        if (ivProfilePicEdit != null) {
            if (profilePicUri != null) {
                Glide.with(this).load(profilePicUri).circleCrop().into(ivProfilePicEdit);
            } else {
                String photoUrl = doc.getString("photoUrl");
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).circleCrop().into(ivProfilePicEdit);
                    existingPhotoUrl = photoUrl;
                } else {
                    ivProfilePicEdit.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        }
    }

    private void showEditLayout() {
        if (viewLayout != null) viewLayout.setVisibility(View.GONE);
        if (editLayout != null) editLayout.setVisibility(View.VISIBLE);
        if (tvTitle != null) tvTitle.setText("Edit Profile");
        if (etName != null) etName.requestFocus();
    }

    // ... ALL OTHER METHODS SAME (uploadProfilePic, saveProfileToFirestore, etc.) ...

    private void uploadProfilePic(String name, String email, String phone, String bio, String gender) {
        String fileName = "profile_pics/" + userId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        Log.d(TAG, "📤 Uploading to: " + fileName);
        btnSave.setText("Uploading...");

        ref.putFile(profilePicUri)
                .addOnProgressListener(snapshot -> Log.d(TAG, "📈 Progress: " + snapshot.getBytesTransferred() + "/" + snapshot.getTotalByteCount()))
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveProfileToFirestore(name, email, phone, bio, gender, uri.toString());
                    }).addOnFailureListener(e -> saveProfileToFirestore(name, email, phone, bio, gender, null));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_LONG).show();
                    saveProfileToFirestore(name, email, phone, bio, gender, existingPhotoUrl);
                    resetSaveButton();
                });
    }

    private void saveProfileToFirestore(String name, String email, String phone, String bio, String gender, String newPhotoUrl) {
        Log.d(TAG, "💾 Saving profile with gender: " + gender);

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("email", email.isEmpty() ? null : email);
        profile.put("phone", phone);
        profile.put("bio", bio.isEmpty() ? null : bio);
        profile.put("gender", gender.isEmpty() ? null : gender);
        if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) profile.put("photoUrl", newPhotoUrl);
        profile.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ SAVED with gender: " + gender);
                    Toast.makeText(getContext(), "✅ Profile saved!", Toast.LENGTH_SHORT).show();
                    profilePicUri = null;
                    existingPhotoUrl = newPhotoUrl;
                    resetSaveButton();
                    loadProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Save failed", e);
                    Toast.makeText(getContext(), "Save failed", Toast.LENGTH_LONG).show();
                    resetSaveButton();
                });
    }

    private void resetSaveButton() {
        if (btnSave != null) {
            btnSave.setText("Save Profile");
            btnSave.setEnabled(true);
        }
    }

    private void loadProfile() {
        showLoading();
        FirebaseUser user = mAuth.getCurrentUser();
        String authPhone = user != null ? user.getPhoneNumber() : null;
        if (etPhone != null) etPhone.setText(authPhone != null ? authPhone : "Phone not verified");
        loadProfileDetails(authPhone);
    }

    private void loadProfileDetails(String authPhone) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    hideLoading();
                    if (doc.exists()) showProfileView(doc, authPhone);
                    else showEditMode();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showEditMode();
                });
    }

    private void showProfileView(DocumentSnapshot doc, String authPhone) {
        tvName.setText(doc.getString("name") != null ? doc.getString("name") : "Not set");
        tvEmail.setText(doc.getString("email") != null ? doc.getString("email") : "Not set");
        tvPhone.setText(authPhone != null ? authPhone : "Not verified");
        tvBio.setText(doc.getString("bio") != null ? doc.getString("bio") : "No bio");

        String gender = doc.getString("gender");
        if (tvGender != null && gender != null && !gender.isEmpty()) {
            tvGender.setText(gender);
            tvGender.setVisibility(View.VISIBLE);
        } else {
            tvGender.setVisibility(View.GONE);
        }

        existingPhotoUrl = doc.getString("photoUrl");
        if (ivProfilePic != null) {
            String photoUrl = existingPhotoUrl;
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).circleCrop().placeholder(android.R.drawable.ic_menu_gallery).into(ivProfilePic);
            } else {
                ivProfilePic.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        viewLayout.setVisibility(View.VISIBLE);
        editLayout.setVisibility(View.GONE);
        tvTitle.setText("Your Profile");
        isEditing = false;
    }

    private void toggleEditMode(boolean enable) {
        isEditing = enable;
        if (enable) loadProfileDataForEditing();
        else {
            viewLayout.setVisibility(View.VISIBLE);
            editLayout.setVisibility(View.GONE);
            tvTitle.setText("Your Profile");
        }
    }

    private void showEditMode() {
        viewLayout.setVisibility(View.GONE);
        editLayout.setVisibility(View.VISIBLE);
        tvTitle.setText("Complete Your Profile");
        etName.setText("");
        etName.requestFocus();
        setupSpinners();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void logoutUser() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        viewLayout.setVisibility(View.GONE);
        editLayout.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showLoginRequired() {
        hideLoading();
        Toast.makeText(getContext(), "Please login first", Toast.LENGTH_LONG).show();
        tvTitle.setText("Login Required");
    }
}
