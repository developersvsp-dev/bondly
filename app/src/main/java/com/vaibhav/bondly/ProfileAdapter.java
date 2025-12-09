package com.vaibhav.bondly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {
    private List<Profile> profiles;
    private OnProfileClickListener profileListener;
    private OnLikeClickListener likeListener;  // 🔥 NEW

    public interface OnProfileClickListener {
        void onProfileClick(Profile profile);
    }

    public interface OnLikeClickListener {  // 🔥 NEW
        void onLikeClick(String targetUid, boolean isLike);
    }

    public ProfileAdapter(List<Profile> profiles, OnProfileClickListener profileListener, OnLikeClickListener likeListener) {
        this.profiles = profiles;
        this.profileListener = profileListener;
        this.likeListener = likeListener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        Profile profile = profiles.get(position);
        holder.bind(profile);
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    class ProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvPhone, tvGender, tvBio;
        ImageButton ibLike;  // 🔥 NEW

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvBio = itemView.findViewById(R.id.tvBio);
            ibLike = itemView.findViewById(R.id.ibLike);  // 🔥 NEW
        }

        void bind(Profile profile) {
            tvName.setText(profile.getName());
            tvGender.setText(profile.getGender() != null ? profile.getGender() : "Not set");
            tvBio.setText(profile.getBio() != null ? profile.getBio() : "No bio");
            tvBio.setVisibility(View.VISIBLE);

            // Photo loading
            if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
                Glide.with(ivProfile).load(profile.getPhotoUrl()).circleCrop().into(ivProfile);
            } else {
                ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // 🔥 LIKE BUTTON
            ibLike.setImageResource(
                    profile.isLikedByMe() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
            );
            ibLike.setOnClickListener(v -> {
                boolean willLike = !profile.isLikedByMe();
                if (likeListener != null) {
                    likeListener.onLikeClick(profile.getUid(), willLike);
                }
                profile.setLikedByMe(willLike);  // Update immediately
                ibLike.setImageResource(willLike ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            });

            // Profile click
            itemView.setOnClickListener(v -> {
                if (profileListener != null) {
                    profileListener.onProfileClick(profile);
                }
            });
        }
    }
}
