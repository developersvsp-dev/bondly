package com.vaibhav.bondly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FeedActivity extends AppCompatActivity {
    private TextView tvWelcome;
    private RecyclerView rvFeed;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        mAuth = FirebaseAuth.getInstance();
        tvWelcome = findViewById(R.id.tvWelcome);
        rvFeed = findViewById(R.id.rvFeed);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        checkUserAuth();
        setupFeed();
        setupBottomNavigation();
    }

    private void checkUserAuth() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String welcomeText = "Welcome, " + (user.getDisplayName() != null ? user.getDisplayName() : "User") + "!";
            tvWelcome.setText(welcomeText);
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupFeed() {
        rvFeed.setLayoutManager(new LinearLayoutManager(this));
        // Add your feed adapter here
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_feed) {
                // Already on Feed screen
                return true;
            } else if (itemId == R.id.nav_inbox) {
                Toast.makeText(this, "Inbox clicked", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(FeedActivity.this, InboxActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(FeedActivity.this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(FeedActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

}
