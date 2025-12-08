package com.vaibhav.bondly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import android.util.Log;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {
    private List<Profile> profiles;
    private OnProfileClickListener listener;

    public interface OnProfileClickListener {
        void onProfileClick(Profile profile);
    }

    public ProfileAdapter(List<Profile> profiles, OnProfileClickListener listener) {
        this.profiles = profiles;
        this.listener = listener;
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
        TextView tvName, tvPhone, tvGender, tvBio;  // 🔥 ADDED GENDER + BIO

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvGender = itemView.findViewById(R.id.tvGender);  // 🔥 NEW
            tvBio = itemView.findViewById(R.id.tvBio);        // 🔥 NEW
        }

        void bind(Profile profile) {
            tvName.setText(profile.getName());
            //tvPhone.setText(profile.getPhone());
            tvGender.setText(profile.getGender() != null ? profile.getGender() : "Not set");
            tvBio.setText(profile.getBio() != null ? profile.getBio() : "No bio");

            tvBio.setVisibility(View.VISIBLE);  // 🔥 ONE LINE FIX

            // Photo + click (unchanged)
            if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
                Glide.with(ivProfile).load(profile.getPhotoUrl()).circleCrop().into(ivProfile);
            } else {
                ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            itemView.setOnClickListener(v -> listener.onProfileClick(profile));
        }



    }
}
