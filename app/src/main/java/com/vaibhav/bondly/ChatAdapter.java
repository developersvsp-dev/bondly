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
        String otherUserId = getOtherUserId(chat);

        // Load user details
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        holder.nameText.setText(name != null ? name : "Unknown User");

                        Glide.with(context)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person_placeholder)
                                .error(R.drawable.ic_person_placeholder)
                                .circleCrop()
                                .into(holder.profileImage);
                    }
                });

        // Last message
        holder.lastMessageText.setText(chat.lastMessage != null ? chat.lastMessage : "No messages yet");

        // Time
        holder.timeText.setText(formatTime(chat.timestamp));

        // 🔥 UNREAD COUNT BADGE ONLY (NO DOT)
        if (chat.unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(chat.unreadCount > 99 ? "99+" : String.valueOf(chat.unreadCount));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        // Open chat
        holder.itemView.setOnClickListener(v -> openChat(chat, otherUserId));
    }

    @Override
    public int getItemCount() {
        return chatsList.size();
    }

    private String getOtherUserId(Chat chat) {
        if (chat.users != null && chat.users.size() == 2) {
            String user1 = chat.users.get(0);
            String user2 = chat.users.get(1);
            return user1.equals(currentUserId) ? user2 : user1;
        }
        return "unknown";
    }

    private void openChat(Chat chat, String otherUserId) {
        ChatFragment chatFragment = ChatFragment.newInstance(chat.chatId, otherUserId, currentUserId);
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
        TextView nameText, lastMessageText, timeText, tvUnreadCount;
        // View unreadDot;  // 🔥 REMOVED - NO MORE DOT

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile);
            nameText = itemView.findViewById(R.id.tv_name);
            lastMessageText = itemView.findViewById(R.id.tv_last_message);
            timeText = itemView.findViewById(R.id.tv_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            // unreadDot removed completely
        }
    }
}
