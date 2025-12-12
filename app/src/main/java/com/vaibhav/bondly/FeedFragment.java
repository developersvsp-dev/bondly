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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedFragment extends Fragment {
    private static final String TAG = "FeedFragment";
    private RecyclerView rvFeed;
    private ProgressBar progressBar;
    private TextView tvWelcome;
    private ProfileAdapter adapter;
    private List<Profile> profiles;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Map<String, Boolean> myLikes = new HashMap<>(); // 🔥 Track my likes

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

        // 🔥 Setup RecyclerView with LIKE SUPPORT
        profiles = new ArrayList<>();
        adapter = new ProfileAdapter(profiles,
                profile -> {
                    if (getContext() != null && profile != null) {
                        Toast.makeText(getContext(), "View " + profile.getName() + "'s profile", Toast.LENGTH_SHORT).show();
                    }
                },
                (targetUid, isLike) -> handleLike(targetUid, isLike) // 🔥 LIKE CALLBACK
        );
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

    // 🔥 Load profiles + CHECK EXISTING LIKES FROM FIRESTORE
    private void loadProfiles() {
        if (getContext() == null || mAuth.getCurrentUser() == null) return;

        showLoading(true);
        tvWelcome.setText("Loading profiles...");

        String currentUserId = mAuth.getCurrentUser().getUid();

        // 🔥 STEP 1: Load MY LIKES from Firestore
        db.collection("likes")
                .whereEqualTo("likerId", currentUserId)
                .get()
                .addOnSuccessListener(likesQuery -> {
                    myLikes.clear();
                    for (var likeDoc : likesQuery) {
                        String likedUid = likeDoc.getString("likedId");
                        if (likedUid != null) {
                            myLikes.put(likedUid, true);
                            Log.d(TAG, "✅ Restored like: " + likedUid);
                        }
                    }
                    Log.d(TAG, "✅ Loaded " + myLikes.size() + " existing likes");

                    // 🔥 STEP 2: Load users (myLikes is populated)
                    loadUsersAfterLikes(currentUserId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load my likes", e);
                    loadUsersAfterLikes(currentUserId); // Fallback
                });
    }

    private void loadUsersAfterLikes(String currentUserId) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    profiles.clear();

                    for (var doc : querySnapshot) {
                        String uid = doc.getId();

                        // 🔥 SKIP CURRENT USER
                        if (uid.equals(currentUserId)) continue;

                        // 🔥 LOAD ALL FIELDS + LIKES DATA
                        String name = doc.getString("name");
                        String photoUrl = doc.getString("photoUrl");
                        String gender = doc.getString("gender");
                        String bio = doc.getString("bio");

                        Long likesCountLong = doc.getLong("likesCount");
                        int likesCount = likesCountLong != null ? likesCountLong.intValue() : 0;
                        boolean likedByMe = myLikes.getOrDefault(uid, false);

                        Profile profile = new Profile();
                        profile.setUid(uid);
                        profile.setName(name != null ? name : "User " + uid.substring(0, 6));
                        profile.setPhotoUrl(photoUrl);
                        profile.setGender(gender);
                        profile.setBio(bio);
                        profile.setLikesCount(likesCount);
                        profile.setLikedByMe(likedByMe);

                        profiles.add(profile);
                        Log.d(TAG, "✅ Loaded: " + profile.getName() + " | LikedByMe: " + likedByMe);
                    }

                    adapter.notifyDataSetChanged();
                    showLoading(false);

                    if (profiles.isEmpty()) {
                        tvWelcome.setText("No other users yet. Invite friends!");
                    } else {
                        tvWelcome.setText("Found " + profiles.size() + " users nearby");
                    }

                    updateWelcomeMessage();
                    Log.d(TAG, "✅ Profiles loaded: " + profiles.size());
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

    // 🔥 Handle like button clicks
    private void handleLike(String targetUid, boolean isLike) {
        if (mAuth.getCurrentUser() == null || getContext() == null) return;

        String currentUid = mAuth.getCurrentUser().getUid();
        String likeDocId = currentUid + "_" + targetUid;

        Map<String, Object> likeData = new HashMap<>();
        likeData.put("likerId", currentUid);
        likeData.put("likedId", targetUid);
        likeData.put("timestamp", Timestamp.now());
        likeData.put("isLike", isLike);

        if (isLike) {
            // ADD LIKE
            db.collection("likes")
                    .document(likeDocId)
                    .set(likeData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Like added for " + targetUid);
                        updateLikesCount(targetUid, 1);
                        Toast.makeText(getContext(), "Liked! ❤️", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Like failed", e);
                        Toast.makeText(getContext(), "Like failed", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // REMOVE LIKE
            db.collection("likes")
                    .document(likeDocId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "❌ Like removed for " + targetUid);
                        updateLikesCount(targetUid, -1);
                        Toast.makeText(getContext(), "Unliked", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Unlike failed", e);
                    });
        }

        // Update local tracking immediately
        myLikes.put(targetUid, isLike);

        // Update UI
        int position = findProfilePosition(targetUid);
        if (position != -1) {
            profiles.get(position).setLikedByMe(isLike);
            adapter.notifyItemChanged(position);
        }
    }

    // 🔥 Update likes count in target user's document
    private void updateLikesCount(String targetUid, int increment) {
        db.collection("users").document(targetUid)
                .update("likesCount", com.google.firebase.firestore.FieldValue.increment(increment))
                .addOnFailureListener(e -> {
                    // Field doesn't exist, create it
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("likesCount", increment);
                    db.collection("users").document(targetUid)
                            .set(updateData, SetOptions.merge())
                            .addOnSuccessListener(a -> Log.d(TAG, "✅ Created likesCount"));
                });
    }

    private int findProfilePosition(String uid) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getUid().equals(uid)) {
                return i;
            }
        }
        return -1;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvFeed != null) rvFeed.setVisibility(show ? View.GONE : View.VISIBLE);
    }

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
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Welcome message failed", e));
    }
}
