package com.vaibhav.bondly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatFragment extends Fragment {
    private RecyclerView recyclerMessages;
    private MessageAdapter messageAdapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private FirebaseFirestore db;
    private String chatId, otherUserId, currentUserId;

    public static ChatFragment newInstance(String chatId, String otherUserId, String currentUserId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("chatId", chatId);
        args.putString("otherUserId", otherUserId);
        args.putString("currentUserId", currentUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatId = getArguments().getString("chatId");
        otherUserId = getArguments().getString("otherUserId");
        currentUserId = getArguments().getString("currentUserId");

        db = FirebaseFirestore.getInstance();
        setupRecyclerView(view);
        setupSendButton(view);
        listenForMessages();
        return view;
    }

    private void setupRecyclerView(View view) {
        recyclerMessages = view.findViewById(R.id.recycler_messages);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        messageAdapter = new MessageAdapter(currentUserId);
        recyclerMessages.setAdapter(messageAdapter);
        ((LinearLayoutManager) recyclerMessages.getLayoutManager()).setStackFromEnd(true);
    }

    private void setupSendButton(View view) {
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("senderId", currentUserId);
            messageData.put("receiverId", otherUserId);
            messageData.put("message", message);
            messageData.put("timestamp", System.currentTimeMillis());

            db.collection("chats").document(chatId)
                    .collection("messages").add(messageData)
                    .addOnSuccessListener(doc -> {
                        etMessage.setText("");
                        Map<String, Object> chatUpdate = new HashMap<>();
                        chatUpdate.put("lastMessage", message);
                        chatUpdate.put("timestamp", System.currentTimeMillis());
                        db.collection("chats").document(chatId).update(chatUpdate);
                    });
        }
    }

    private void listenForMessages() {
        db.collection("chats").document(chatId)
                .collection("messages").orderBy("timestamp")
                .limit(50).addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null) {
                        ArrayList<Message> tempList = new ArrayList<>();
                        for (var doc : snapshots.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) tempList.add(message);
                        }
                        messageAdapter.updateMessages(tempList);
                        recyclerMessages.postDelayed(() -> {
                            if (tempList.size() > 0) {
                                recyclerMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                            }
                        }, 200);
                    }
                });
    }
}
