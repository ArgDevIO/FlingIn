package argdev.io.flingin.java.models;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class User {

    public ProfileInfo profile_info;

    public User() {

    }

    public User(ProfileInfo profile_info) {
        this.profile_info = profile_info;
    }

    public ProfileInfo getProfile_info() {
        return profile_info;
    }

    public void setProfile_info(ProfileInfo profile_info) {
        this.profile_info = profile_info;
    }

    @NonNull
    @Override
    public String toString() {
        return profile_info.toString();
    }
}


