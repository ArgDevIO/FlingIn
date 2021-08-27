package argdev.io.flingin.java.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.zip.DeflaterOutputStream;

public class Challenge {

    private String to;
    private String from;
    private String deadline;
    private Double latitude;
    private Double longitude;
    private String type;

    public Challenge() {
    }

    public Challenge(String to, String from, String deadline, Double latitude, Double longitude, String type) {
        this.to = to;
        this.from = from;
        this.deadline = deadline;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
