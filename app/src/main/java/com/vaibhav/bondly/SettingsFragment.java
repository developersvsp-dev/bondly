package com.vaibhav.bondly;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    private FirebaseFirestore db;
    private String currentUserId;
    private View matchesButton;

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        currentUserId = UserManager.getCurrentUserId();
        db = FirebaseFirestore.getInstance();

        matchesButton = view.findViewById(R.id.btn_matches);
        matchesButton.setOnClickListener(v -> navigateToMatches());

        View privacyButton = view.findViewById(R.id.btn_privacy_policy);
        privacyButton.setOnClickListener(v -> openPrivacyPolicy());

        // ⭐ CHILD SAFETY REPORT BUTTON (MANDATORY FOR DATING APPS)
        FloatingActionButton reportFab = view.findViewById(R.id.reportFab);
        reportFab.setOnClickListener(v -> showReportDialog());

        return view;
    }

    private void navigateToMatches() {
        MatchesFragment matchesFragment = new MatchesFragment();
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, matchesFragment)
                .addToBackStack("matches")
                .commit();

        Log.d("SettingsFragment", "✅ Navigated to Matches");
    }

    private void openPrivacyPolicy() {
        String privacyUrl = "https://sites.google.com/view/lifematevsp/home";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
        startActivity(browserIntent);
    }

    // ⭐ CHILD SAFETY COMPLIANCE - REPORT DIALOG (PLAY STORE APPROVED)
    private void showReportDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Report Safety Issue")
                .setMessage("Contact developersvsp@gmail.com\n\n\nAll reports are confidential.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Copy Email", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Bondly Support", "developersvsp@gmail.com");
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "✅ Email copied to clipboard!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        matchesButton = null;
    }
}
