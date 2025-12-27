package com.vaibhav.bondly;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 🔥 MATCHES BUTTON
        View matchesButton = view.findViewById(R.id.btn_matches);
        matchesButton.setOnClickListener(v -> navigateToMatches());

        // 🔥 PRIVACY POLICY BUTTON (NEW)
        View privacyButton = view.findViewById(R.id.btn_privacy_policy);
        privacyButton.setOnClickListener(v -> openPrivacyPolicy());

        return view;
    }

    private void navigateToMatches() {
        MatchesFragment matchesFragment = new MatchesFragment();
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, matchesFragment)
                .addToBackStack("matches")
                .commit();
    }

    // 🔥 OPEN PRIVACY POLICY IN BROWSER
    private void openPrivacyPolicy() {
        // Replace with your actual privacy policy URL
        String privacyUrl = "https://sites.google.com/view/lifematevsp/home";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
        startActivity(browserIntent);
    }
}
