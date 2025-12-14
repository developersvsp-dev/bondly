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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.UserViewHolder> {
    private ArrayList<User> usersList;
    private Context context;
    private String currentUserId;

    public MatchesAdapter(ArrayList<User> usersList, Context context) {
        this.usersList = usersList;
        this.context = context;
        this.currentUserId = UserManager.getCurrentUserId();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_liker, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersList.get(position);
        holder.nameText.setText(user.name);
        holder.bioText.setText(user.bio);

        if (user.profileImage != null && !user.profileImage.isEmpty()) {
            Glide.with(context).load(user.profileImage).into(holder.profileImage);
        }

        holder.itemView.setOnClickListener(v -> startChat(user.userId));
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    // ✅ FIXED: CONSISTENT CHAT ID FOR BOTH USERS
    private void startChat(String otherUserId) {
        // 🔥 ALWAYS CREATE SAME CHAT ID REGARDLESS OF WHO STARTS
        String[] userIds = {currentUserId, otherUserId};
        Arrays.sort(userIds);  // Alphabetical: smaller ID first
        String chatId = userIds[0] + "_" + userIds[1];

        Log.d("MatchesAdapter", "🔗 Chat ID: " + chatId + " (current=" + currentUserId + ", other=" + otherUserId + ")");

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("users", Arrays.asList(currentUserId, otherUserId));
        chatData.put("lastMessage", "");
        chatData.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("chats")
                .document(chatId)
                .set(chatData)
                .addOnSuccessListener(aVoid -> {
                    ChatFragment chatFragment = ChatFragment.newInstance(chatId, otherUserId, currentUserId);
                    ((AppCompatActivity) context).getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, chatFragment)
                            .addToBackStack("chat")
                            .commit();
                })
                .addOnFailureListener(e -> {
                    // Chat exists - open anyway
                    Log.d("MatchesAdapter", "Chat already exists, opening...");
                    ChatFragment chatFragment = ChatFragment.newInstance(chatId, otherUserId, currentUserId);
                    ((AppCompatActivity) context).getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, chatFragment)
                            .addToBackStack("chat")
                            .commit();
                });
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText, bioText;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile);
            nameText = itemView.findViewById(R.id.tv_name);
            bioText = itemView.findViewById(R.id.tv_bio);
        }
    }
}
