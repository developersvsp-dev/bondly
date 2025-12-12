package com.vaibhav.bondly;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private ArrayList<Chat> chatsList;
    private Context context;
    private String currentUserId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ChatAdapter(ArrayList<Chat> chatsList, Context context, String currentUserId) {
        this.chatsList = chatsList;
        this.context = context;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatsList.get(position);

        // 🔥 FIX: Ensure correct otherUserId
        String otherUserId = getOtherUserId(chat);
        Log.d("ChatAdapter", "Chat: " + chat.chatId + " | Other: " + otherUserId);

        // ✅ Load other user details
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        holder.nameText.setText(name != null ? name : "Unknown User");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(context).load(photoUrl).into(holder.profileImage);
                        }
                    }
                });

        holder.lastMessageText.setText(chat.lastMessage != null ? chat.lastMessage : "Say hi to start chatting!");
        holder.timeText.setText(formatTime(chat.timestamp));

        // ✅ TAP CHAT TO OPEN
        holder.itemView.setOnClickListener(v -> openChat(chat, otherUserId));
    }

    @Override
    public int getItemCount() {
        return chatsList.size();
    }

    // 🔥 CRITICAL FIX: Get CORRECT other user (not current user)
    private String getOtherUserId(Chat chat) {
        if (chat.users != null && chat.users.size() == 2) {
            // 🔥 FORCE CORRECT ORDER: Return NON-current user
            String user1 = chat.users.get(0);
            String user2 = chat.users.get(1);

            Log.d("ChatAdapter", "Users array: [" + user1 + ", " + user2 + "] | Current: " + currentUserId);

            if (user1.equals(currentUserId)) {
                return user2;  // ✅ OTHER USER
            } else {
                return user1;  // ✅ OTHER USER
            }
        }
        return "unknown";
    }


    // 🔥 FIXED: Pass CORRECT otherUserId
    private void openChat(Chat chat, String otherUserId) {
        Log.d("ChatAdapter", "Opening chat with otherUserId: " + otherUserId);

        ChatFragment chatFragment = ChatFragment.newInstance(
                chat.chatId,
                otherUserId,        // ✅ CORRECT OTHER USER
                currentUserId       // ✅ CURRENT USER
        );

        ((AppCompatActivity) context).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack("chat")
                .commit();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText, lastMessageText, timeText;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile);
            nameText = itemView.findViewById(R.id.tv_name);
            lastMessageText = itemView.findViewById(R.id.tv_last_message);
            timeText = itemView.findViewById(R.id.tv_time);
        }
    }
}
