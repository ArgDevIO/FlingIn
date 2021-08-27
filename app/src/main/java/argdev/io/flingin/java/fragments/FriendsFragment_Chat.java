package argdev.io.flingin.java.fragments;


import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.common.collect.Sets;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import argdev.io.flingin.R;
import argdev.io.flingin.java.activities.ChatActivity;
import argdev.io.flingin.java.models.Conv;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment_Chat extends Fragment {

    private RecyclerView mConvList;

    private DatabaseReference mConvDatabase;
    private DatabaseReference mMessageDatabase;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mFriendsDatabase;

    private FirebaseRecyclerAdapter firebaseConvAdapter;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;

    private View mMainView;

    private String chatMsg;
    private String chatType;

    private boolean areFriends;

    private Set<String> friendsOnDB;

    public FriendsFragment_Chat() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        friendsOnDB = new HashSet<>();


        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_friends_chat, container, false);

        mConvList = mMainView.findViewById(R.id.chat_conv_list);
        mAuth = FirebaseAuth.getInstance();

        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mConvDatabase = FirebaseDatabase.getInstance().getReference().child("chat").child(mCurrent_user_id);

        mConvDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        mMessageDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id);
        mUsersDatabase.keepSynced(true);
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("friends");


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mConvList.setHasFixedSize(true);
        mConvList.setLayoutManager(linearLayoutManager);
        ((DefaultItemAnimator) mConvList.getItemAnimator()).setSupportsChangeAnimations(false);

        // Inflate the layout for this fragment
        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        fetch();
        //listenForFriendAddRemove();
        firebaseConvAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (firebaseConvAdapter != null)
            firebaseConvAdapter.stopListening();
    }

    private void listenForFriendAddRemove() {
        mFriendsDatabase.child(mCurrent_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (friendsOnDB.size() > 0) {
                    Set<String> currentFriends = new HashSet<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        currentFriends.add(snapshot.getKey());
                    }

                    Set<String> tempFriends = currentFriends;

                    Set<String> diff;

                    if (friendsOnDB.size() > tempFriends.size()) {
                        diff = Sets.difference(friendsOnDB, tempFriends);
                        String diffUserID = new ArrayList<>(diff).get(0);
                        ConvViewHolder itemView = getViewByID(diffUserID);
                        Log.d("ADDFRIEND", "onDataChange: Deleted :: " + itemView.userNameView.getText());

                        itemView.setUserOnline("false");
                    } else {
                        diff = Sets.difference(tempFriends, friendsOnDB);
                        ArrayList<String> toList = new ArrayList<>(diff);
                        if (toList.size() != 0) {
                            ConvViewHolder itemView = getViewByID(toList.get(0));
                            Log.d("ADDFRIEND", "onDataChange: Added :: " + itemView.userNameView.getText());

                            itemView.setUserOnline("true");
                        }
                    }

                    friendsOnDB = currentFriends;
                } else {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        friendsOnDB.add(snapshot.getKey());
                    }
                    Log.d("ADDFRIEND", "FIRSTTIME " + friendsOnDB.toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private ConvViewHolder getViewByID(String diffUserID) {
        for (int i = 0; i < mConvList.getLayoutManager().getChildCount(); i++) {
            ConvViewHolder viewHolder = (ConvViewHolder) mConvList.findViewHolderForAdapterPosition(i);
            if (viewHolder.getUserUID().equals(diffUserID))
                return viewHolder;
        }
        return null;
    }

    private void fetch() {
        Query convQuery = mConvDatabase.orderByChild("timestamp");

        FirebaseRecyclerOptions<Conv> options =
                new FirebaseRecyclerOptions.Builder<Conv>()
                        .setQuery(convQuery, new SnapshotParser<Conv>() {
                            @NonNull
                            @Override
                            public Conv parseSnapshot(@NonNull DataSnapshot snapshot) {
                                Boolean boolSeen = snapshot.child("seen").getValue(Boolean.class);
                                long unread = (long) snapshot.child("unread").getValue();
                                Long timeStamp = snapshot.child("timestamp").getValue(Long.class);

                                if (boolSeen != null && timeStamp != null)
                                    return new Conv(boolSeen, unread, timeStamp);
                                else return new Conv();
                            }
                        })
                        .build();

        firebaseConvAdapter = new FirebaseRecyclerAdapter<Conv, ConvViewHolder>(options) {

            @NonNull
            @Override
            public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_item_layout, parent, false);

                return new ConvViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final ConvViewHolder holder, int position, @NonNull final Conv model) {
                final String list_user_id = getRef(position).getKey();
                final long itemID = getItemId(position);

                Query lastMessageQuery = mMessageDatabase.child(list_user_id).limitToLast(1);

                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        chatMsg = dataSnapshot.child("message").getValue().toString();
                        chatType = dataSnapshot.child("type").getValue().toString();
                        Log.d("SNAPSHOT", "onChildAdded: " + model);
                        holder.setMessage(chatMsg, chatType, model.getUnread(), model.isSeen());
                        holder.setTime(model.getTimestamp());

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("profile_info").child("username").getValue().toString();
                        final String imageUri = dataSnapshot.child("profile_info").child("image_url").getValue().toString();
                        final String userOnline = dataSnapshot.child("online").getValue().toString();


                        holder.setUserUID(list_user_id);
                        //holder.setUserOnline(userOnline);
                        holder.setName(userName);
                        holder.setUserImage(imageUri);
                        holder.mView.setOnClickListener(view -> {

                            holder.userOnlineView.setVisibility(View.GONE);

                            View profileImgShared = holder.userImageView;
                            View profileNameShared = holder.userNameView;
                            String imgTransition = "profile_picture";
                            String nameTransition = "profile_name";

                            ActivityOptions transitionOptions = ActivityOptions.makeSceneTransitionAnimation(
                                    getActivity(),
                                    Pair.create(profileImgShared, imgTransition),
                                    Pair.create(profileNameShared, nameTransition));

                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                            chatIntent.putExtra("USER_ID", list_user_id);
                            chatIntent.putExtra("USER_NAME", userName);
                            chatIntent.putExtra("USER_IMAGE", imageUri);

                            startActivity(chatIntent, transitionOptions.toBundle());
                        });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

            }
        };

        mConvList.setAdapter(firebaseConvAdapter);
    }

    public class ConvViewHolder extends RecyclerView.ViewHolder {

        String userUID;
        View mView;
        TextView userNameView;
        TextView userLastMsgView;
        ImageView userOnlineView;
        final CircleImageView userImageView;

        TextView chatTimeView;
        TextView chatUnreadTxt;
        ConstraintLayout chatUnreadMsgView;
        ImageView chatPhotoIc;


        public ConvViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            userNameView = mView.findViewById(R.id.chat_item_name);
            userLastMsgView = mView.findViewById(R.id.chat_item_message);
            userImageView = mView.findViewById(R.id.chat_item_image);
            userOnlineView = mView.findViewById(R.id.chat_item_online_ic);

            chatUnreadMsgView = mView.findViewById(R.id.chat_item_unread_msg_counter);
            chatUnreadMsgView.setVisibility(View.INVISIBLE);
            chatUnreadTxt = mView.findViewById(R.id.chat_item_unread_msg_txt);
            chatUnreadTxt.setVisibility(View.INVISIBLE);
            chatTimeView = mView.findViewById(R.id.chat_item_time);
            chatPhotoIc = mView.findViewById(R.id.chat_item_message_photo_ic);
            chatPhotoIc.setVisibility(View.GONE);
        }

        public String getUserUID() {
            return userUID;
        }

        public void setUserUID(String userUID) {
            this.userUID = userUID;
        }

        public void setMessage(String message, String type, long unread, boolean isSeen) {

            if (type.equals("text")) {
                chatPhotoIc.setVisibility(View.GONE);
                userLastMsgView.setText(message);
            }
            else {
                chatPhotoIc.setVisibility(View.VISIBLE);
                userLastMsgView.setText("      Photo");
            }
            if (!isSeen) {
                if (unread != 0) {
                    chatUnreadTxt.setText(String.valueOf(unread));
                    chatUnreadTxt.setVisibility(View.VISIBLE);
                    chatTimeView.setTextColor(ContextCompat.getColor(getContext(), R.color.lightRedOchre4));
                    chatUnreadMsgView.setVisibility(View.VISIBLE);
                }
            } else {
                chatTimeView.setTextColor(getResources().getColor(R.color.lightCyan3));
                chatUnreadMsgView.setVisibility(View.INVISIBLE);
            }
        }

        public void setTime(long timestamp) {
            String chatTime = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(timestamp));
            chatTimeView.setText(chatTime);
        }

        public void setName(String name) {
            userNameView.setText(name);
        }

        public void setUserImage(final String imageUri) {
            Picasso.get().load(imageUri).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_profile)
                    .into(userImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(imageUri).placeholder(R.drawable.default_profile)
                                    .into(userImageView);
                        }
                    });

        }

        public void setUserOnline(String online_status) {
            if (online_status.equals("true")) {
                userOnlineView.setVisibility(View.VISIBLE);
            } else {
                userOnlineView.setVisibility(View.INVISIBLE);
            }
        }
    }

}
