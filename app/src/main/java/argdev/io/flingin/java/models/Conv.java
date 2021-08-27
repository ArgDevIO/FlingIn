package argdev.io.flingin.java.models;

import androidx.annotation.NonNull;

public class Conv {

    public boolean seen;
    public long unread;
    public long timestamp;

    public Conv() {
    }

    public Conv(boolean seen, long unread, long timestamp) {
        this.seen = seen;
        this.unread = unread;
        this.timestamp = timestamp;
    }

    public long getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return seen + " :: " + timestamp;
    }
}
