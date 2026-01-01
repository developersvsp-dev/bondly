package com.vaibhav.bondly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.vaibhav.bondly.MainActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;
import com.vaibhav.bondly.Message;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.HashMap;
import java.util.Map;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";

    private static final String ARG_CHAT_ID = "chat_id";
    private static final String ARG_OTHER_USER_ID = "other_user_id";
    private static final String ARG_CURRENT_USER_ID = "current_user_id";

    private String chatId, otherUserId, currentUserId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // 🔥 LIFECYCLE-SAFE LISTENERS
    private ListenerRegistration messagesListener;
    private ListenerRegistration userListener;

    // UI Elements
    private TextView tvChatUserName, tvOnlineStatus;
    private ImageView ivChatProfile;
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;

    // 🔥 MESSAGING
    private MessageAdapter messageAdapter;
    private ArrayList<Message> messagesList;

    // 🔥 PUBLIC newInstance()
    public static ChatFragment newInstance(String chatId, String otherUserId, String currentUserId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, chatId);
        args.putString(ARG_OTHER_USER_ID, otherUserId);
        args.putString(ARG_CURRENT_USER_ID, currentUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatId = getArguments().getString(ARG_CHAT_ID);
            otherUserId = getArguments().getString(ARG_OTHER_USER_ID);
            currentUserId = getArguments().getString(ARG_CURRENT_USER_ID);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "🔥 Chat opened: " + chatId + " with user: " + otherUserId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }

        initViews(view);
        loadChatUserHeader(view);
        setupMessageRecycler();
        setupSendButton();
        markMessagesAsRead();

        return view;
    }

    private void initViews(View view) {
        tvChatUserName = view.findViewById(R.id.tv_chat_user_name);
        ivChatProfile = view.findViewById(R.id.iv_chat_profile);
        tvOnlineStatus = view.findViewById(R.id.tv_online_status);
        recyclerMessages = view.findViewById(R.id.recycler_messages);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
    }

    private void markMessagesAsRead() {
        db.collection("chats").document(chatId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        doc.getReference().update("isRead", true);
                    }
                    Log.d(TAG, "✅ Marked " + querySnapshot.size() + " messages as read");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to mark messages as read", e);
                });
    }

    private void loadChatUserHeader(View view) {
        if (tvChatUserName == null || otherUserId == null || otherUserId.isEmpty() || db == null) {
            if (tvChatUserName != null) tvChatUserName.setText("Chat");
            return;
        }

        if (userListener != null) {
            userListener.remove();
        }

        userListener = db.collection("users").document(otherUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || !snapshot.exists()) {
                        if (tvChatUserName != null) tvChatUserName.setText("User");
                        return;
                    }

                    String name = snapshot.getString("name");
                    String photoUrl = snapshot.getString("photoUrl");
                    Boolean isOnline = snapshot.getBoolean("isOnline");
                    Long lastSeen = snapshot.getLong("lastSeen");

                    if (tvChatUserName != null) {
                        tvChatUserName.setText(name != null ? name : "User");
                    }

                    if (tvOnlineStatus != null) {
                        if (Boolean.TRUE.equals(isOnline)) {
                            tvOnlineStatus.setText("online");
                            tvOnlineStatus.setTextColor(0xFF4CAF50);
                        } else if (lastSeen != null) {
                            try {
                                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                                cal.setTimeInMillis(lastSeen);
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                                String timeStr = sdf.format(cal.getTime());

                                tvOnlineStatus.setText("last seen " + timeStr);
                                tvOnlineStatus.setTextColor(0xFF757575);
                            } catch (Exception e) {
                                tvOnlineStatus.setText("offline");
                                tvOnlineStatus.setTextColor(0xFF757575);
                            }
                        } else {
                            tvOnlineStatus.setText("offline");
                            tvOnlineStatus.setTextColor(0xFF757575);
                        }
                    }

                    if (ivChatProfile != null) {
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_person_placeholder)
                                    .error(R.drawable.ic_person_placeholder)
                                    .circleCrop()
                                    .into(ivChatProfile);
                        } else {
                            Glide.with(this)
                                    .load(R.drawable.ic_person_placeholder)
                                    .circleCrop()
                                    .into(ivChatProfile);
                        }
                    }
                });
    }

    private void setupMessageRecycler() {
        if (recyclerMessages == null) return;

        recyclerMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messagesList, currentUserId);
        recyclerMessages.setAdapter(messageAdapter);

        loadMessages();
    }

    private void loadMessages() {
        if (messagesListener != null) {
            messagesListener.remove();
        }

        messagesListener = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Message listen failed", error);
                        return;
                    }

                    if (messagesList == null || messageAdapter == null) return;

                    messagesList.clear();
                    if (snapshot != null) {
                        for (var doc : snapshot) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                messagesList.add(message);
                            }
                        }
                    }
                    messageAdapter.notifyDataSetChanged();
                    scrollToBottom();
                });
    }

    private void setupSendButton() {
        if (btnSend == null || etMessage == null) return;

        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
                etMessage.setText("");
            }
        });
    }

    // 🔥 FIXED: Updates CHAT document for real-time inbox!
    private void sendMessage(String text) {
        Message message = new Message();
        message.senderId = currentUserId;
        message.receiverId = otherUserId;
        message.message = text;
        message.timestamp = System.currentTimeMillis();
        message.isRead = false;

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "📤 Message sent: " + text);

                    // 🔥 TRIGGER INBOX UPDATE + RESURRECT DELETED CHAT
                    Map<String, Object> chatUpdate = new HashMap<>();
                    chatUpdate.put("lastMessage", text);
                    chatUpdate.put("timestamp", com.google.firebase.Timestamp.now());  // Timestamp obj
                    chatUpdate.put("deletedBy", com.google.firebase.firestore.FieldValue.delete());  // 🔥 REVIVE

                    db.collection("chats").document(chatId)
                            .update(chatUpdate)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ CHAT RESURRECTED & UPDATED - INBOX REFRESHES LIVE!");
                               // getRecipientTokenForNotification(text);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Chat metadata update failed", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Message send failed", e);
                });
    }
