package com.vaibhav.bondly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;

public class MatchesFragment extends Fragment {
    private RecyclerView recyclerView;
    private MatchesAdapter adapter;
    private ArrayList<User> likersList;
    private FirebaseFirestore db;
    private String currentUserId = "WoYpUC8XCDa8ITNPPCdc3rKRuEZ2"; // YOUR CORRECT USER ID

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matches, container, false);

        recyclerView = view.findViewById(R.id.recycler_matches);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        likersList = new ArrayList<>();
        adapter = new MatchesAdapter(likersList, getContext());
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadLikers();

        return view;
    }

    private void loadLikers() {
        Log.d("MatchesFragment", "🔍 Searching likes for user: " + currentUserId);

        db.collection("likes")
                .whereEqualTo("likedId", currentUserId)
                .whereEqualTo("isLike", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("MatchesFragment", "✅ Found " + querySnapshot.size() + " likes");

                    likersList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String likerId = doc.getString("likerId");
                        Log.d("MatchesFragment", "👤 Liker ID: " + likerId);

                        if (likerId != null && !likerId.equals(currentUserId)) {
                            fetchUserDetails(likerId);
                        } else {
                            Log.d("MatchesFragment", "⏭️ Skipping self-like");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MatchesFragment", "❌ Query failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Error loading likes", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserDetails(String likerId) {
        db.collection("users").document(likerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = new User();
                        user.userId = likerId;
                        user.name = documentSnapshot.getString("name") != null ?
                                documentSnapshot.getString("name") : "Unknown User";
                        user.bio = documentSnapshot.getString("bio") != null ?
                                documentSnapshot.getString("bio") : "No bio";

                        // ✅ FETCH PHOTO URL FROM FIREBASE
                        user.profileImage = documentSnapshot.getString("photoUrl");

                        likersList.add(user);
                        adapter.notifyDataSetChanged();
                        Log.d("MatchesFragment", "✅ Added user: " + user.name + " with photo: " + user.profileImage);
                    }
                });
    }
}
