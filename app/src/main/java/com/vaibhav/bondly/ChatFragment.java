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
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;  // 🔥 ADD

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";

    private static final String ARG_CHAT_ID = "chat_id";
    private static final String ARG_OTHER_USER_ID = "other_user_id";
    private static final String ARG_CURRENT_USER_ID = "current_user_id";

    private String chatId, otherUserId, currentUserId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private TextView tvChatUserName, tvOnlineStatus;
    private ImageView ivChatProfile;
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;

    // 🔥 MESSAGING
    private MessageAdapter messageAdapter;
    private ArrayList<Message> messagesList;

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

        // 🔥 HIDE BOTTOM NAV (WhatsApp style)
        if (getActivity() instanceof com.vaibhav.bondly.MainActivity) {
            ((com.vaibhav.bondly.MainActivity) getActivity()).hideBottomNavigation();
        }

        initViews(view);
        loadChatUserHeader(view);
        setupMessageRecycler();
        setupSendButton();

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

    // 🔥 UPDATED: isOnline=true → "online" | isOnline=false → "last seen HH:mm"
    private void loadChatUserHeader(View view) {
        if (tvChatUserName == null || otherUserId == null || otherUserId.isEmpty() || db == null) {
            if (tvChatUserName != null) tvChatUserName.setText("Chat");
            return;
        }

        db.collection("users").document(otherUserId)
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
                                Log.d("CHAT_DEBUG", "🔥 RAW: " + lastSeen);

                                // TEST 1: Device time (what's wrong)
                                Date rawDate = new Date(lastSeen);
                                SimpleDateFormat deviceSdf = new SimpleDateFormat("HH:mm z", Locale.getDefault());
                                Log.d("CHAT_DEBUG", "🔥 DEVICE TIME: " + deviceSdf.format(rawDate));

                                // TEST 2: Pure IST Calendar
                                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                                cal.setTimeInMillis(lastSeen);
                                SimpleDateFormat istSdf = new SimpleDateFormat("HH:mm z", Locale.getDefault());
                                istSdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                                Log.d("CHAT_DEBUG", "🔥 IST CALENDAR: " + istSdf.format(cal.getTime()));

                                // FINAL DISPLAY
                                SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                displaySdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                                String timeStr = displaySdf.format(cal.getTime());

                                Log.d("CHAT_DEBUG", "🔥 FINAL DISPLAY: " + timeStr);

                                tvOnlineStatus.setText("last seen " + timeStr);
                                tvOnlineStatus.setTextColor(0xFF757575);
                            } catch (Exception e) {
                                Log.e("CHAT_ERROR", "Time format error", e);
                                tvOnlineStatus.setText("offline");
                            }
                        } else {
                            tvOnlineStatus.setText("offline");
                            tvOnlineStatus.setTextColor(0xFF757575);
                        }
                    }

                    if (photoUrl != null && !photoUrl.isEmpty() && ivChatProfile != null) {
                        Glide.with(this).load(photoUrl).circleCrop().into(ivChatProfile);
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
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Message listen failed", error);
                        return;
                    }

                    if (messagesList == null) return;

                    messagesList.clear();
                    if (snapshot != null) {
                        for (var doc : snapshot) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                messagesList.add(message);
                            }
                        }
                    }
                    if (messageAdapter != null) {
                        messageAdapter.notifyDataSetChanged();
                        scrollToBottom();
                    }
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

    private void sendMessage(String text) {
        Message message = new Message();
        message.senderId = currentUserId;
        message.receiverId = otherUserId;
        message.message = text;
        message.timestamp = System.currentTimeMillis();

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "📤 Message sent: " + text);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                });
    }

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
        if (getActivity() instanceof com.vaibhav.bondly.MainActivity) {
            ((com.vaibhav.bondly.MainActivity) getActivity()).showBottomNavigation();
        }
    }
}
