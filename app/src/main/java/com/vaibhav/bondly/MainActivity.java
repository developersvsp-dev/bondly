package com.vaibhav.bondly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigation;
    private FirebaseAuth mAuth;
    private View splashView;  // Show while checking auth
    private boolean isNavigationSetup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        splashView = findViewById(R.id.splash_view);  // Add ProgressBar in layout

        // 🔥 SHOW SPLASH WHILE CHECKING
        showSplash();

        // 🔥 BULLETPROOF: Check IMMEDIATELY + AuthStateListener
        checkAuthImmediately();
        setupAuthStateListener();
    }

    private void checkAuthImmediately() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = mAuth.getCurrentUser();
            Log.d(TAG, "🔥 IMMEDIATE CHECK: " + (user != null ? user.getUid() : "NULL"));

            if (user != null) {
                hideSplash();
                setupBottomNavigation();
            }
        }, 500);  // 0.5s delay for Firebase sync
    }

    private void setupAuthStateListener() {
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            Log.d(TAG, "🔥 AuthStateListener: " + (user != null ? user.getUid() : "NULL"));

            if (user != null && !isNavigationSetup) {
                hideSplash();
                setupBottomNavigation();
            }
        });
    }

    private void setupBottomNavigation() {
        if (isNavigationSetup) return;

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

        bottomNavigation.setSelectedItemId(R.id.nav_feed);
        loadFragment(new FeedFragment());
        isNavigationSetup = true;
        Log.d(TAG, "✅ FEED + BOTTOM NAV LOADED!");
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void showSplash() {
        if (splashView != null) splashView.setVisibility(View.VISIBLE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.GONE);
    }

    private void hideSplash() {
        if (splashView != null) splashView.setVisibility(View.GONE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.VISIBLE);
    }

    private void goToRegisterActivity() {
        hideSplash();
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuth != null) {
            mAuth.removeAuthStateListener(firebaseAuth -> {});
        }
    }
}