//    private void sendV1FCMNotification(String token, String senderName, String messageText, String chatId) {
//        String SERVER_KEY = "YOUR_SERVER_KEY_HERE";  // Firebase Console
//
//        try {
//            JSONObject json = new JSONObject();
//            json.put("to", token);
//
//            JSONObject notification = new JSONObject();
//            notification.put("title", senderName);
//            notification.put("body", messageText);
//
//            JSONObject data = new JSONObject();
//            data.put("chatId", chatId);
//            data.put("type", "message");
//
//            json.put("notification", notification);
//            json.put("data", data);
//
//            RequestQueue queue = Volley.newRequestQueue(getContext());
//            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
//                    "https://fcm.googleapis.com/fcm/send",
//                    json,  // ✅ Now JSONObject
//                    response -> Log.d(TAG, "✅ FCM SENT: " + response.toString()),
//                    error -> Log.e(TAG, "❌ FCM FAILED", error)) {
//
//                @Override
//                public Map<String, String> getHeaders() {
//                    Map<String, String> headers = new HashMap<>();
//                    headers.put("Authorization", "key=" + SERVER_KEY);
//                    headers.put("Content-Type", "application/json");
//                    return headers;
//                }
//            };
//            queue.add(request);
//        } catch (JSONException e) {
//            Log.e(TAG, "❌ JSON Error", e);
//        }
//    }




//    private void getRecipientTokenForNotification(String messageText) {
//        db.collection("users").document(currentUserId)
//                .get()
//                .addOnSuccessListener(senderSnapshot -> {
//                    String senderName = senderSnapshot.getString("name");
//                    if (senderName == null || senderName.isEmpty()) {
//                        senderName = "Someone";
//                    }
//                    String finalSenderName = senderName;
//
//                    db.collection("users").document(otherUserId)
//                            .get()
//                            .addOnSuccessListener(recipientSnapshot -> {
//                                if (!recipientSnapshot.exists()) {
//                                    Log.d(TAG, "❌ Recipient not found");
//                                    return;
//                                }
//
//                                String recipientToken = recipientSnapshot.getString("fcmToken");
//                                if (recipientToken == null || recipientToken.isEmpty()) {
//                                    Log.d(TAG, "❌ No FCM token");
//                                    return;
//                                }
//
//                                Log.d(TAG, "🚀=== FCM READY ===");
//                                Log.d(TAG, "👤 Recipient: " + otherUserId);
//                                Log.d(TAG, "🔑 TOKEN: " + recipientToken);
//                                Log.d(TAG, "💬 TITLE: " + finalSenderName);
//                                Log.d(TAG, "📝 BODY: " + messageText);
//                                Log.d(TAG, "🔥 SENDING V1 FCM!");
//                               // sendV1FCMNotification(recipientToken, finalSenderName, messageText, chatId);  // ← ADD THIS LINE
//                            })
//                            .addOnFailureListener(e ->
//                                    Log.e(TAG, "❌ Token fetch failed", e)
//                            );
//                })
//                .addOnFailureListener(e ->
//                        Log.e(TAG, "❌ Sender fetch failed", e)
//                );
//    }

    private void scrollToBottom() {
        if (messageAdapter != null && recyclerMessages != null && !messagesList.isEmpty()) {
            recyclerMessages.post(() -> {
                recyclerMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        messageAdapter = null;
        messagesList = null;

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNavigation();
        }
    }
}
