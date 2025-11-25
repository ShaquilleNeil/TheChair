// Shaq’s Notes:
// This class is a simple singleton used to store the currently logged-in user
// and (optionally) cache their profile image in memory.
//
// It works as a central access point for user data across activities/fragments,
// without repeatedly fetching info from Firestore. The cached Bitmap helps avoid
// re-downloading the profile picture multiple times.

package com.example.thechair.Adapters;

import android.graphics.Bitmap;

public class UserManager {

    private static UserManager instance;   // singleton instance
    private appUsers currentUser;          // logged-in user object
    private Bitmap profileBitmap;          // in-memory profile photo cache

    // Private constructor to enforce singleton pattern
    private UserManager() {}

    // Access the single shared instance
    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // Store the active user model
    public void setUser(appUsers user) {
        this.currentUser = user;
    }

    // Retrieve the active user model
    public appUsers getUser() {
        return currentUser;
    }

    // Store bitmap cache of profile picture
    public void setProfileBitmap(Bitmap bitmap) {
        this.profileBitmap = bitmap;
    }

    // Retrieve cached profile picture
    public Bitmap getProfileBitmap() {
        return profileBitmap;
    }
}
