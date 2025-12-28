// 🔥 FULLY UPDATED MatchesAdapter.java - WORKS EVERY CLICK
package com.vaibhav.bondly;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    private MatchesFragment fragment;
    private long lastClickTime = 0;

    public MatchesAdapter(ArrayList<User> usersList, Context context, MatchesFragment fragment) {
        this.usersList = usersList;
        this.context = context;
        this.currentUserId = UserManager.getCurrentUserId();
        this.fragment = fragment;
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

        holder.itemView.setOnClickListener(v -> handleUserClick(user));
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    // 🔥 PERFECT FIX FROM FeedFragment - MAIN THREAD + NULL CHECKS
    private void handleUserClick(User user) {
        Log.d("MatchesAdapter", "👆 User clicked: " + user.name);

        // 🔥 DEBOUNCE
        if (System.currentTimeMillis() - lastClickTime < 1000) {
            Log.d("MatchesAdapter", "⏳ Too fast - ignoring click");
            return;
        }
        lastClickTime = System.currentTimeMillis();

        // 🔥 NULL CHECKS (FeedFragment pattern)
        if (context == null || fragment == null || fragment.getActivity() == null) {
            Log.d("MatchesAdapter", "🚫 Context/Fragment destroyed");
            return;
        }

        BillingManager billingManager = BillingManager.getInstance(context);
        billingManager.checkSubscriptionStatus(isSubscribed -> {
            Log.d("MatchesAdapter", "🎯 CALLBACK FIRED: isSubscribed = " + isSubscribed);

            // 🔥 CRITICAL NULL CHECKS
            if (fragment.getActivity() == null || context == null) {
                Log.d("MatchesAdapter", "🚫 Fragment destroyed");
                return;
            }

            // 🔥 MAIN THREAD GUARANTEE (exact FeedFragment pattern)
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isSubscribed) {
                    Log.d("MatchesAdapter", "✅ SUB ACTIVE - Opening chat");
                    startChat(user.userId);
                } else {
                    Log.d("MatchesAdapter", "❌ NO SUB - SHOWING DIALOG");

                    new AlertDialog.Builder(context)
                            .setTitle("🔒 Unlock Chat")
                            .setMessage("Subscribe for ₹150/month to chat with " + user.name + "?")
                            .setCancelable(true)
                            .setPositiveButton("Subscribe Now", (dialog, which) -> {
                                Log.d("MatchesAdapter", "👆 SUBSCRIBE BUTTON TAPPED");
                                if (context instanceof Activity) {
                                    billingManager.launchSubscriptionPurchase((Activity) context, subscribed -> {
                                        Log.d("MatchesAdapter", "💰 PURCHASE CALLBACK: " + subscribed);
                                        if (subscribed) {
                                            fragment.onPaymentSuccess(user.userId);
                                        } else {
                                            Toast.makeText(context, "Subscription required to chat", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Log.d("MatchesAdapter", "❌ CANCEL TAPPED");
                            })
                            .setOnCancelListener(dialog -> {
                                Log.d("MatchesAdapter", "❌ DIALOG CANCELLED (back button)");
                            })
                            .setOnDismissListener(dialog -> {
                                Log.d("MatchesAdapter", "✅ DIALOG DISMISSED");
                            })
                            .show();
                }
            });
        });
    }

    public void startChat(String otherUserId) {
        String[] userIds = {currentUserId, otherUserId};
        Arrays.sort(userIds);
        String chatId = userIds[0] + "_" + userIds[1];

        Log.d("MatchesAdapter", "🔗 Chat ID: " + chatId);

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
                    Log.d("MatchesAdapter", "✅ Chat opened");
                })
                .addOnFailureListener(e -> {
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

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile);
            nameText = itemView.findViewById(R.id.tv_name);
            bioText = itemView.findViewById(R.id.tv_bio);
        }
    }
}
