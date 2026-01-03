package com.vaibhav.bondly;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;

public class ProfilePagerAdapter extends FragmentStateAdapter {
    private List<Profile> profiles = new ArrayList<>();

    public ProfilePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Profile profile = profiles.get(position);
        return ProfileItemFragment.newInstance(profile);
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public void setProfiles(List<Profile> newProfiles) {
        this.profiles = newProfiles;
        notifyDataSetChanged();
    }
}
