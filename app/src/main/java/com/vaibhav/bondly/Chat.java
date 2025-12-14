package com.vaibhav.bondly;

import java.util.ArrayList;
import java.util.List;

public class Chat {
    public String chatId;
    public ArrayList<String> users = new ArrayList<>();
    public String lastMessage;
    public long timestamp;
    public String otherUserId;
    public int unreadCount = 0;
    public Chat() {}
}


