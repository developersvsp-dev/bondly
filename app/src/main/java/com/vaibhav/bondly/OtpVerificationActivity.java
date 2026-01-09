package com.vaibhav.bondly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class OtpVerificationActivity extends AppCompatActivity {
    private static final String TAG = "OtpVerification";
    private EditText etOtp;
    private Button btnVerifyOtp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationId;
    private String userName, phoneNumber, userGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔥 GET DATA FROM INTENT (may be null for existing users)
        verificationId = getIntent().getStringExtra("verificationId");
        userName = getIntent().getStringExtra("userName");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        userGender = getIntent().getStringExtra("userGender");

        Log.d(TAG, "🔥 Intent data - Name: " + userName + ", Phone: " + phoneNumber + ", Gender: " + userGender);

        etOtp = findViewById(R.id.etOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() == 6) {
                // 🔥 LOADING STATE - DISABLE + SHOW PROCESSING
                btnVerifyOtp.setEnabled(false);
                btnVerifyOtp.setText("Verifying...");
                verifyOTP(otp);
            } else {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOTP(String otp) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    // 🔥 RESET BUTTON STATE
                    btnVerifyOtp.setEnabled(true);
                    btnVerifyOtp.setText("Verify OTP");
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "✅ OTP Verified for user: " + user.getUid());
                            handleUserProfile(user.getUid());
                        }
                    } else {
                        Log.e(TAG, "❌ OTP failed", task.getException());
                        Toast.makeText(this, "Invalid OTP", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // 🔥 SMARTEST APPROACH: Check → Update only if needed
    private void handleUserProfile(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 🔥 USER EXISTS → UPDATE ONLY MISSING FIELDS
                        Log.d(TAG, "✅ User exists, checking data...");
                        Map<String, Object> existingData = documentSnapshot.getData();

                        boolean needsUpdate = false;
                        Map<String, Object> updates = new HashMap<>();

                        // 🔥 ONLY UPDATE IF INTENT HAS DATA
                        if (userName != null && !userName.trim().isEmpty()) {
                            updates.put("name", userName);
                            needsUpdate = true;
                        }
                        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                            updates.put("phone", phoneNumber);
                            needsUpdate = true;
                        }
                        if (userGender != null && !userGender.trim().isEmpty()) {
                            updates.put("gender", userGender);
                            needsUpdate = true;
                        }

                        if (needsUpdate) {
                            // 🔥 USE .update() → PRESERVES bio, photoUrl, etc.
                            db.collection("users").document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Profile updated");
                                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                        goToMainActivity();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Update failed", e);
                                        goToMainActivity(); // Still proceed
                                    });
                        } else {
                            // 🔥 NO NEW DATA → Just welcome back
                            Log.d(TAG, "✅ No new data needed");
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        }
                    } else {
                        // 🔥 NEW USER → CREATE FULL PROFILE
                        Log.d(TAG, "🔥 New user - creating profile");
                        createNewUserProfile(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Profile check failed", e);
                    // 🔥 SAFE FALLBACK: Create new if check fails
                    createNewUserProfile(userId);
                });
    }

    // 🔥 NEW USER ONLY
    private void createNewUserProfile(String userId) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", userName != null ? userName : "User");
        profile.put("phone", phoneNumber != null ? phoneNumber : "");
        profile.put("gender", userGender != null ? userGender : "");
        profile.put("photoUrl", "");
        profile.put("bio", "");
        profile.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ New profile created");
                    Toast.makeText(this, "✅ Welcome to Bondly!", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Profile creation failed", e);
                    Toast.makeText(this, "Profile save failed", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                });
    }

    // 🔥 CLEAN NAVIGATION
    private void goToMainActivity() {
        Intent intent = new Intent(OtpVerificationActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
