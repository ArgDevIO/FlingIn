package argdev.io.flingin.java.fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import argdev.io.flingin.R;
import argdev.io.flingin.java.activities.ChatActivity;
import argdev.io.flingin.java.activities.ProfileActivity;
import argdev.io.flingin.java.activities.UsersActivity;
import argdev.io.flingin.java.models.Friend;
import argdev.io.flingin.java.models.ProfileInfo;
import argdev.io.flingin.java.models.User;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment_Friends extends Fragment {

    private View mMainView;

    private FloatingActionButton addFriend;
    private FloatingActionButton backBtn;

    private RelativeLayout navBar;

    private EditText mSearchField;
    private RecyclerView mResultList;
    private FirebaseRecyclerAdapter searchFriendsAdapter;

    //Recycler
    private RecyclerView mFriendsList;
    private FirebaseRecyclerAdapter myFriendsAdapter;

    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUsersDatabase;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;
    private int prevExpandedPosition = -1;
    private int mExpandedPosition = -1;


    public FriendsFragment_Friends() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_friends_friends, container, false);

        navBar = mMainView.findViewById(R.id.add_friends_search_nav);

        mSearchField = mMainView.findViewById(R.id.friends_add_search);
        mResultList = mMainView.findViewById(R.id.fragment_friends_addFriends_list);
        mResultList.setHasFixedSize(true);
        mResultList.setLayoutManager(new LinearLayoutManager(getContext()));

        addFriend = mMainView.findViewById(R.id.friends_addFriends_btn);
        backBtn = mMainView.findViewById(R.id.friends_goBack_btn);

        backBtn.hide();


        //Recycler
        mFriendsList = mMainView.findViewById(R.id.fragment_friends_list);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("friends").child(mCurrent_user_id);
        mFriendsDatabase.keepSynced(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getActivity()));


        fetch();
        myFriendsAdapter.startListening();

        addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (prevExpandedPosition >= 0) {
                    final FriendsViewHolder prevHolder = (FriendsViewHolder)mFriendsList.findViewHolderForAdapterPosition(prevExpandedPosition);

                    Log.d("PREVHOLDER", "onClick: " + prevHolder.getName());

                    prevHolder.mSendMsg.setVisibility(View.GONE);
                    prevHolder.mViewProf.setVisibility(View.GONE);
                    AnimatorSet set = slideAnimation(prevHolder.mWrapper, false);
                    set.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            prevHolder.mOptionsView.setAlpha(0.0f);
                        }
                    });
                    set.start();
                }


                addFriend.hide();
                backBtn.show();
                navBar.animate().translationY(0);
                mFriendsList.animate().translationY(0);
                mFriendsList.animate().alpha(0.0f);
                mFriendsList.setVisibility(View.GONE);

                mResultList.setVisibility(View.VISIBLE);
                mResultList.animate().alpha(1.0f);
                mSearchField.getText().clear();
                mSearchField.requestFocus();
                mSearchField.postDelayed(() -> {
                    InputMethodManager keyboard = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.showSoftInput(mSearchField, 0);
                },200);
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                addFriend.show();
                backBtn.hide();

                float density = getResources().getDisplayMetrics().density;
                float pixels = 50 * density;


                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                //Find the currently focused view, so we can grab the correct window token from it.
                View view1 = getActivity().getCurrentFocus();
                //If no view currently has focus, create a new one, just so we can grab a window token from it
                if (view1 == null) {
                    view1 = new View(getActivity());
                }
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                navBar.animate().translationY(-pixels);
                mFriendsList.setVisibility(View.VISIBLE);
                mFriendsList.animate().translationY(-pixels);
                mFriendsList.animate().alpha(1.0f);

                mResultList.animate().alpha(0.0f);
                mResultList.setVisibility(View.GONE);
                mSearchField.getText().clear();
                mSearchField.clearFocus();
            }
        });


        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    firebaseUserSearch(charSequence.toString());
                    searchFriendsAdapter.startListening();
                } else {
                    if (searchFriendsAdapter != null)
                        searchFriendsAdapter.stopListening();
                    mResultList.removeAllViews();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return mMainView;
    }

    private void firebaseUserSearch(String searchText) {

        Query firebaseSearchQuery = mUsersDatabase.orderByChild("profile_info/username").startAt(searchText).endAt(searchText + "\uf8ff");

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(firebaseSearchQuery, new SnapshotParser<User>() {
                        @NonNull
                        @Override
                        public User parseSnapshot(@NonNull DataSnapshot snapshot) {
                            User tmpUser = new User(snapshot.child("profile_info").getValue(ProfileInfo.class));
                            return tmpUser;
                        }
                    })
                    .build();

        searchFriendsAdapter = new FirebaseRecyclerAdapter<User, UsersActivity.UsersViewHolder>(options) {

            @Override
            protected void onBindViewHolder(@NonNull UsersActivity.UsersViewHolder holder, int position, @NonNull final User model) {
                Log.d("MODELUSER", "onBindViewHolder: " + model.getProfile_info());
                String username = model.getProfile_info().getUsername();
                String image = model.getProfile_info().getImage_url();
                String status = model.getProfile_info().getStatus();

                holder.setmUsername(username);
                holder.setmImage(image, getContext());
                holder.setmStatus(status);

                final String user_id = getRef(position).getKey();

                holder.getmView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), ProfileActivity.class);
                        intent.putExtra("USER_ID", user_id);
                        startActivity(intent);
                    }
                });

            }

            @NonNull
            @Override
            public UsersActivity.UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.all_users_single_layout, parent, false);

                return new UsersActivity.UsersViewHolder(view);
            }
        };
        mResultList.setAdapter(searchFriendsAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        myFriendsAdapter.startListening();
        if (searchFriendsAdapter != null)
            searchFriendsAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        myFriendsAdapter.stopListening();
        if (searchFriendsAdapter != null)
            searchFriendsAdapter.stopListening();
    }

    private void fetch() {
        Query query = mFriendsDatabase;

        FirebaseRecyclerOptions<Friend> options =
                new FirebaseRecyclerOptions.Builder<Friend>()
                        .setQuery(query, snapshot -> new Friend(snapshot.child("date").getValue().toString()))
                        .build();

        myFriendsAdapter = new FirebaseRecyclerAdapter<Friend, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, final int position, @NonNull final Friend model) {

                holder.onlineIcon.setVisibility(View.VISIBLE);
                final String user_id = getRef(position).getKey();

                mUsersDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("profile_info").child("username").getValue().toString();
                        final String status = dataSnapshot.child("profile_info").child("status").getValue().toString();
                        final String userImage = dataSnapshot.child("profile_info").child("image_url").getValue().toString();

                        if (dataSnapshot.hasChild("online")) {
                            String userOnline = dataSnapshot.child("online").getValue().toString();
                            holder.setUserOnline(userOnline);
                        }

                        holder.setName(userName);
                        holder.setStatus(status);
                        holder.setImage(userImage, getContext());

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                if (position == prevExpandedPosition || prevExpandedPosition < 0) {
                                    Log.d("PREVHOLDER", "onClick: same position");
                                    if (holder.mOptionsView.getAlpha() == 0.0f) {
                                        // make it visible

                                        holder.mSendMsg.setVisibility(View.VISIBLE);
                                        holder.mViewProf.setVisibility(View.VISIBLE);

                                        holder.mOptionsView.setAlpha(1.0f);
                                        slideAnimation(holder.mWrapper, true).start();

                                        prevExpandedPosition = position;

                                    } else if (holder.mOptionsView.getAlpha() == 1.0f){
                                        // make it gone
                                        holder.mSendMsg.setVisibility(View.GONE);
                                        holder.mViewProf.setVisibility(View.GONE);

                                        AnimatorSet set = slideAnimation(holder.mWrapper, false);
                                        set.addListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                holder.mOptionsView.setAlpha(0.0f);
                                            }
                                        });
                                        set.start();
                                        prevExpandedPosition = -1;
                                    }
                                } else {
                                    final FriendsViewHolder prevHolder = (FriendsViewHolder)mFriendsList.findViewHolderForAdapterPosition(prevExpandedPosition);

                                    Log.d("PREVHOLDER", "onClick: " + prevHolder.getName());

                                    prevHolder.mSendMsg.setVisibility(View.GONE);
                                    prevHolder.mViewProf.setVisibility(View.GONE);
                                    AnimatorSet set = slideAnimation(prevHolder.mWrapper, false);
                                    set.addListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            prevHolder.mOptionsView.setAlpha(0.0f);
                                        }
                                    });
                                    set.start();

                                    if (holder.mOptionsView.getAlpha() == 0.0f) {
                                        // make it visible

                                        holder.mSendMsg.setVisibility(View.VISIBLE);
                                        holder.mViewProf.setVisibility(View.VISIBLE);

                                        holder.mOptionsView.setAlpha(1.0f);
                                        slideAnimation(holder.mWrapper, true).start();

                                        prevExpandedPosition = position;

                                    } else if (holder.mOptionsView.getAlpha() == 1.0f){
                                        // make it gone
                                        holder.mSendMsg.setVisibility(View.GONE);
                                        holder.mViewProf.setVisibility(View.GONE);

                                        AnimatorSet set1 = slideAnimation(holder.mWrapper, false);
                                        set1.addListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                holder.mOptionsView.setAlpha(0.0f);
                                            }
                                        });
                                        set1.start();
                                        prevExpandedPosition = -1;
                                    }
                                }

                            }
                        });

                        holder.mSendMsg.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                holder.onlineIcon.setVisibility(View.GONE);
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);

                                View profileImgShared = holder.mImage;
                                View profileNameShared = holder.userNameView;
                                String imgTransition = "profile_picture";
                                String nameTransition = "profile_name";

                                ActivityOptions transitionOptions = ActivityOptions.makeSceneTransitionAnimation(
                                        getActivity(),
                                        Pair.create(profileImgShared, imgTransition),
                                        Pair.create(profileNameShared, nameTransition));

                                chatIntent.putExtra("USER_ID", user_id);
                                chatIntent.putExtra("USER_NAME", userName);
                                chatIntent.putExtra("USER_IMAGE", userImage);
                                startActivity(chatIntent, transitionOptions.toBundle());
                            }
                        });

                        holder.mViewProf.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                holder.onlineIcon.setVisibility(View.GONE);

                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);

                                View profileImgShared = holder.mImage;
                                View profileNameShared = holder.userNameView;
                                View profileStatusShared = holder.userStatusView;
                                String imgTransition = "profile_picture";
                                String nameTransition = "profile_name";
                                String statusTransition = "profile_status";

                                ActivityOptions transitionOptions = ActivityOptions.makeSceneTransitionAnimation(
                                        getActivity(),
                                        Pair.create(profileImgShared, imgTransition),
                                        Pair.create(profileNameShared, nameTransition),
                                        Pair.create(profileStatusShared, statusTransition));

                                profileIntent.putExtra("USER_ID", user_id);
                                startActivity(profileIntent, transitionOptions.toBundle());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_single_layout, parent, false);


                return new FriendsViewHolder(view);
            }
        };
        mFriendsList.setAdapter(myFriendsAdapter);
    }

    private AnimatorSet slideAnimation(final RelativeLayout wrapper, boolean expand) {
        int currentHeight = wrapper.getHeight();
        Log.d("HEIGHTT", "slideAnimation: " + currentHeight);
        int newHeight = (expand) ? 380 : 234;

        final ValueAnimator slideAnim = ValueAnimator
                .ofInt(currentHeight, newHeight)
                .setDuration(300);

        slideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Integer value = (Integer) valueAnimator.getAnimatedValue();

                wrapper.getLayoutParams().height = value;

                wrapper.requestLayout();
            }
        });

        final AnimatorSet set = new AnimatorSet();
        set.play(slideAnim);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        return set;
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        LinearLayout mOptionsView;
        MaterialCardView mSendMsg;
        MaterialCardView mViewProf;

        TextView userStatusView;
        TextView userNameView;

        RelativeLayout mWrapper;

        CircleImageView mImage;

        ImageView onlineIcon;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            mOptionsView = itemView.findViewById(R.id.user_single_options);
            mSendMsg = itemView.findViewById(R.id.user_single_sendMsgBtn);
            mViewProf = itemView.findViewById(R.id.user_single_viewProfBtn);
            mWrapper = itemView.findViewById(R.id.users_single_wrapper);

            userStatusView = mView.findViewById(R.id.user_single_status);
            userNameView = mView.findViewById(R.id.user_single_name);

            mImage = mView.findViewById(R.id.users_single_image);

            onlineIcon = mView.findViewById(R.id.user_single_online_ic);
        }

        public void setStatus(String status) {
            userStatusView.setText(status);
        }

        public void setName(String name) {
            userNameView.setText(name);
        }

        public String getName() {
            TextView userNameView = mView.findViewById(R.id.user_single_name);
            return userNameView.getText().toString();
        }

        public void setImage(final String imageUri, Context context) {


            Picasso.get().load(imageUri).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_profile)
                    .into(mImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(imageUri).placeholder(R.drawable.default_profile)
                                    .into(mImage);
                        }
                    });
        }

        public void setUserOnline(String online) {


            if (online.equals("true"))
                onlineIcon.setVisibility(View.VISIBLE);
            else
                onlineIcon.setVisibility(View.INVISIBLE);
        }
    }

}
