package argdev.io.flingin.java.fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.Distance;
import com.google.maps.model.LatLng;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.time.Period;

import argdev.io.flingin.R;
import argdev.io.flingin.java.activities.MapsActivity;
import argdev.io.flingin.java.models.Challenge;
import argdev.io.flingin.java.services.GPS_Service;
import argdev.io.flingin.java.utils.GoogleMapsDistanceCallback;
import argdev.io.flingin.java.utils.GoogleMapsUtils;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChallengesFragment_Received extends Fragment {

    public static final String TAG = "ChallengesFragment_Received_Debug";

    //Recycler
    private RecyclerView mChallengesReceivedList;
    private FirebaseRecyclerAdapter mChallengesReceivedAdapter;

    private DatabaseReference mChallengesDatabase;
    private DatabaseReference mUsersDatabase;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;
    
    private View mView;

    private GoogleMapsUtils googleMapsUtils;

    //SwipeRefreshRecyclerView
    private SwipeRefreshLayout mSwipeRefresh;

    public ChallengesFragment_Received() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_challenges_received, container, false);

        googleMapsUtils = new GoogleMapsUtils(getActivity());
        googleMapsUtils.getLocationPermission();

        //Recycler
        mChallengesReceivedList = mView.findViewById(R.id.challenges_received_list);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mChallengesDatabase = FirebaseDatabase.getInstance().getReference().child("challenges").child("received").child(mCurrent_user_id);
        mChallengesDatabase.keepSynced(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");

        mChallengesReceivedList.setHasFixedSize(true);
        mChallengesReceivedList.setLayoutManager(new LinearLayoutManager(getActivity()));

        mSwipeRefresh = mView.findViewById(R.id.challenges_received_swipeRefreshList);
        mSwipeRefresh.setOnRefreshListener(() -> new Handler().postDelayed(() -> {
            fetch();
            mChallengesReceivedAdapter.startListening();
        }, 1000));
        mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getContext(), R.color.lightRedOchre4));

        fetch();
        mChallengesReceivedAdapter.startListening();
        
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mChallengesReceivedAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mChallengesReceivedAdapter.stopListening();
    }

    private void fetch() {

        googleMapsUtils.getDeviceLocation();
        Query query = mChallengesDatabase.orderByChild("deadline");

        FirebaseRecyclerOptions<Challenge> options =
                new FirebaseRecyclerOptions.Builder<Challenge>()
                        .setQuery(query, snapshot -> {
                            Log.d("CHALLreceived", "parseSnapshot: " + snapshot.getValue().toString());
                            return new Challenge(
                                    "",
                                    snapshot.child("from").getValue().toString(),
                                    snapshot.child("deadline").getValue().toString(),
                                    snapshot.child("latitude").getValue(Double.class),
                                    snapshot.child("longitude").getValue(Double.class),
                                    snapshot.child("type").getValue().toString()
                            );
                        }).build();
        mChallengesReceivedAdapter = new FirebaseRecyclerAdapter<Challenge, ChallengesReceivedViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChallengesReceivedViewHolder holder, final int position, @NonNull final Challenge model) {

                final String user_id = model.getFrom();
                final String challenge_id = getRef(position).getKey();

                LocalDate deadline = LocalDate.parse(model.getDeadline());
                LocalDate dateNow = LocalDate.now();
                int days_left = Period.between(dateNow, deadline).getDays();



                mUsersDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("profile_info").child("username").getValue().toString();
                        final String userImage = dataSnapshot.child("profile_info").child("image_url").getValue().toString();

                        holder.setName(userName);
                        holder.setImage(userImage);

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent mapIntent = new Intent(getActivity(), MapsActivity.class);
                                mapIntent.putExtra("challenges_received", true);
                                mapIntent.putExtra("challenge_id", challenge_id);
                                mapIntent.putExtra("latitude", model.getLatitude());
                                mapIntent.putExtra("longitude", model.getLongitude());
                                mapIntent.putExtra("from_id", model.getFrom());
                                mapIntent.putExtra("from_name", userName);
                                mapIntent.putExtra("from_image", userImage);
                                startActivity(mapIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        mSwipeRefresh.setRefreshing(false);
                    }
                });

                holder.setDaysLeft(days_left);

                new Handler().postDelayed(() -> {
                    googleMapsUtils.calculateDirections(model.getLatitude(), model.getLongitude(), null);
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        Distance mDistance = googleMapsUtils.getmDistance();
                        holder.setDistance(mDistance);
                        mSwipeRefresh.setRefreshing(false);
                    }, 1000);
                }, 4000);


            }

            @NonNull
            @Override
            public ChallengesReceivedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.challenges_received_list_item, parent, false);

                return new ChallengesReceivedViewHolder(view);
            }
        };
        mChallengesReceivedList.setAdapter(mChallengesReceivedAdapter);

    }

    public static class ChallengesReceivedViewHolder extends RecyclerView.ViewHolder {

        View mView;
        CircleImageView mImage;
        TextView mUsername;
        TextView mDaysLeft;
        TextView mDistance;
        ProgressBar mDistanceLoading;

        public ChallengesReceivedViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            mImage = mView.findViewById(R.id.challenges_received_item_image);
            mUsername = mView.findViewById(R.id.challenges_received_item_username);
            mDaysLeft = mView.findViewById(R.id.challenges_received_item_days_left_txt);
            mDistance = mView.findViewById(R.id.challenges_received_item_distance);
            mDistanceLoading = mView.findViewById(R.id.challenges_received_item_distance_loading);
        }

        public void setName(String userName) {
            mUsername.setText(userName);
        }


        public void setImage(String userImage) {
            Picasso.get().load(userImage).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_profile)
                    .into(mImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(userImage).placeholder(R.drawable.default_profile)
                                    .into(mImage);
                        }
                    });
        }

        public void setDaysLeft(int days_left) {
            mDaysLeft.setText(Integer.toString(days_left));
        }

        public void setDistance(Distance distance) {
            mDistance.setText(distance.humanReadable);
            hideDistanceLoading();
        }

        public void hideDistanceLoading() {
            mDistanceLoading.animate().alpha(0.0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mDistanceLoading.setVisibility(View.GONE);
                }
            });
        }
    }
}
