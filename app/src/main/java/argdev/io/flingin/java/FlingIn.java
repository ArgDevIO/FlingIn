package argdev.io.flingin.java;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import argdev.io.flingin.R;

public class FlingIn extends Application implements LifecycleObserver {

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;

    public static boolean singleToneFetchProfile = false;
    public static boolean inEditingMode = false;
    public static boolean appFirstStart = true;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        enableFirebasePersistence();
        initializePicassoOfflineMode();
        createNotificationChannel();

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid()).child("online");
            updateOnlineFeature();
        }
    }

    private void initializeDB(FirebaseUser currentUser) {
        if (mUserDatabase == null) {
            if (currentUser != null) {
                mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid()).child("online");
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        //App in background
        FirebaseUser currentUser = mAuth.getCurrentUser();
        initializeDB(currentUser);
        if (currentUser != null)
            mUserDatabase.setValue(System.currentTimeMillis());
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroyApp() {
        Log.d("DESTROYED", "onDestroyApp: ");
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        // App in foreground
        if (!appFirstStart && !inEditingMode) singleToneFetchProfile = false;

        appFirstStart = false;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        initializeDB(currentUser);
        if (currentUser != null)
            mUserDatabase.setValue("true");
    }

    public static void setInEditingMode(boolean inEditingMode) {
        FlingIn.inEditingMode = inEditingMode;
    }

    public static boolean getSingleToneFetchProfile() {
        return singleToneFetchProfile;
    }

    public static void setSingleToneFetchProfile(boolean value) {
        singleToneFetchProfile = value;
    }


    private void updateOnlineFeature() {
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    mUserDatabase.onDisconnect().setValue(System.currentTimeMillis());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name_friend_requests);
            String description = getString(R.string.channel_desc_friend_requests);
            String channel_id = getString(R.string.channel_id_friend_requests);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void enableFirebasePersistence() {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    private void initializePicassoOfflineMode() {
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(false);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);
    }
}
