package argdev.io.flingin.java.fragments;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.maps.model.LatLng;
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

import java.time.LocalDate;
import java.time.Period;

import argdev.io.flingin.R;
import argdev.io.flingin.java.activities.MapsActivity;
import argdev.io.flingin.java.models.Challenge;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChallengesFragment_Sent extends Fragment {

    View mView;

    //Recycler
    private RecyclerView mChallengesSentList;
    private FirebaseRecyclerAdapter mChallengesSentAdapter;

    private DatabaseReference mChallengesDatabase;
    private DatabaseReference mUsersDatabase;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;

    public ChallengesFragment_Sent() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_challenges_sent, container, false);

        //Recycler
        mChallengesSentList = mView.findViewById(R.id.challenges_sent_list);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mChallengesDatabase = FirebaseDatabase.getInstance().getReference().child("challenges").child("sent").child(mCurrent_user_id);
        mChallengesDatabase.keepSynced(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");

        mChallengesSentList.setHasFixedSize(true);
        mChallengesSentList.setLayoutManager(new LinearLayoutManager(getActivity()));

        fetch();
        mChallengesSentAdapter.startListening();

        FloatingActionButton challengeCreateBtn = mView.findViewById(R.id.challenge_create_btn);
        challengeCreateBtn.setOnClickListener(view -> {
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
            View dialogView = getLayoutInflater().inflate(R.layout.challenge_type_dialog_layout, null);

            MaterialCardView mLocationBtn = dialogView.findViewById(R.id.challenge_type_location_btn);
            MaterialCardView mARbtn = dialogView.findViewById(R.id.challenge_type_ar_btn);
            mBuilder.setView(dialogView);
            AlertDialog dialog = mBuilder.create();

            mLocationBtn.setOnClickListener(view1 -> {
               Intent mapIntent = new Intent(getActivity(), MapsActivity.class);
               startActivity(mapIntent);
               dialog.dismiss();
            });
            dialog.show();
        });

        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mChallengesSentAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mChallengesSentAdapter.stopListening();
    }

    private void fetch() {

        Query query = mChallengesDatabase.orderByChild("deadline");

        FirebaseRecyclerOptions<Challenge> options =
                new FirebaseRecyclerOptions.Builder<Challenge>()
                        .setQuery(query, snapshot -> {
                            Log.d("CHALLSENT", "parseSnapshot: " + snapshot.getValue().toString());
                            return new Challenge(
                                    snapshot.child("to").getValue().toString(),
                                    "",
                                    snapshot.child("deadline").getValue().toString(),
                                    snapshot.child("latitude").getValue(Double.class),
                                    snapshot.child("longitude").getValue(Double.class),
                                    snapshot.child("type").getValue().toString()
                            );
                        }).build();
        mChallengesSentAdapter = new FirebaseRecyclerAdapter<Challenge, ChallengesSentViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChallengesSentViewHolder holder, final int position, @NonNull final Challenge model) {

                final String user_id = model.getTo();

                LocalDate deadline = LocalDate.parse(model.getDeadline());
                LocalDate dateNow = LocalDate.now();
                int days_left = Period.between(dateNow, deadline).getDays();

                holder.setDaysLeft(days_left);

                mUsersDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("profile_info").child("username").getValue().toString();
                        final String userImage = dataSnapshot.child("profile_info").child("image_url").getValue().toString();

                        holder.setName(userName);
                        holder.setImage(userImage);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ChallengesSentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.challenges_sent_list_item, parent, false);

                return new ChallengesSentViewHolder(view);
            }
        };
        mChallengesSentList.setAdapter(mChallengesSentAdapter);
    }

    public static class ChallengesSentViewHolder extends RecyclerView.ViewHolder {

        View mView;
        CircleImageView mImage;
        TextView mUsername;
        TextView mDaysLeft;

        public ChallengesSentViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            mImage = mView.findViewById(R.id.challenges_sent_item_image);
            mUsername = mView.findViewById(R.id.challenges_sent_item_username);
            mDaysLeft = mView.findViewById(R.id.challenges_sent_item_days_left_txt);
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
    }

}
