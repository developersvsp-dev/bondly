package com.vaibhav.bondly;

import com.google.firebase.auth.FirebaseAuth;

public class UserManager {
    public static String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }
}
