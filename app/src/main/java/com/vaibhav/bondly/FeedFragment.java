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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
    private List<Profile> allProfiles = new ArrayList<>();  // 🔥 FULL LIST
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Map<String, Boolean> myLikes = new HashMap<>();

    // 🔥 FILTER STATE
    private Button btnFilterAll, btnFilterMale, btnFilterFemale;
    private String currentFilter = "all";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        rvFeed = view.findViewById(R.id.rvFeed);
        progressBar = view.findViewById(R.id.progressBar);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        Button premiumBtn = view.findViewById(R.id.btnGoPremium);

        // 🔥 FILTER BUTTONS
        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterMale = view.findViewById(R.id.btnFilterMale);
        btnFilterFemale = view.findViewById(R.id.btnFilterFemale);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profiles = new ArrayList<>();
        adapter = new ProfileAdapter(profiles,
                profile -> {
                    if (getContext() != null && profile != null) {
                        Toast.makeText(getContext(), "View " + profile.getName() + "'s profile", Toast.LENGTH_SHORT).show();
                    }
                },
                (targetUid, isLike) -> handleLike(targetUid, isLike),
                targetUid -> handleMessage(targetUid)
        );

        if (rvFeed != null) {
            rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
            rvFeed.setAdapter(adapter);
        }

        if (premiumBtn != null) {
            premiumBtn.setOnClickListener(v -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), PremiumActivity.class));
                }
            });
        }

        // 🔥 SETUP FILTERS
        setupFilters();

        new Handler().postDelayed(this::loadProfiles, 200);
        return view;
    }

    // 🔥 NEW: FILTER SETUP
    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> setFilter("all"));
        btnFilterMale.setOnClickListener(v -> setFilter("male"));
        btnFilterFemale.setOnClickListener(v -> setFilter("female"));
        updateFilterButtons();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        filterProfiles();
        updateFilterButtons();
    }

    private void filterProfiles() {
        profiles.clear();

        for (Profile profile : allProfiles) {
            if (currentFilter.equals("all") ||
                    (currentFilter.equals("male") && "male".equalsIgnoreCase(profile.getGender())) ||
                    (currentFilter.equals("female") && "female".equalsIgnoreCase(profile.getGender()))) {
                profiles.add(profile);
            }
        }

        adapter.notifyDataSetChanged();
        if (tvWelcome != null) {
            tvWelcome.setText("Showing " + profiles.size() + " " + currentFilter + " profiles");
        }
    }

    private void updateFilterButtons() {
        int selectedColor = getResources().getColor(android.R.color.holo_blue_dark, null);
        int normalColor = getResources().getColor(android.R.color.darker_gray, null);

        // Reset all
        btnFilterAll.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        btnFilterMale.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        btnFilterFemale.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));

        // Highlight selected
        switch (currentFilter) {
            case "all":
                btnFilterAll.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                break;
            case "male":
                btnFilterMale.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                break;
            case "female":
                btnFilterFemale.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                break;
        }
    }

    private void loadProfiles() {
        if (getContext() == null || mAuth.getCurrentUser() == null) return;

        showLoading(true);
        tvWelcome.setText("Loading profiles...");

        String currentUserId = mAuth.getCurrentUser().getUid();

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
                    loadUsersAfterLikes(currentUserId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load my likes", e);
                    loadUsersAfterLikes(currentUserId);
                });
    }

    private void loadUsersAfterLikes(String currentUserId) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    allProfiles.clear();  // 🔥 CLEAR FULL LIST
                    List<Profile> tempProfiles = new ArrayList<>();  // 🔥 TEMP LIST

                    for (var doc : querySnapshot) {
                        String uid = doc.getId();
                        if (uid.equals(currentUserId)) continue;

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

                        tempProfiles.add(profile);
                        Log.d(TAG, "✅ Loaded: " + profile.getName() + " | Gender: " + gender + " | LikedByMe: " + likedByMe);
                    }

                    // 🔥 STORE IN FULL LIST + APPLY FILTER
                    allProfiles.addAll(tempProfiles);
                    filterProfiles();  // 🔥 AUTO FILTER
                    showLoading(false);

                    Log.d(TAG, "✅ Profiles loaded: " + allProfiles.size() + " total");
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

    // 🔥 REST OF YOUR EXISTING METHODS (unchanged)
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

        myLikes.put(targetUid, isLike);
        int position = findProfilePosition(targetUid);
        if (position != -1) {
            profiles.get(position).setLikedByMe(isLike);
            adapter.notifyItemChanged(position);
        }
    }
  //  private static final boolean DEBUG_MODE = true;
    private void handleMessage(String targetUid) {
        Log.d(TAG, "🚀 MESSAGE TAP - UID: " + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "NULL"));

        if (mAuth.getCurrentUser() == null || getContext() == null) {
            Log.d(TAG, "🚫 Auth/Context null");
            return;
        }

        // 🔥🔥 DEBUG BYPASS - REMOVES PAYMENT CHECK IN DEBUG BUILDS 🔥🔥
        //if (DEBUG_MODE) {
        //    Log.d(TAG, "🔓 DEBUG MODE ACTIVE - FREE CHAT ACCESS");
        //    openChat(targetUid);
         //   return;
      //  }

        BillingManager billingManager = BillingManager.getInstance(getContext());
        billingManager.checkSubscriptionStatus(isSubscribed -> {
            Log.d(TAG, "🎯 CALLBACK FIRED: isSubscribed = " + isSubscribed);

            if (getActivity() == null || getContext() == null) {
                Log.d(TAG, "🚫 Fragment destroyed");
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (isSubscribed) {
                    Log.d(TAG, "✅ SUB ACTIVE - Opening chat");
                    openChat(targetUid);
                } else {
                    Log.d(TAG, "❌ NO SUB - SHOWING DIALOG");

                    Toast.makeText(requireContext(), "🚨 SUBSCRIBE TO CHAT!", Toast.LENGTH_LONG).show();

                    new AlertDialog.Builder(requireContext())
                            .setTitle("🔒 Send Messages")
                            .setMessage("Subscribe for ₹150/month to unlock unlimited messaging!")
                            .setCancelable(true)
                            .setPositiveButton("Subscribe Now", (dialog, which) -> {
                                Log.d(TAG, "👆 SUBSCRIBE BUTTON TAPPED");
                                billingManager.launchSubscriptionPurchase(requireActivity(), subscribed -> {
                                    Log.d(TAG, "💰 PURCHASE CALLBACK: " + subscribed);
                                    if (subscribed) {
                                        Toast.makeText(getContext(), "✅ Chat unlocked!", Toast.LENGTH_SHORT).show();
                                        openChat(targetUid);
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Log.d(TAG, "❌ CANCEL TAPPED");
                            })
                            .setOnCancelListener(dialog -> {
                                Log.d(TAG, "❌ DIALOG CANCELLED (back button)");
                            })
                            .setOnDismissListener(dialog -> {
                                Log.d(TAG, "✅ DIALOG DISMISSED");
                            })
                            .show();
                }
            });
        });
    }

    private void openChat(String targetUid) {
        String currentUid = mAuth.getCurrentUser().getUid();
        String chatId = currentUid.compareTo(targetUid) < 0
                ? currentUid + "_" + targetUid
                : targetUid + "_" + currentUid;

        // 🔥 CREATE TOP-LEVEL CHAT DOCUMENT (Triggers Inbox!)
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("users", java.util.Arrays.asList(currentUid, targetUid));
        chatData.put("timestamp", com.google.firebase.Timestamp.now());
        chatData.put("lastMessage", "Say hi to start chatting!");

        db.collection("chats").document(chatId)
                .set(chatData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ CHAT DOC CREATED: " + chatId + " → INBOX UPDATES!");

                    // 🔥 RESURRECT DELETED CHAT - Clear deletedBy for BOTH users
                    Map<String, Object> resurrectData = new HashMap<>();
                    resurrectData.put("deletedBy", com.google.firebase.firestore.FieldValue.delete());

                    db.collection("chats").document(chatId)
                            .update(resurrectData)
                            .addOnSuccessListener(resurrect -> {
                                Log.d(TAG, "🔥 CHAT RESURRECTED - VISIBLE IN BOTH INBOXES!");
                            })
                            .addOnFailureListener(e -> Log.w(TAG, "⚠️ Resurrection failed (ok if not deleted)", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Chat doc failed", e));

        // Navigate to ChatFragment
        ChatFragment chatFragment = ChatFragment.newInstance(chatId, targetUid, currentUid);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack("chat")
                    .commit();
        }

        Log.d(TAG, "💬 Opening chat: " + chatId);
        Toast.makeText(getContext(), "Opening chat...", Toast.LENGTH_SHORT).show();
    }



    private void updateLikesCount(String targetUid, int increment) {
        db.collection("users").document(targetUid)
                .update("likesCount", com.google.firebase.firestore.FieldValue.increment(increment))
                .addOnFailureListener(e -> {
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
        if (rvFeed != null) rvFeed.setLayoutManager(show ? null : new LinearLayoutManager(getContext()));
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
                        if (name != null) {
                            String welcome = "Welcome back, " + name + "!";
                            tvWelcome.setText(welcome);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Welcome message failed", e));
    }
}
