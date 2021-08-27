package argdev.io.flingin.java.models;

import androidx.annotation.NonNull;

public class ProfileInfo {
    public String image_url;
    public String username;
    public String status;

    public ProfileInfo() {

    }

    public ProfileInfo(String image_url, String username, String status) {
        this.image_url = image_url;
        this.username = username;
        this.status = status;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @NonNull
    @Override
    public String toString() {
        return username + " :: " + status + " :: " + image_url;
    }
}