package argdev.io.flingin.java.models;

public class FriendRequest {

    public String request_type;

    public FriendRequest() {
    }

    public FriendRequest(String request_type) {
        this.request_type = request_type;
    }

    public String getRequest_type() {
        return request_type;
    }

    public void setRequest_type(String request_type) {
        this.request_type = request_type;
    }
}
