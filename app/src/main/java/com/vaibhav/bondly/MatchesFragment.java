package com.vaibhav.bondly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;  // 🔥 THIS WAS MISSING
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MatchesFragment extends Fragment {
    private RecyclerView recyclerView;
    private MatchesAdapter adapter;
    private ArrayList<User> likersList;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration likesListener;
    private Set<String> currentLikerIds = new HashSet<>(); // 🔥 UNLIKE FIX only

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matches, container, false);

        currentUserId = UserManager.getCurrentUserId();
        Log.d("MatchesFragment", "🔥 Current User ID: " + currentUserId);

        recyclerView = view.findViewById(R.id.recycler_matches);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        likersList = new ArrayList<>();
        adapter = new MatchesAdapter(likersList, getContext(), this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        setupRealtimeListener();

        Button btnInbox = view.findViewById(R.id.btn_inbox);
        if (btnInbox != null) {
            btnInbox.setOnClickListener(v -> navigateToInbox());
        }

        return view;
    }

    // 🔥 REAL-TIME MATCHES ONLY (no green dot logic)
    private void setupRealtimeListener() {
        Log.d("MatchesFragment", "🔥 Starting real-time listener for user: " + currentUserId);

        likesListener = db.collection("likes")
                .whereEqualTo("likedId", currentUserId)
                .whereEqualTo("isLike", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("MatchesFragment", "❌ Listener error: " + error.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        Log.d("MatchesFragment", "🔥 REAL-TIME: " + snapshots.size() + " matches found");
                        updateMatchesList(snapshots.getDocuments());
                    }
                });
    }

    // 🔥 INSTANT UNLIKE REMOVAL (core feature preserved)
    private void updateMatchesList(java.util.List<DocumentSnapshot> likeDocs) {
        Set<String> newLikerIds = new HashSet<>();

        for (DocumentSnapshot doc : likeDocs) {
            String likerId = doc.getString("likerId");
            if (likerId != null && !likerId.equals(currentUserId)) {
                newLikerIds.add(likerId);
            }
        }

        Log.d("MatchesFragment", "🔥 Processing " + newLikerIds.size() + " unique matches");

        currentLikerIds = newLikerIds;

        likersList.clear();
        adapter.notifyDataSetChanged();
        Log.d("MatchesFragment", "🔥 UI cleared - loading matches");

        for (String likerId : currentLikerIds) {
            fetchUserDetails(likerId);
        }
    }

    private void fetchUserDetails(String likerId) {
        if (!currentLikerIds.contains(likerId)) {
            Log.d("MatchesFragment", "❌ Skipped " + likerId + " - no longer active");
            return;
        }

        db.collection("users").document(likerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!currentLikerIds.contains(likerId)) {
                        Log.d("MatchesFragment", "❌ User " + likerId + " unliked during fetch");
                        return;
                    }

                    if (documentSnapshot.exists()) {
                        User user = new User();
                        user.userId = likerId;
                        user.name = documentSnapshot.getString("name") != null ?
                                documentSnapshot.getString("name") : "Unknown User";
                        user.bio = documentSnapshot.getString("bio") != null ?
                                documentSnapshot.getString("bio") : "No bio";
                        user.profileImage = documentSnapshot.getString("photoUrl");

                        if (!likersList.contains(user)) {
                            likersList.add(user);
                            adapter.notifyDataSetChanged();
                            Log.d("MatchesFragment", "✅ Added match: " + user.name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MatchesFragment", "❌ Failed to fetch user " + likerId);
                });
    }

    public void onPaymentSuccess(String targetUserId) {
        Log.d("MatchesFragment", "💎 Payment success - opening chat with: " + targetUserId);
        adapter.startChat(targetUserId);
    }

    private void navigateToInbox() {
        InboxFragment inboxFragment = new InboxFragment();
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, inboxFragment)
                .addToBackStack("inbox")
                .commit();
        Log.d("MatchesFragment", "📱 Navigated to Inbox");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (likesListener != null) {
            likesListener.remove();
            Log.d("MatchesFragment", "🔥 Listener removed");
        }
    }
}
