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
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigation;
    private FirebaseAuth mAuth;
    private View splashView;
    private boolean isNavigationSetup = false;

    // 🔥 HEADER CONTROL
    private AppBarLayout appbarHeader;
    private Toolbar toolbar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        splashView = findViewById(R.id.splash_view);

        // 🔥 HIDE BOTTOM NAV IMMEDIATELY - NO FLASH
        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.GONE);
        }

        showSplash(); // Shows splash_view

        // Your existing 1-sec auth check
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = mAuth.getCurrentUser();

            if (user != null) {
                Log.d(TAG, "✅ User already logged in: " + user.getUid());
                hideSplash();
                setupBottomNavigation(); // This will SHOW bottom nav
            } else {
                Log.d(TAG, "❌ No user - redirect to Register");
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 1000);
    }



    private void checkAuthImmediately() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = mAuth.getCurrentUser();
            Log.d(TAG, "🔥 CURRENT USER ID: " + (user != null ? user.getUid() : "NULL"));

            if (user != null) {
                Log.d(TAG, "✅ User exists: " + user.getUid());
                hideSplash();
                setupBottomNavigation();
            } else {
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

        // 🔥 INITIALIZE HEADER VIEWS
        appbarHeader = findViewById(R.id.appbar_header);
        toolbar = findViewById(R.id.toolbar);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_feed) {
                fragment = new FeedFragment();
                showHeader("Bondly");
            }
            else if (id == R.id.nav_inbox) {
                fragment = new InboxFragment();
                hideHeader();
            }
            else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
                hideHeader();
            }
            else if (id == R.id.nav_settings) {
                fragment = new SettingsFragment();
                hideHeader();
            }

            if (fragment != null) loadFragment(fragment);
            return true;
        });

        // 🔥 START WITH FEED + HEADER
        bottomNavigation.setSelectedItemId(R.id.nav_feed);
        loadFragment(new FeedFragment());
        showHeader("Bondly");
        isNavigationSetup = true;
        Log.d(TAG, "✅ APP READY on FEED - User ID: " + mAuth.getCurrentUser().getUid());
        registerFCMToken();
    }

    private void registerFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                String uid = mAuth.getCurrentUser().getUid();
                Log.d(TAG, "✅ FCM Token saved: " + token.substring(0, 20) + "...");

                Map<String, Object> tokenData = new HashMap<>();
                tokenData.put("fcmToken", token);

                FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void showHeader(String title) {
        if (appbarHeader != null) {
            appbarHeader.setVisibility(View.VISIBLE);
        }
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(title);
        }
    }

    private void hideHeader() {
        if (appbarHeader != null) {
            appbarHeader.setVisibility(View.GONE);
        }
    }

    private void showSplash() {
        if (splashView != null) splashView.setVisibility(View.VISIBLE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.GONE);
    }

    private void hideSplash() {
        if (splashView != null) splashView.setVisibility(View.GONE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.VISIBLE);
    }

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

    @Override
    protected void onStart() {
        super.onStart();
        updateUserStatus(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUserStatus(false);
    }

    private void updateUserStatus(boolean isOnline) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> status = new HashMap<>();
        status.put("isOnline", isOnline);
        status.put("lastSeen", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(status, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update status", e));
    }
}