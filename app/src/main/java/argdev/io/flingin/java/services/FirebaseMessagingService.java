package argdev.io.flingin.java.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.RemoteMessage;

import argdev.io.flingin.R;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String notification_title = remoteMessage.getNotification().getTitle();
        String notification_msg = remoteMessage.getNotification().getBody();
        String click_action = remoteMessage.getNotification().getClickAction();
        String user_id = remoteMessage.getData().get("userID");

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(click_action);
        intent.putExtra("USER_ID", user_id);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.channel_id_friend_requests))
                .setSmallIcon(R.drawable.ic_friend_request)
                .setContentTitle(notification_title)
                .setContentText(notification_msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)// Set the intent that will fire when the user taps the notification
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify((int)System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        Log.d("FCM", "Refreshed token: " + s);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(s);
    }

    private void sendRegistrationToServer(String s) {
        if (FirebaseAuth.getInstance().getUid() != null) {
            String firebase_user_uid = FirebaseAuth.getInstance().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child(firebase_user_uid);

            ref.child("device_token").setValue(s).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getApplicationContext(), "FCM:: New Token Generated -> ", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
