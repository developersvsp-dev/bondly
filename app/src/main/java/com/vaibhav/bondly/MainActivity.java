package com.vaibhav.bondly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigation;
    private FirebaseAuth mAuth;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isAuthChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (!isAuthChecked) {
            checkAuthState();
            isAuthChecked = true;
        }
    }

    private void checkAuthState() {
        handler.postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "User logged in: " + currentUser.getUid());
                setupBottomNavigation();
            } else {
                Log.d(TAG, "No user found, redirecting to login");
                goToLoginActivity();
            }
        }, 1000);
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_feed) fragment = new FeedFragment();
            else if (id == R.id.nav_inbox) fragment = new InboxFragment();
            else if (id == R.id.nav_profile) fragment = new ProfileFragment();
            else if (id == R.id.nav_settings) fragment = new SettingsFragment();

            if (fragment != null) loadFragment(fragment);
            return true;
        });

        if (getSupportFragmentManager().getFragments().isEmpty()) {
            bottomNavigation.setSelectedItemId(R.id.nav_feed);
            loadFragment(new FeedFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
