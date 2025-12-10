package com.vaibhav.bondly;

import android.content.Context;
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

        // ✅ FIXED: Load other user details WITHOUT exists()
        db.collection("users").document(chat.otherUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.getData() != null) {
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

        // ✅ TAP CHAT TO OPEN - PASS CURRENT USER ID
        holder.itemView.setOnClickListener(v -> openChat(chat));
    }

    @Override
    public int getItemCount() {
        return chatsList.size();
    }

    // ✅ OPEN CHAT WITH PROPER USER IDs
    private void openChat(Chat chat) {
        ChatFragment chatFragment = ChatFragment.newInstance(
                chat.chatId,
                chat.otherUserId,
                currentUserId  // ✅ DYNAMIC USER ID
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
