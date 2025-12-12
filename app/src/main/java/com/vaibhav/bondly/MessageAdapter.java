package com.vaibhav.bondly;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private ArrayList<Message> messagesList;
    private String currentUserId;

    // 🔥 FIXED: Accept list + currentUserId
    public MessageAdapter(ArrayList<Message> messagesList, String currentUserId) {
        this.messagesList = messagesList;
        this.currentUserId = currentUserId;
        Log.d("MessageAdapter", "🔥 Initialized with currentUserId: " + currentUserId);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        Log.d("MessageAdapter", "Creating viewType: " + viewType + " (0=sent/RIGHT, 1=received/LEFT)");

        if (viewType == 0) {
            // YOUR messages - RIGHT side
            return new MessageViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false)
            );
        } else {
            // OTHER USER messages - LEFT side
            return new MessageViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false)
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messagesList.get(position);
        holder.messageText.setText(message.message);
        holder.timeText.setText(formatTime(message.timestamp));

        Log.d("MessageAdapter", "Position " + position +
                " | senderId=" + message.senderId +
                " | currentUserId=" + currentUserId +
                " | isMyMessage=" + message.senderId.equals(currentUserId));
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messagesList.get(position);
        boolean isMyMessage = message.senderId.equals(currentUserId);
        int viewType = isMyMessage ? 0 : 1;  // 0=RIGHT (you), 1=LEFT (other)
        Log.d("MessageAdapter", "getItemViewType pos=" + position +
                " sender=" + message.senderId +
                " current=" + currentUserId +
                " → viewType=" + viewType);
        return viewType;
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_message);
            timeText = itemView.findViewById(R.id.tv_time);
        }
    }
}
