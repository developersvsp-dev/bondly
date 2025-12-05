package com.vaibhav.bondly;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_splash);

        setContentView(R.layout.activity_splash);
        // Wait 2 seconds → open MainActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish(); // prevent going back to splash
        }, 1000);
    }
}
