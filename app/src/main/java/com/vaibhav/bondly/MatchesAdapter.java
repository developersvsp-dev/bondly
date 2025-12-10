package com.vaibhav.bondly;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.UserViewHolder> {
    private ArrayList<User> usersList;
    private Context context;

    public MatchesAdapter(ArrayList<User> usersList, Context context) {
        this.usersList = usersList;
        this.context = context;
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
    }

    @Override
    public int getItemCount() {
        return usersList.size();
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
