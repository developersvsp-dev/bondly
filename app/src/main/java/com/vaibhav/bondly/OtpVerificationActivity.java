package com.vaibhav.bondly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
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
import java.util.HashMap;
import java.util.Map;

public class OtpVerificationActivity extends AppCompatActivity {
    private EditText etOtp;
    private Button btnVerifyOtp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationId;
    private String userName, phoneNumber, userGender; // 🔥 ADDED GENDER

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔥 GET ALL DATA FROM RegisterActivity (including GENDER)
        verificationId = getIntent().getStringExtra("verificationId");
        userName = getIntent().getStringExtra("userName");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        userGender = getIntent().getStringExtra("userGender"); // 🔥 NEW LINE

        etOtp = findViewById(R.id.etOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() == 6) {
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
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 🔥 CREATE COMPLETE PROFILE WITH GENDER
                            createUserProfile(user.getUid(), userName, phoneNumber, userGender);
                        }
                    } else {
                        Toast.makeText(this, "Invalid OTP: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // 🔥 FIXED: Now saves GENDER too!
    private void createUserProfile(String userId, String name, String phone, String gender) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("phone", phone);
        profile.put("gender", gender);           // 🔥 ADDED GENDER
        profile.put("photoUrl", "");
        profile.put("createdAt", FieldValue.serverTimestamp());  // 🔥 FIXED: Use FieldValue

        db.collection("users").document(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Welcome, " + name + " (" + gender + ")!", Toast.LENGTH_SHORT).show();
                    // Navigate to MainActivity
                    Intent intent = new Intent(OtpVerificationActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Profile save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Still go to MainActivity
                    Intent intent = new Intent(OtpVerificationActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}
