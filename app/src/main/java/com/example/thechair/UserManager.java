package com.example.thechair;

import android.graphics.Bitmap;

public class UserManager {
    private static UserManager instance;
    private appUsers currentUser;
    private Bitmap profileBitmap; // cache the loaded profile image

    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void setUser(appUsers user) {
        this.currentUser = user;
    }

    public appUsers getUser() {
        return currentUser;
    }

    public void setProfileBitmap(Bitmap bitmap) {
        this.profileBitmap = bitmap;
    }

    public Bitmap getProfileBitmap() {
        return profileBitmap;
    }
}
