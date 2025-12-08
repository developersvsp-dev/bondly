package com.vaibhav.bondly;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {
    private static final String TAG = "FeedFragment";
    private RecyclerView rvFeed;
    private ProgressBar progressBar;
    private TextView tvWelcome;
    private ProfileAdapter adapter;
    private List<Profile> profiles;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        // Initialize views with null checks
        rvFeed = view.findViewById(R.id.rvFeed);
        progressBar = view.findViewById(R.id.progressBar);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        Button premiumBtn = view.findViewById(R.id.btnGoPremium);

        // Setup Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        profiles = new ArrayList<>();
        adapter = new ProfileAdapter(profiles, profile -> {
            if (getContext() != null && profile != null) {
                Toast.makeText(getContext(), "View " + profile.getName() + "'s profile", Toast.LENGTH_SHORT).show();
            }
        });
        if (rvFeed != null) {
            rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
            rvFeed.setAdapter(adapter);
        }

        // Premium button
        if (premiumBtn != null) {
            premiumBtn.setOnClickListener(v -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), PremiumActivity.class));
                }
            });
        }

        // Load profiles after fragment is ready
        new Handler().postDelayed(this::loadProfiles, 200);

        return view;
    }

    // 🔥 UPDATED: EXCLUDES CURRENT USER FROM FEED
    // 🔥 UPDATE loadProfiles() method ONLY - replace existing one:
    private void loadProfiles() {
        if (getContext() == null) return;

        showLoading(true);
        tvWelcome.setText("Loading profiles...");

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        Log.d(TAG, "🔥 Current user ID: " + currentUserId);

        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    profiles.clear();

                    for (var doc : querySnapshot) {
                        String uid = doc.getId();

                        // 🔥 SKIP CURRENT USER
                        if (uid.equals(currentUserId)) {
                            Log.d(TAG, "⏭️ Skipping current user: " + uid);
                            continue;
                        }

                        // 🔥 LOAD ALL FIELDS EXCEPT PHONE
                        String name = doc.getString("name");
                        String photoUrl = doc.getString("photoUrl");
                        String gender = doc.getString("gender");
                        String bio = doc.getString("bio");

                        Profile profile = new Profile();
                        profile.setUid(uid);
                        profile.setName(name != null ? name : "User " + uid.substring(0, 6));
                        // 🔥 PHONE REMOVED - NO profile.setPhone()
                        profile.setPhotoUrl(photoUrl);
                        profile.setGender(gender);
                        profile.setBio(bio);
                        profiles.add(profile);

                        Log.d(TAG, "✅ Loaded: " + profile.getName() + " (" + gender + ") | Bio: " + bio);
                    }

                    adapter.notifyDataSetChanged();
                    showLoading(false);

                    if (profiles.isEmpty()) {
                        tvWelcome.setText("No other users yet. Invite friends!");
                    } else {
                        tvWelcome.setText("Found " + profiles.size() + " users nearby");
                    }

                    updateWelcomeMessage();
                    Log.d(TAG, "✅ OTHER profiles loaded: " + profiles.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore failed", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Cannot load profiles", Toast.LENGTH_SHORT).show();
                    }
                    showLoading(false);
                    tvWelcome.setText("Failed to load profiles");
                });
    }



    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvFeed != null) rvFeed.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // 🔥 UPDATED: Welcome message with gender support
    private void updateWelcomeMessage() {
        if (mAuth.getCurrentUser() == null || tvWelcome == null || getContext() == null) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (getContext() == null || tvWelcome == null) return;
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String gender = doc.getString("gender");
                        if (name != null) {
                            String welcome = "Welcome back, " + name + "!";
                            if (gender != null && !gender.isEmpty()) {
                                welcome += " (" + gender + ")";
                            }
                            tvWelcome.setText(welcome);
                            Log.d(TAG, "✅ Welcome: " + welcome);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Welcome message failed", e);
                });
    }
}
