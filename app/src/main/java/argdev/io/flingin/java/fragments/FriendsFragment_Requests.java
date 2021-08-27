package argdev.io.flingin.java.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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
import argdev.io.flingin.java.activities.ProfileActivity;
import argdev.io.flingin.java.models.FriendRequest;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment_Requests extends Fragment {

    private View mMainView;

    //Recycler
    private RecyclerView mRequestsList;
    private FirebaseRecyclerAdapter mRequestsAdapter;

    private DatabaseReference mFriendDatabase;
    private DatabaseReference mFriendsRequestsRef;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mRootRef;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;

    public FriendsFragment_Requests() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_friends_requests, container, false);

        mRequestsList = mMainView.findViewById(R.id.fragment_requests_list);
        mRequestsList.setHasFixedSize(true);
        mRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("friends");
        mFriendsRequestsRef = FirebaseDatabase.getInstance().getReference().child("friend_req");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");

        fetch();
        mRequestsAdapter.startListening();

        return mMainView;
    }

    @Override
    public void onDestroy() {
        mRequestsAdapter.stopListening();
        super.onDestroy();
    }

    private void fetch() {
        Query query = mFriendsRequestsRef.child(mCurrent_user_id)
                .orderByChild("request_type").equalTo("received");

        FirebaseRecyclerOptions<FriendRequest> options =
                new FirebaseRecyclerOptions.Builder<FriendRequest>()
                        .setQuery(query, snapshot -> new FriendRequest(snapshot.child("request_type").getValue().toString()))
                        .build();

        mRequestsAdapter = new FirebaseRecyclerAdapter<FriendRequest, FriendRequestsViewHolder>(options) {

            @NonNull
            @Override
            public FriendRequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.friend_req_item_layout, parent, false);

                return new FriendRequestsViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FriendRequestsViewHolder holder, int position, @NonNull FriendRequest model) {
                final String user_id = getRef(position).getKey();

                mUsersDatabase.child(user_id).child("profile_info").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String username = dataSnapshot.child("username").getValue().toString();
                        String imageUri = dataSnapshot.child("image_url").getValue().toString();

                        holder.setmUsernameView(username);
                        holder.setmImageView(imageUri);

                        mFriendDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                List<String> userFriends = new ArrayList<>();
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    userFriends.add(data.getKey());
                                }

                                holder.mFriendsView.setText(userFriends.size() + " Friend");

                                mFriendDatabase.child(mCurrent_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        List<String> currentUserFriends = new ArrayList<>();
                                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                                            currentUserFriends.add(data.getKey());
                                        }

                                        int mutualFriendsCounter;

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

                                        holder.mMutualFriendsView.setText(mutualFriendsCounter + " Mutual");
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

                        holder.mRequestAcceptBtn.setOnClickListener(view -> {
                            final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                            Map friendsMap = new HashMap<>();
                            friendsMap.put("friends/" + mCurrent_user_id + "/" + user_id + "/date", currentDate);
                            friendsMap.put("friends/" + user_id + "/" + mCurrent_user_id + "/date", currentDate);

                            friendsMap.put("friend_req/" + mCurrent_user_id + "/" + user_id, null);
                            friendsMap.put("friend_req/" + user_id + "/" + mCurrent_user_id, null);

                            mRootRef.updateChildren(friendsMap, (databaseError, databaseReference) -> {
                                if (databaseError != null) {
                                    Toast.makeText(getContext(), "::Failed Making Friend::\n" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        });

                        holder.mRequestDeclineBtn.setOnClickListener(view -> {
                            Map friendsMap = new HashMap<>();
                            friendsMap.put("friend_req/" + mCurrent_user_id + "/" + user_id, null);
                            friendsMap.put("friend_req/" + user_id + "/" + mCurrent_user_id, null);

                            mRootRef.updateChildren(friendsMap, (databaseError, databaseReference) -> {
                                if (databaseError != null) {
                                    Toast.makeText(getContext(), "::Failed Making Friend::\n" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        };
        mRequestsList.setAdapter(mRequestsAdapter);
    }

    public static class FriendRequestsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        CircleImageView mImageView;
        TextView mUsernameView;
        TextView mFriendsView;
        TextView mMutualFriendsView;

        MaterialCardView mRequestAcceptBtn;
        MaterialCardView mRequestDeclineBtn;

        public FriendRequestsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;

            mImageView = itemView.findViewById(R.id.friend_req_item_image);
            mUsernameView = itemView.findViewById(R.id.friend_req_item_name);
            mFriendsView = itemView.findViewById(R.id.friends_req_friends_counter);
            mMutualFriendsView = itemView.findViewById(R.id.friends_req_mutual_counter);

            mRequestAcceptBtn = itemView.findViewById(R.id.friend_req_accept);
            mRequestDeclineBtn = itemView.findViewById(R.id.friend_req_decline);
        }

        public void setmImageView(final String imageUri) {
            Picasso.get().load(imageUri).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_profile)
                    .into(mImageView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(imageUri).placeholder(R.drawable.default_profile)
                                    .into(mImageView);
                        }
                    });
        }

        public void setmUsernameView(String name) {
            mUsernameView.setText(name);
        }

        public void setmFriendsView(String friendsCounter) {
            mFriendsView.setText(friendsCounter);
        }

        public void setmMutualFriendsView(String mutualCounter) {
            mMutualFriendsView.setText(mutualCounter);
        }
    }

}
