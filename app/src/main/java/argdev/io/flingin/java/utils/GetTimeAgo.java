package argdev.io.flingin.java.utils;

import android.app.Application;

public class GetTimeAgo extends Application {
    /*
     * ----------- DEVELOPED BY ArgDev -----------
     * ---- WhatsApp Last Seen Time Ago Format ----
     */

    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        String userDateTemp = new java.text.SimpleDateFormat("D:HH:mm").format(new java.util.Date(time));
        String userDate = new java.text.SimpleDateFormat("MMM d, yyyy").format(new java.util.Date(time));
        String userTime = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(time));
        String userDay = new java.text.SimpleDateFormat("E").format(new java.util.Date(time));

        String currentDate = new java.text.SimpleDateFormat("D:HH:mm").format(new java.util.Date(System.currentTimeMillis()));

        if (getUserSingleTime(userDateTemp, "day") == (getUserSingleTime(currentDate, "day"))) {
            return "today at " + userTime;
        } else if ((getUserSingleTime(currentDate, "day") - (getUserSingleTime(userDateTemp, "day"))) == 1) {
            return "yesterday at " + userTime;
        } else if ((getUserSingleTime(currentDate, "day") - (getUserSingleTime(userDateTemp, "day"))) < 7) {
            return userDay + " " + userTime;
        } else if ((getUserSingleTime(currentDate, "day") - (getUserSingleTime(userDateTemp, "day"))) >= 7) {
            return userDate;
        } else return null;

    }

    private static int getUserSingleTime(String date, String type) {
        String[] parts = date.split(":");
        if (type.equals("day"))
            return Integer.parseInt(parts[0]);
        else if (type.equals("hr"))
            return Integer.parseInt(parts[0]);
        else if (type.equals("min"))
            return Integer.parseInt(parts[0]);
        return 0;
    }
}
