package com.vaibhav.bondly;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextInputEditText etName, etEmail, etPhone, etBio; // **TextInputEditText**
    private Button btnSave, btnEdit, btnLogout;
    private LinearLayout editLayout, viewLayout;
    private TextView tvName, tvEmail, tvPhone, tvBio, tvTitle;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isEditing = false;
    private String userId;

    public ProfileFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupClickListeners();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
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
        btnSave = view.findViewById(R.id.btnSave);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnLogout = view.findViewById(R.id.btnLogout);

        editLayout = view.findViewById(R.id.editLayout);
        viewLayout = view.findViewById(R.id.viewLayout);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvBio = view.findViewById(R.id.tvBio);
        tvTitle = view.findViewById(R.id.tvTitle);
        progressBar = view.findViewById(R.id.progressBar);

        // Phone read-only
        if (etPhone != null) {
            etPhone.setEnabled(false);
            etPhone.setFocusable(false);
            etPhone.setKeyListener(null);
            etPhone.setBackground(null);
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProfile());
        btnEdit.setOnClickListener(v -> toggleEditMode(true));
        btnLogout.setOnClickListener(v -> logoutUser());
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

    private void loadProfile() {
        showLoading();
        FirebaseUser user = mAuth.getCurrentUser();
        String authPhone = null;

        if (user != null && user.getPhoneNumber() != null) {
            authPhone = user.getPhoneNumber();
            if (etPhone != null) etPhone.setText(authPhone);
        } else {
            if (etPhone != null) etPhone.setText("Phone not verified");
        }
        loadProfileDetails(authPhone);
    }

    private void loadProfileDetails(String authPhone) {
        db.collection("users").document(userId)
                .collection("profile")
                .document("details")
                .get()
                .addOnSuccessListener(doc -> {
                    hideLoading();
                    if (doc.exists()) {
                        showProfileView(doc, authPhone);
                    } else {
                        showEditMode();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showEditMode();
                });
    }

    private void showProfileView(com.google.firebase.firestore.DocumentSnapshot doc, String authPhone) {
        tvName.setText(doc.getString("name") != null ? doc.getString("name") : "Not set");
        tvEmail.setText(doc.getString("email") != null ? doc.getString("email") : "Not set");
        tvPhone.setText(authPhone != null ? authPhone : "Not verified");
        tvBio.setText(doc.getString("bio") != null ? doc.getString("bio") : "No bio");

        viewLayout.setVisibility(View.VISIBLE);
        editLayout.setVisibility(View.GONE);
        tvTitle.setText("Your Profile");
        isEditing = false;
    }

    private void showEditMode() {
        viewLayout.setVisibility(View.GONE);
        editLayout.setVisibility(View.VISIBLE);
        tvTitle.setText("Complete Your Profile");
        if (etName != null) etName.requestFocus();
    }

    private void toggleEditMode(boolean enable) {
        isEditing = enable;
        if (enable) {
            viewLayout.setVisibility(View.GONE);
            editLayout.setVisibility(View.VISIBLE);
            tvTitle.setText("Edit Profile");
            if (etName != null) etName.requestFocus();
        } else {
            viewLayout.setVisibility(View.VISIBLE);
            editLayout.setVisibility(View.GONE);
            tvTitle.setText("Your Profile");
        }
    }

    private void saveProfile() {
        String name = etName != null ? etName.getText().toString().trim() : "";
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";
        String bio = etBio != null ? etBio.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("email", email.isEmpty() ? null : email);
        profile.put("phone", phone);
        profile.put("bio", bio.isEmpty() ? null : bio);
        profile.put("updatedAt", FieldValue.serverTimestamp());

        btnSave.setEnabled(false);
        db.collection("users").document(userId)
                .collection("profile")
                .document("details")
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile saved!", Toast.LENGTH_SHORT).show();
                    loadProfile();
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
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
