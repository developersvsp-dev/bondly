package com.vaibhav.bondly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileItemFragment extends Fragment {
    private static final String ARG_PROFILE = "profile";
    private Profile profile;
    private FeedFragment feedFragment;

    public static ProfileItemFragment newInstance(Profile profile) {
        ProfileItemFragment fragment = new ProfileItemFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, profile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            profile = getArguments().getParcelable(ARG_PROFILE);
        }
        feedFragment = (FeedFragment) getParentFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_profile_fullscreen, container, false);

        ShapeableImageView ivProfile = view.findViewById(R.id.ivProfile);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvGender = view.findViewById(R.id.tvGender);
        TextView tvBio = view.findViewById(R.id.tvBio);
        ImageButton ibLike = view.findViewById(R.id.ibLike);
        ImageButton ibMessage = view.findViewById(R.id.ibMessage);

        // 🔥 PERFECT BINDING (unchanged)
        tvName.setText(profile.getName());
        tvGender.setText(profile.getGender() != null ? "(" + profile.getGender() + ")" : "");
        tvBio.setText(profile.getBio() != null ? profile.getBio() : "No bio");

        if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
            Glide.with(ivProfile).load(profile.getPhotoUrl()).circleCrop().into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_person);
        }

        ibLike.setImageResource(profile.isLikedByMe() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        ibLike.setOnClickListener(v -> {
            boolean willLike = !profile.isLikedByMe();
            profile.setLikedByMe(willLike);
            ibLike.setImageResource(willLike ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            if (feedFragment != null) {
                feedFragment.handleLike(profile.getUid(), willLike);
            }
        });

        ibMessage.setOnClickListener(v -> {
            if (feedFragment != null) {
                feedFragment.handleMessage(profile.getUid());
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔥 SAFE HEIGHT FIX - No crash + Buttons visible!
        view.post(() -> {
            if (view.getLayoutParams() != null) {
                view.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                view.setLayoutParams(view.getLayoutParams());
            }
            view.setMinimumHeight(700);  // Realme safe minimum
            view.requestLayout();
        });
    }
}
