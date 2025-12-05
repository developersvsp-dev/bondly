package com.vaibhav.bondly;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;

public class FeedFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        Button premiumBtn = view.findViewById(R.id.btnGoPremium);
        premiumBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PremiumActivity.class));
        });

        return view;
    }
}
