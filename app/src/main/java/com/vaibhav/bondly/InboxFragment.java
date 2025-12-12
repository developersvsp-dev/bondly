package com.vaibhav.bondly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;  // 🔥 ADD THIS
import java.util.Map;       // 🔥 ADD THIS

public class InboxFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private ArrayList<Chat> chatsList;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox2, container, false);

        currentUserId = UserManager.getCurrentUserId();
        Log.d("InboxFragment", "🔥 Current User ID: " + currentUserId);

        recyclerView = view.findViewById(R.id.recycler_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        chatsList = new ArrayList<>();
        adapter = new ChatAdapter(chatsList, getContext(), currentUserId);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadChats();

        return view;
    }

    private void loadChats() {
        Log.d("InboxFragment", "🔍 Loading chats for: " + currentUserId);

        db.collection("chats")
                .whereArrayContains("users", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("InboxFragment", "❌ Error: " + error.getMessage());
                        return;
                    }

                    chatsList.clear();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        Log.d("InboxFragment", "✅ Found " + snapshot.size() + " chats!");

                        // 🔥 DEDUPE: One chat per user pair
                        Map<String, Chat> uniqueByPair = new HashMap<>();

                        for (var doc : snapshot.getDocuments()) {
                            Chat chat = doc.toObject(Chat.class);
                            if (chat == null || chat.users == null || chat.users.size() < 2) continue;

                            chat.chatId = doc.getId();

                            String u1 = chat.users.get(0);
                            String u2 = chat.users.get(1);
                            // 🔥 CANONICAL KEY (alphabetical order)
                            String key = u1.compareTo(u2) < 0 ? (u1 + "_" + u2) : (u2 + "_" + u1);

                            // Keep only FIRST (newest due to DESC order)
                            if (!uniqueByPair.containsKey(key)) {
                                chat.otherUserId = u1.equals(currentUserId) ? u2 : u1;
                                uniqueByPair.put(key, chat);
                            }
                        }

                        chatsList.addAll(uniqueByPair.values());
                    } else {
                        Log.d("InboxFragment", "📭 No chats yet");
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("InboxFragment", "✅ Loaded " + chatsList.size() + " chats (deduped)");
                });
    }
}
