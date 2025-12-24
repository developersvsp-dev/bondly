package com.vaibhav.bondly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {
    private List<Profile> profiles;
    private OnProfileClickListener profileListener;
    private OnLikeClickListener likeListener;
    private OnMessageClickListener messageListener;

    public interface OnProfileClickListener {
        void onProfileClick(Profile profile);
    }

    public interface OnLikeClickListener {
        void onLikeClick(String targetUid, boolean isLike);
    }

    public interface OnMessageClickListener {
        void onMessageClick(String targetUid);
    }

    public ProfileAdapter(List<Profile> profiles, OnProfileClickListener profileListener,
                          OnLikeClickListener likeListener, OnMessageClickListener messageListener) {
        this.profiles = profiles;
        this.profileListener = profileListener;
        this.likeListener = likeListener;
        this.messageListener = messageListener;
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
        ShapeableImageView ivProfile;
        TextView tvName, tvGender, tvBio;
        ImageButton ibLike, ibMessage;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvBio = itemView.findViewById(R.id.tvBio);
            ibLike = itemView.findViewById(R.id.ibLike);
            ibMessage = itemView.findViewById(R.id.ibMessage);
        }

        void bind(Profile profile) {
            tvName.setText(profile.getName());
            tvGender.setText(profile.getGender() != null ? "(" + profile.getGender() + ")" : "");
            tvBio.setText(profile.getBio() != null ? profile.getBio() : "No bio");

            // Photo loading
            if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
                Glide.with(ivProfile).load(profile.getPhotoUrl()).circleCrop().into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.ic_person);
            }

            // 🔥 LIKE BUTTON (Top Right)
            ibLike.setImageResource(
                    profile.isLikedByMe() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
            );
            ibLike.setOnClickListener(v -> {
                boolean willLike = !profile.isLikedByMe();
                if (likeListener != null) {
                    likeListener.onLikeClick(profile.getUid(), willLike);
                }
                profile.setLikedByMe(willLike);
                ibLike.setImageResource(willLike ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            });

            // 🔥 MESSAGE BUTTON (Bottom Right)
            ibMessage.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && messageListener != null) {
                    messageListener.onMessageClick(profiles.get(position).getUid());
                }
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
