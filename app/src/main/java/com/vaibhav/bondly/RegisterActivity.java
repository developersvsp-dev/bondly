package com.vaibhav.bondly;

import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhoneNumber;
    private Spinner spinnerGender;  // 🔥 NEW
    private Button btnSendOTP, btnLogin;
    private TextView tvLoginPrompt;
    private FirebaseAuth mAuth;

    // 🔥 STORE USER DATA for OTP screen
    private String userName, phoneNumber, userGender;  // 🔥 Added gender

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spinnerGender = findViewById(R.id.spinnerGender);  // 🔥 NEW
        btnSendOTP = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        tvLoginPrompt = findViewById(R.id.tvLoginPrompt);

        // 🔥 SETUP GENDER SPINNER
        setupGenderSpinner();

        btnSendOTP.setOnClickListener(v -> {
            userName = etName.getText().toString().trim();
            phoneNumber = etPhoneNumber.getText().toString().trim();
            userGender = spinnerGender.getSelectedItem() != null ?
                    spinnerGender.getSelectedItem().toString() : "";

            // 🔥 VALIDATE GENDER TOO
            if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(phoneNumber) || userGender.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all fields including gender", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userGender.equals("Select Gender")) {
                Toast.makeText(RegisterActivity.this, "Please select your gender", Toast.LENGTH_SHORT).show();
                return;
            }
// 🔥 LOADING STATE - DISABLE + SHOW PROGRESS
            btnSendOTP.setEnabled(false);
            btnSendOTP.setText("Sending OTP...");
            // Start phone number verification
            startPhoneNumberVerification(phoneNumber);
        });

        // Login button navigation
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        tvLoginPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void setupGenderSpinner() {
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
        spinnerGender.setSelection(0); // Default "Select Gender"
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                signInWithPhoneAuthCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(RegisterActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                super.onCodeSent(verificationId, token);

                                // 🔥 RESET BUTTON AFTER SUCCESS
                                btnSendOTP.setEnabled(true);
                                btnSendOTP.setText("Send OTP");
                                // 🚀 PASS ALL DATA TO OTP SCREEN
                                Intent intent = new Intent(RegisterActivity.this, OtpVerificationActivity.class);
                                intent.putExtra("verificationId", verificationId);
                                intent.putExtra("userName", userName);
                                intent.putExtra("phoneNumber", phoneNumber);
                                intent.putExtra("userGender", userGender);  // 🔥 NEW
                                startActivity(intent);
                                Toast.makeText(RegisterActivity.this, "OTP sent! Check your SMS.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
