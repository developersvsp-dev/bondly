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

public class InboxFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private ArrayList<Chat> chatsList;
    private FirebaseFirestore db;
    private String currentUserId;  // ✅ DECLARED HERE

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox2, container, false);

        // ✅ AUTO DETECT CURRENT USER ID
        currentUserId = UserManager.getCurrentUserId();
        Log.d("InboxFragment", "🔥 Current User ID: " + currentUserId);

        recyclerView = view.findViewById(R.id.recycler_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        chatsList = new ArrayList<>();
        adapter = new ChatAdapter(chatsList, getContext(), currentUserId);  // ✅ Passes auto-detected ID
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadChats();

        // ✅ BACK BUTTON
        Button btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        return view;
    }

    private void loadChats() {
        Log.d("InboxFragment", "🔍 Loading chats for user: " + currentUserId);

        db.collection("chats")
                .whereArrayContains("users", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("InboxFragment", "✅ Found " + querySnapshot.size() + " chats");
                    chatsList.clear();

                    for (var doc : querySnapshot.getDocuments()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.chatId = doc.getId();

                            // ✅ SET OTHER USER ID (not current user)
                            if (chat.users != null && chat.users.size() >= 2) {
                                chat.otherUserId = chat.users.get(0).equals(currentUserId)
                                        ? chat.users.get(1)
                                        : chat.users.get(0);
                            }

                            chatsList.add(chat);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    Log.d("InboxFragment", "✅ Loaded " + chatsList.size() + " chats");
                })
                .addOnFailureListener(e -> {
                    Log.e("InboxFragment", "❌ Failed to load chats: " + e.getMessage());
                });
    }
}
