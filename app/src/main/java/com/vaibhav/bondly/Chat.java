package com.vaibhav.bondly;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.List;

public class Chat {
    public String chatId;
    public ArrayList<String> users = new ArrayList<>();
    public String lastMessage;
    public long timestamp;
    public String otherUserId;
    public int unreadCount = 0;

    // 🔥 NEW: For delete chat feature (WhatsApp style)
    @PropertyName("deletedBy")
    public List<String> deletedBy = new ArrayList<>();

    public Chat() {}
}
