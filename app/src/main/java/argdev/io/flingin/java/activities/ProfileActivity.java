package argdev.io.flingin.java.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import argdev.io.flingin.R;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mUserame;
    private TextView mStatus;
    private TextView mFriendsCounter;
    private TextView mMutualCounter;

    private Button mProfileSendReqBtn, mDeclineReq;

    private DatabaseReference mUserProfileInfoRef;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;


    private DatabaseReference mRootRef;

    private FirebaseUser mCurrent_User;

    private int mCurrent_state;

    /*
     * ----- FRIEND STATES -----
     * 0 : Not Friend
     * 1 : Request Sent
     * 2 : Request Received
     * 3 : Friend
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("USER_ID");

        mRootRef = FirebaseDatabase.getInstance().getReference();
        //mRootRef.keepSynced(true);
        mUserProfileInfoRef = FirebaseDatabase.getInstance().getReference().child("users").child(user_id).child("profile_info");
        //mUserProfileInfoRef.keepSynced(true);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("friend_req");
        //mFriendReqDatabase.keepSynced(true);
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("friends");
        //mFriendDatabase.keepSynced(true);

        mCurrent_User = FirebaseAuth.getInstance().getCurrentUser();

        mProfileImage = findViewById(R.id.activity_profile_photo);
        mUserame = findViewById(R.id.activity_profile_username);
        mStatus = findViewById(R.id.activity_profile_status);
        mFriendsCounter = findViewById(R.id.activity_profile_friends_counter);
        mMutualCounter = findViewById(R.id.activity_profile_mutual_counter);
        mProfileSendReqBtn = findViewById(R.id.activity_profile_btn_friendRequest);
        mDeclineReq = findViewById(R.id.activity_profile_btn_declineRequest);

        mDeclineReq.setVisibility(View.GONE);

        mCurrent_state = 0;

        setOnClickListeners(user_id);
        setCustomFonts();


        mUserProfileInfoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String username = dataSnapshot.child("username").getValue().toString();
                String imageUri = dataSnapshot.child("image_url").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();

                mUserame.setText(username);
                mStatus.setText(status);
                loadImage(imageUri);

                // ------------ FRIENDS LIST / REQUEST FEATURE ---------------
                mFriendReqDatabase.child(mCurrent_User.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(user_id)) {
                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if (req_type.equals("received")) {

                                mCurrent_state = 2;
                                mProfileSendReqBtn.setText(getResources().getString(R.string.btn_accept_friend));

                                mDeclineReq.setVisibility(View.VISIBLE);

                            } else if (req_type.equals("sent")) {
                                mCurrent_state = 1;
                                mProfileSendReqBtn.setText(getResources().getString(R.string.btn_cancel_friend));

                                mDeclineReq.setVisibility(View.GONE);
                            }
                        } else {
                            mFriendDatabase.child(mCurrent_User.getUid()).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(user_id)) {
                                        mCurrent_state = 3;
                                        mProfileSendReqBtn.setText(getResources().getString(R.string.btn_unfriend));

                                        mDeclineReq.setVisibility(View.GONE);
                                    } else {
                                        mCurrent_state = 0;
                                        mProfileSendReqBtn.setText(getResources().getString(R.string.btn_send_friend));

                                        mDeclineReq.setVisibility(View.GONE);
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        mFriendDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> userFriends = new ArrayList<>();
                for (DataSnapshot data : dataSnapshot.getChildren()){
                    userFriends.add(data.getKey());
                }

                mFriendsCounter.setText(userFriends.size() + " Friend");

                mFriendDatabase.child(mCurrent_User.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<String> currentUserFriends = new ArrayList<>();
                        for (DataSnapshot data : dataSnapshot.getChildren()){
                            currentUserFriends.add(data.getKey());
                        }

                        int mutualFriendsCounter = 0;

                        if (currentUserFriends.size() > userFriends.size()) {
                            List<String> mutual = currentUserFriends.stream()
                                    .filter(userFriends::contains)
                                    .collect(Collectors.toList());

                           mutualFriendsCounter = mutual.size();
                        } else {
                            List<String> mutual = userFriends.stream()
                                    .filter(currentUserFriends::contains)
                                    .collect(Collectors.toList());

                            mutualFriendsCounter = mutual.size();
                        }

                        mMutualCounter.setText(mutualFriendsCounter + " Mutual");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void setOnClickListeners(final String user_id) {

        //region SEND REQUEST BUTTON
        mProfileSendReqBtn.setOnClickListener(view -> {

            mProfileSendReqBtn.setEnabled(false);

            // ---- NOT FRIENDS ----
            if (mCurrent_state == 0) { // 0 NOT FRIENDS

                DatabaseReference newNotificationRef = mRootRef.child("notifications").child(user_id).push();
                String newNotificationID = newNotificationRef.getKey();

                HashMap<String, String> notificationData = new HashMap<>();
                notificationData.put("from", mCurrent_User.getUid());
                notificationData.put("type", "request");

                Map requestMap = new HashMap<>();
                requestMap.put("friend_req/" + mCurrent_User.getUid() + "/" + user_id + "/request_type", "sent");
                requestMap.put("friend_req/" + user_id + "/" + mCurrent_User.getUid() + "/request_type", "received");
                requestMap.put("notifications/" + user_id + "/" + newNotificationID, notificationData);

                mRootRef.updateChildren(requestMap, (databaseError, databaseReference) -> {
                    if (databaseError != null) {
                        Toast.makeText(ProfileActivity.this, "::Failed Sending Request::\n" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        mProfileSendReqBtn.setEnabled(true);
                        mCurrent_state = 1;
                        mProfileSendReqBtn.setText(getResources().getString(R.string.btn_cancel_friend));

                        mDeclineReq.setVisibility(View.GONE);
                    }
                });
            }

            // ---- CANCEL REQUEST ----
            if (mCurrent_state == 1) { // 1 REQUEST SENT

                Map cancelReqMap = new HashMap();
                cancelReqMap.put("friend_req/" + mCurrent_User.getUid() + "/" + user_id, null);
                cancelReqMap.put("friend_req/" + user_id + "/" + mCurrent_User.getUid(), null);

                mRootRef.updateChildren(cancelReqMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Toast.makeText(ProfileActivity.this, "::Failed Canceling Friend Request::\n" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            mProfileSendReqBtn.setEnabled(true);
                            mCurrent_state = 0;
                            mProfileSendReqBtn.setText(getResources().getString(R.string.btn_send_friend));

                            mDeclineReq.setVisibility(View.GONE);
                        }
                    }
                });
            }

            // ---- REQ RECEIVED ----
            if (mCurrent_state == 2) { // REQUEST RECEIVED

                final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                Map friendsMap = new HashMap<>();
                friendsMap.put("friends/" + mCurrent_User.getUid() + "/" + user_id + "/date", currentDate);
                friendsMap.put("friends/" + user_id + "/" + mCurrent_User.getUid() + "/date", currentDate);

                friendsMap.put("friend_req/" + mCurrent_User.getUid() + "/" + user_id, null);
                friendsMap.put("friend_req/" + user_id + "/" + mCurrent_User.getUid(), null);

                mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Toast.makeText(ProfileActivity.this, "::Failed Making Friend::\n" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            mProfileSendReqBtn.setEnabled(true);
                            mCurrent_state = 3;
                            mProfileSendReqBtn.setText(getResources().getString(R.string.btn_unfriend));

                            mDeclineReq.setVisibility(View.GONE);
                        }
                    }
                });
            }

            // ---- UNFRIEND ----
            if (mCurrent_state == 3) { // FRIENDS

                Map unfriendMap = new HashMap();
                unfriendMap.put("friends/" + mCurrent_User.getUid() + "/" + user_id, null);
                unfriendMap.put("friends/" + user_id + "/" + mCurrent_User.getUid(), null);

                mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Toast.makeText(ProfileActivity.this, "::Failed Unfriending::\n" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            mProfileSendReqBtn.setEnabled(true);
                            mCurrent_state = 0;
                            mProfileSendReqBtn.setText(getResources().getString(R.string.btn_send_friend));

                            mDeclineReq.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
        //endregion

        //region DECLINE REQUEST BUTTON
        mDeclineReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map cancelReqMap = new HashMap();
                cancelReqMap.put("friend_req/" + mCurrent_User.getUid() + "/" + user_id, null);
                cancelReqMap.put("friend_req/" + user_id + "/" + mCurrent_User.getUid(), null);

                mRootRef.updateChildren(cancelReqMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Toast.makeText(ProfileActivity.this, "::Failed Declining Friend Request::\n" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            mProfileSendReqBtn.setEnabled(true);
                            mCurrent_state = 0;
                            mProfileSendReqBtn.setText(getResources().getString(R.string.btn_send_friend));

                            mDeclineReq.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
        //endregion
    }

    private void loadImage(final String imageUri) {
        Picasso.get().load(imageUri).networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.default_profile)
                .into(mProfileImage, new Callback() {
                    @Override
                    public void onSuccess() { }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(imageUri).placeholder(R.drawable.default_profile)
                                .into(mProfileImage);
                    }
                });
    }

    private void setCustomFonts() {
        Typeface typeface = Typeface.createFromAsset(this.getAssets(), "fonts/Tumbly.otf");
        mUserame.setTypeface(typeface);
    }
}
