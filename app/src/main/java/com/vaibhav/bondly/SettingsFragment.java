package com.vaibhav.bondly;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass for Settings screen with Matches button.
 */
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Find and setup Matches button
        Button matchesButton = view.findViewById(R.id.btn_matches);
        matchesButton.setOnClickListener(v -> navigateToMatches());

        return view;
    }

    private void navigateToMatches() {
        // Navigate to MatchesFragment
        MatchesFragment matchesFragment = new MatchesFragment();
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, matchesFragment) // Replace with your actual container ID
                .addToBackStack("matches")
                .commit();
    }
}
