package com.vaibhav.bondly;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InboxFragment extends Fragment implements ChatAdapter.OnChatSelectedListener {
    private static final String TAG = "InboxFragment";

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private ArrayList<Chat> chatsList;
    private FirebaseFirestore db;
    private String currentUserId;
    private Toolbar toolbar;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 🔥 REAL-TIME LISTENERS
    private ListenerRegistration chatsListener;
    private Map<String, ListenerRegistration> unreadListeners = new HashMap<>();  // 🔥 LIVE unread counts

    // 🔥 MULTI-SELECT MODE
    private boolean isSelectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private Menu toolbarMenu;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox2, container, false);

        currentUserId = UserManager.getCurrentUserId();
        recyclerView = view.findViewById(R.id.recycler_chats);
        toolbar = view.findViewById(R.id.toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        chatsList = new ArrayList<>();
        adapter = new ChatAdapter(chatsList, getContext(), currentUserId, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        loadChats();
        setupSwipeToDelete();

        return view;
    }

    private void setupToolbar() {
        if (toolbar == null || getContext() == null) return;

        toolbar.getMenu().clear();
        toolbar.setTitle("Chats");
        toolbar.inflateMenu(R.menu.chat_options_menu);

        toolbar.setOnMenuItemClickListener(item -> {
            tintMenuIconWhite(item);
            if (item.getItemId() == R.id.action_delete_chat) {
                enterSelectionMode();
            }
            return true;
        });
    }


    public void onChatSelected(int position, boolean isChecked) {
        if (position == -1) {
            exitSelectionMode();
            return;
        }

        if (isChecked) {
            selectedPositions.add(position);
        } else {
            selectedPositions.remove(position);
        }
        updateSelectionUI();
    }

    @Override
    public void onSelectionCountChanged(int count) {
        updateSelectionUI();
    }

    private void updateSelectionUI() {
        updateToolbarTitle();

        if (selectedPositions.isEmpty() && isSelectionMode) {
            exitSelectionMode();
            return;
        }

        if (!selectedPositions.isEmpty() && isSelectionMode) {
            showDeleteMenu();
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        selectedPositions.clear();
        adapter.setSelectionMode(true);
        toolbar.setTitle("Select chats");
        Toast.makeText(getContext(), "Tap chats to select", Toast.LENGTH_SHORT).show();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedPositions.clear();
        adapter.setSelectionMode(false);
        toolbar.setTitle("Chats");
        setupToolbar();
    }

    private void updateToolbarTitle() {
        if (!selectedPositions.isEmpty()) {
            toolbar.setTitle(selectedPositions.size() + " selected");
        } else if (isSelectionMode) {
            toolbar.setTitle("Select chats");
        } else {
            toolbar.setTitle("Chats");
        }
    }

    private void showDeleteMenu() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.delete_selection_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_selected) {
                deleteSelectedChats();
            }
            return true;
        });
    }

    private void deleteSelectedChats() {
        if (selectedPositions.isEmpty()) {
            Toast.makeText(getContext(), "No chats selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Delete " + selectedPositions.size() + " chats?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Integer[] positions = selectedPositions.toArray(new Integer[0]);
                    for (int i = positions.length - 1; i >= 0; i--) {
                        int position = positions[i];
                        if (position < chatsList.size()) {
                            Chat chat = chatsList.get(position);
                            deleteSingleChat(chat, position);
                        }
                    }
                    exitSelectionMode();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    showDeleteMenu();
                })
                .show();
    }

    private void deleteSingleChat(Chat chat, int position) {
        if (chat == null || chat.chatId == null) return;

        db.collection("chats").document(chat.chatId)
                .update("deletedBy", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    mainHandler.post(() -> {
                        if (getActivity() != null && position < chatsList.size() && adapter != null) {
                            chatsList.remove(position);
                            adapter.notifyItemRemoved(position);
                        }
                    });
                });
    }

    private void tintMenuIconWhite(MenuItem item) {
        Drawable icon = item.getIcon();
        if (icon != null) {
            icon.mutate();
            icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < chatsList.size() && !isSelectionMode) {
                    Chat chat = chatsList.get(position);
                    deleteSingleChat(chat, position);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    // 🔥 PERFECT REAL-TIME CHATS LISTENER
    private void loadChats() {
        if (chatsListener != null) {
            chatsListener.remove();
        }

        chatsListener = db.collection("chats")
                .whereArrayContains("users", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Chats listen failed: " + error.getMessage());
                        return;
                    }

                    if (getActivity() == null) return;

                    chatsList.clear();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        Map<String, Chat> uniqueByPair = new HashMap<>();

                        for (var doc : snapshot.getDocuments()) {
                            Chat chat = doc.toObject(Chat.class);
                            if (chat == null || chat.users == null || chat.users.size() < 2) continue;

                            // 🔥 FIXED: Safe null + empty array check
                            if (chat.deletedBy != null && !chat.deletedBy.isEmpty() &&
                                    chat.deletedBy.contains(currentUserId)) {
                                Log.d(TAG, "⏭️ Skipping deleted chat: " + chat.chatId);
                                continue;
                            }

                            chat.chatId = doc.getId();
                            String u1 = chat.users.get(0);
                            String u2 = chat.users.get(1);
                            String key = u1.compareTo(u2) < 0 ? (u1 + "_" + u2) : (u2 + "_" + u1);

                            if (!uniqueByPair.containsKey(key)) {
                                chat.otherUserId = u1.equals(currentUserId) ? u2 : u1;
                                uniqueByPair.put(key, chat);
                                calculateUnreadCount(chat);
                            }
                        }
                        chatsList.addAll(uniqueByPair.values());
                    }

                    Log.d(TAG, "✅ LIVE Chats updated: " + chatsList.size() + " | Snapshot: " + (snapshot != null ? snapshot.size() : 0));
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }


    // 🔥 LIVE UNREAD COUNT LISTENER (PER CHAT)
    private void calculateUnreadCount(Chat chat) {
        if (chat == null || chat.chatId == null || currentUserId == null) return;

        // 🔥 Clean up old listener
        String chatIdKey = chat.chatId;
        if (unreadListeners.containsKey(chatIdKey)) {
            unreadListeners.get(chatIdKey).remove();
        }

        // 🔥 NEW LIVE unread listener
        ListenerRegistration unreadListener = db.collection("chats").document(chat.chatId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Unread listen failed: " + error.getMessage());
                        return;
                    }

                    mainHandler.post(() -> {
                        if (getActivity() != null && chat != null && adapter != null) {
                            chat.unreadCount = snapshot != null ? snapshot.size() : 0;
                            int position = chatsList.indexOf(chat);
                            if (position != -1) {
                                adapter.notifyItemChanged(position);
                            }
                            Log.d(TAG, "🔴 LIVE Unread " + chat.chatId.substring(0, 8) + "...: " + chat.unreadCount);
                        }
                    });
                });

        unreadListeners.put(chatIdKey, unreadListener);
    }

    // 🔥 NEW METHOD - NOT OVERRIDE (keeps selection mode working)
    @Override
    public void onChatSelected(String chatId, String targetUid) {
        Log.d(TAG, "💬 Thread clicked: " + chatId);

        BillingManager billingManager = BillingManager.getInstance(requireContext());
        billingManager.checkSubscriptionStatus(isSubscribed -> {
            mainHandler.post(() -> {
                if (isSubscribed) {
                    openChatFragment(chatId, targetUid);
                    Log.d(TAG, "✅ SUB ACTIVE → Chat opened: " + chatId);
                } else {
                    Log.d(TAG, "❌ NO SUB → Showing dialog");
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Send Messages")
                            .setMessage("Subscribe for ₹150/month to continue chatting!")
                            .setPositiveButton("Subscribe Now", (dialog, which) -> {
                                billingManager.launchSubscriptionPurchase(requireActivity(), new BillingManager.SubscriptionCallback() {
                                    @Override
                                    public void onCheckComplete(boolean isSubscribed) {
                                        if (isSubscribed) {
                                            openChatFragment(chatId, targetUid);
                                            Log.d(TAG, "✅ Payment success → Chat opened");
                                        }
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .setCancelable(false)
                            .show();
                }
            });
        });
    }




    // 🔥 HELPER: Open chat fragment
    private void openChatFragment(String chatId, String targetUid) {
        ChatFragment chatFragment = ChatFragment.newInstance(chatId, targetUid, currentUserId);
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack("chat")
                .commit();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // 🔥 PERFECT CLEANUP - ALL LISTENERS
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }

        // 🔥 Remove ALL unread listeners
        for (ListenerRegistration listener : unreadListeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        unreadListeners.clear();

        if (adapter != null) {
            adapter = null;
        }
        recyclerView = null;
        toolbar = null;
    }
}
