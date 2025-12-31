package com.vaibhav.bondly;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private ArrayList<Chat> chatsList;
    private Context context;
    private String currentUserId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 🔥 SELECTION MODE SUPPORT
    private boolean selectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private OnChatSelectedListener listener;

    // 🔥 FIXED INTERFACE - BOTH METHODS REQUIRED
    public interface OnChatSelectedListener {
        void onChatSelected(int position, boolean isChecked);      // 🔥 SELECTION
        void onChatSelected(String chatId, String targetUid);      // 🔥 CHAT OPEN
        void onSelectionCountChanged(int count);
    }

    public ChatAdapter(ArrayList<Chat> chatsList, Context context, String currentUserId, OnChatSelectedListener listener) {
        this.chatsList = chatsList;
        this.context = context;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        if (!selectionMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public boolean isPositionSelected(int position) {
        return selectedPositions.contains(position);
    }

    public int getSelectedCount() {
        return selectedPositions.size();
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

        // 🔥 PERFECT CHECKBOX BINDING
        holder.checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.checkbox.setOnCheckedChangeListener(null);  // Prevent double calls
        holder.checkbox.setChecked(isPositionSelected(position));
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("ChatAdapter", "Checkbox: pos=" + position + ", checked=" + isChecked);
            toggleSelection(position, isChecked);
        });

        // 🔥 LOAD USER DATA
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

        // 🔥 REAL-TIME LAST MESSAGE + UNREAD
        holder.lastMessageText.setText(chat.lastMessage != null ? chat.lastMessage : "No messages yet");
        holder.timeText.setText(formatTime(chat.timestamp));

        if (chat.unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(chat.unreadCount > 99 ? "99+" : String.valueOf(chat.unreadCount));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        // 🔥 CLICK HANDLER
        holder.itemView.setOnClickListener(v -> {
            Log.d("ChatAdapter", "Item click: pos=" + position + ", selection=" + selectionMode);
            if (selectionMode) {
                boolean newState = !isPositionSelected(position);
                holder.checkbox.setChecked(newState);
            } else {
                Chat chatItem = chatsList.get(position);
                String targetUid = getOtherUserId(chatItem);
                if (listener != null) {
                    listener.onChatSelected(chatItem.chatId, targetUid);
                }
            }
        });
    }

    // 🔥 FIXED: CALLS BOTH INTERFACE METHODS
    private void toggleSelection(int position, boolean isChecked) {
        if (isChecked) {
            selectedPositions.add(position);
        } else {
            selectedPositions.remove(position);
        }
        notifyItemChanged(position);
        if (listener != null) {
            listener.onChatSelected(position, isChecked);        // 🔥 SELECTION CALLBACK
            listener.onSelectionCountChanged(getSelectedCount()); // 🔥 COUNT CALLBACK
        }
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

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText, lastMessageText, timeText, tvUnreadCount;
        CheckBox checkbox;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile);
            nameText = itemView.findViewById(R.id.tv_name);
            lastMessageText = itemView.findViewById(R.id.tv_last_message);
            timeText = itemView.findViewById(R.id.tv_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            checkbox = itemView.findViewById(R.id.checkbox);
        }
    }
}
