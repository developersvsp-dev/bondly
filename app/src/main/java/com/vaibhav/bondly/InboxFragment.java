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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InboxFragment extends Fragment implements ChatAdapter.OnChatSelectedListener {
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private ArrayList<Chat> chatsList;
    private FirebaseFirestore db;
    private String currentUserId;
    private Toolbar toolbar;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 🔥 MULTI-SELECT MODE
    private boolean isSelectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private Menu toolbarMenu; // 🔥 For 3-dots control

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

        // 🔥 YOUR ORIGINAL WORKING TOOLBAR SETUP
        setupToolbar();
        loadChats();
        setupSwipeToDelete();

        return view;
    }

    // 🔥 YOUR ORIGINAL WORKING TOOLBAR (KEEPS 3-DOTS WORKING)
    private void setupToolbar() {
        if (toolbar == null || getContext() == null) return;

        toolbar.getMenu().clear();
        toolbar.setTitle("Chats");
        toolbar.inflateMenu(R.menu.chat_options_menu); // YOUR ORIGINAL MENU

        toolbar.setOnMenuItemClickListener(item -> {
            tintMenuIconWhite(item);
            if (item.getItemId() == R.id.action_delete_chat) {
                enterSelectionMode(); // 🔥 THIS WORKS!
            }
            return true;
        });
    }

    // 🔥 INTERFACE IMPLEMENTATION
    @Override
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

    // 🔥 UPDATE UI (Title + Delete button)
    private void updateSelectionUI() {
        updateToolbarTitle();

        if (selectedPositions.isEmpty() && isSelectionMode) {
            exitSelectionMode();
            return;
        }

        // 🔥 Switch to delete menu when selected
        if (!selectedPositions.isEmpty() && isSelectionMode) {
            showDeleteMenu();
        }
    }

    // 🔥 ENTER SELECTION MODE
    private void enterSelectionMode() {
        isSelectionMode = true;
        selectedPositions.clear();
        adapter.setSelectionMode(true);
        toolbar.setTitle("Select chats");
        Toast.makeText(getContext(), "Tap chats to select", Toast.LENGTH_SHORT).show();
    }

    // 🔥 EXIT SELECTION MODE
    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedPositions.clear();
        adapter.setSelectionMode(false);
        toolbar.setTitle("Chats");
        setupToolbar(); // 🔥 RESTORES 3-DOTS
    }

    // 🔥 UPDATE TITLE
    private void updateToolbarTitle() {
        if (!selectedPositions.isEmpty()) {
            toolbar.setTitle(selectedPositions.size() + " selected");
        } else if (isSelectionMode) {
            toolbar.setTitle("Select chats");
        } else {
            toolbar.setTitle("Chats");
        }
    }

    // 🔥 SHOW DELETE BUTTON (replaces 3-dots)
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

    // 🔥 DELETE SELECTED CHATS
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
                    // Cancel - go back to selection
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

    // 🔥 YOUR ORIGINAL METHODS (unchanged)
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

    private void loadChats() {
        db.collection("chats")
                .whereArrayContains("users", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("InboxFragment", "❌ Error: " + error.getMessage());
                        return;
                    }

                    if (getActivity() == null) return;

                    chatsList.clear();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        Map<String, Chat> uniqueByPair = new HashMap<>();

                        for (var doc : snapshot.getDocuments()) {
                            Chat chat = doc.toObject(Chat.class);
                            if (chat == null || chat.users == null || chat.users.size() < 2) continue;

                            if (chat.deletedBy != null && chat.deletedBy.contains(currentUserId)) {
                                continue;
                            }

                            chat.chatId = doc.getId();
                            String u1 = chat.users.get(0);
                            String u2 = chat.users.get(1);
                            String key = u1.compareTo(u2) < 0 ? (u1 + "_" + u2) : (u2 + "_" + u1);

                            if (!uniqueByPair.containsKey(key)) {
                                chat.otherUserId = u1.equals(currentUserId) ? u2 : u1;
                                uniqueByPair.put(key, chat);
                                checkUnreadCount(chat);
                            }
                        }
                        chatsList.addAll(uniqueByPair.values());
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void checkUnreadCount(Chat chat) {
        if (chat == null || chat.chatId == null || getContext() == null) return;

        db.collection("chats").document(chat.chatId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mainHandler.post(() -> {
                        if (getActivity() != null && chat != null && adapter != null) {
                            chat.unreadCount = querySnapshot.size();
                            adapter.notifyDataSetChanged();
                        }
                    });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter = null;
        }
        recyclerView = null;
        toolbar = null;
    }
}
