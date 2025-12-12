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
import com.google.firebase.firestore.FirebaseFirestore;  // 🔥 ADD
import java.util.HashMap;  // 🔥 ADD
import java.util.Map;      // 🔥 ADD

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigation;
    private FirebaseAuth mAuth;
    private View splashView;
    private boolean isNavigationSetup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        splashView = findViewById(R.id.splash_view);

        showSplash();
        checkAuthImmediately();
        setupAuthStateListener();
    }

    private void checkAuthImmediately() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = mAuth.getCurrentUser();
            Log.d(TAG, "🔥 CURRENT USER ID: " + (user != null ? user.getUid() : "NULL"));

            if (user != null) {
                // ✅ AUTO LOGIN IF NEEDED
                if (user.isAnonymous()) {
                    Log.d(TAG, "✅ Anonymous user OK: " + user.getUid());
                }
                hideSplash();
                setupBottomNavigation();
            } else {
                // ✅ AUTO ANONYMOUS LOGIN (Production ready)
                mAuth.signInAnonymously()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "✅ AUTO LOGIN SUCCESS: " + mAuth.getCurrentUser().getUid());
                                hideSplash();
                                setupBottomNavigation();
                            } else {
                                Log.e(TAG, "❌ Auto login failed");
                            }
                        });
            }
        }, 500);
    }

    private void setupAuthStateListener() {
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && !isNavigationSetup) {
                Log.d(TAG, "🔥 Auth ready: " + user.getUid());
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

        // 🔥 CHANGE: Start with FEED instead of Settings
        bottomNavigation.setSelectedItemId(R.id.nav_feed);
        loadFragment(new FeedFragment());
        isNavigationSetup = true;
        Log.d(TAG, "✅ APP READY on FEED - User ID: " + mAuth.getCurrentUser().getUid());
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

    // 🔥 FOR CHATFRAGMENT & INBOX - HIDE/SHOW BOTTOM NAV
    public void hideBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.GONE);
        }
    }

    public void showBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuth != null) {
            mAuth.removeAuthStateListener(firebaseAuth -> {});
        }
    }
    // 🔥 ADD THESE 2 METHODS (app online/offline)
    @Override
    protected void onStart() {
        super.onStart();
        updateUserStatus(true);  // Set ONLINE
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUserStatus(false); // Set OFFLINE
    }

    // 🔥 ADD THIS METHOD
    private void updateUserStatus(boolean isOnline) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> status = new HashMap<>();
        status.put("isOnline", isOnline);
        status.put("lastSeen", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(status, com.google.firebase.firestore.SetOptions.merge())  // MERGE (don't overwrite other fields)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update status", e));
    }
}
